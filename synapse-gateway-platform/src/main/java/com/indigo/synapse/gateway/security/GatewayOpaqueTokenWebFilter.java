package com.indigo.synapse.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indigo.synapse.core.exception.SynapseAuthenticationException;
import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.AuthorizationSnapshotKeys;
import com.indigo.synapse.iam.auth.OpaqueTokenDigest;
import com.indigo.synapse.iam.auth.TokenPrincipalType;
import com.indigo.synapse.security.context.AuthenticatedClient;
import com.indigo.synapse.security.context.AuthenticatedPrincipal;
import com.indigo.synapse.security.context.AuthenticatedUser;
import com.indigo.synapse.web.core.trace.TraceHeaders;
import com.indigo.synapse.web.core.trace.TraceIdGenerator;
import com.indigo.synapse.webflux.exception.ReactiveWebErrorResponseWriter;
import com.indigo.synapse.webflux.exception.WebFluxExceptionResponseFactory;
import org.springframework.core.Ordered;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Gateway Reactive Opaque Access Token Redis 授权快照验证过滤器。
 */
public class GatewayOpaqueTokenWebFilter implements WebFilter, Ordered {

    private static final List<String> PERMIT_PATHS = List.of(
            "/actuator/health",
            "/actuator/info",
            "/error",
            "/iam/auth/login",
            "/iam/auth/refresh"
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final GatewayAuthProperties properties;
    private final WebFluxExceptionResponseFactory responseFactory;
    private final ReactiveWebErrorResponseWriter responseWriter;
    private final Clock clock = Clock.systemUTC();

    public GatewayOpaqueTokenWebFilter(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            GatewayAuthProperties properties,
            WebFluxExceptionResponseFactory responseFactory,
            ReactiveWebErrorResponseWriter responseWriter) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.responseFactory = responseFactory;
        this.responseWriter = responseWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (permit(exchange)) {
            return chain.filter(exchange);
        }
        String token = extractBearerToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return write(exchange, new SynapseAuthenticationException());
        }
        String digest;
        try {
            digest = OpaqueTokenDigest.sha256Hex(token);
        } catch (IllegalArgumentException ex) {
            return write(exchange, new SynapseAuthenticationException());
        }
        return redisTemplate.opsForValue()
                .get(AuthorizationSnapshotKeys.key(digest))
                .switchIfEmpty(Mono.error(new SynapseAuthenticationException()))
                .map(this::decode)
                .flatMap(snapshot -> authenticate(exchange, chain, digest, snapshot))
                .onErrorResume(SynapseAuthenticationException.class, ex -> write(exchange, ex))
                .onErrorResume(RedisConnectionFailureException.class,
                        ex -> write(exchange, new GatewayAuthInfrastructureException(ex)))
                .onErrorResume(RedisSystemException.class,
                        ex -> write(exchange, new GatewayAuthInfrastructureException(ex)));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> authenticate(
            ServerWebExchange exchange,
            WebFilterChain chain,
            String digest,
            AuthorizationSnapshot snapshot) {
        if (!digest.equals(snapshot.tokenDigest())
                || !snapshot.activeAt(clock.instant())
                || !snapshot.matches(properties.getIssuer(), properties.getAudiences())) {
            return Mono.error(new SynapseAuthenticationException());
        }
        AuthenticatedPrincipal principal = principal(snapshot);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, "", authorities(snapshot));
        SecurityContextImpl securityContext = new SecurityContextImpl(authentication);
        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
    }

    private AuthorizationSnapshot decode(String value) {
        try {
            return objectMapper.readValue(value, AuthorizationSnapshot.class);
        } catch (Exception ex) {
            throw new SynapseAuthenticationException();
        }
    }

    private static AuthenticatedPrincipal principal(AuthorizationSnapshot snapshot) {
        if (snapshot.principalType() == TokenPrincipalType.USER) {
            return new AuthenticatedUser(
                    snapshot.subjectId(),
                    snapshot.displayName(),
                    snapshot.tenantId(),
                    snapshot.roles(),
                    snapshot.permissions());
        }
        if (snapshot.principalType() == TokenPrincipalType.CLIENT) {
            return new AuthenticatedClient(
                    snapshot.subjectId(),
                    snapshot.displayName(),
                    snapshot.tenantId(),
                    snapshot.roles(),
                    snapshot.permissions());
        }
        throw new SynapseAuthenticationException();
    }

    private static List<SimpleGrantedAuthority> authorities(AuthorizationSnapshot snapshot) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        snapshot.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority(prefixed("ROLE_", role))));
        snapshot.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(prefixed("PERM_", permission))));
        return authorities;
    }

    private static String prefixed(String prefix, String value) {
        return value.startsWith(prefix) ? value : prefix + value;
    }

    private boolean permit(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        return PERMIT_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> write(ServerWebExchange exchange, RuntimeException exception) {
        String traceId = traceId(exchange);
        return responseWriter.write(exchange, responseFactory.from(exception, traceId), traceId);
    }

    private static String traceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TraceHeaders.TRACE_ID);
        return traceId == null || traceId.isBlank() ? TraceIdGenerator.generate() : traceId;
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        String value = authorization.trim();
        if (value.length() <= 6
                || !value.regionMatches(true, 0, "Bearer", 0, 6)
                || !Character.isWhitespace(value.charAt(6))) {
            return null;
        }
        String token = value.substring(6).trim();
        return token.isBlank() ? null : token;
    }
}
