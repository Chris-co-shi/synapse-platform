package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.gatewayproof.GatewayProofCanonicalRequest;
import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import com.indigo.synapse.security.gatewayproof.GatewayProofNonceGenerator;
import com.indigo.synapse.security.gatewayproof.GatewayProofSigner;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import com.indigo.synapse.security.gatewayproof.GatewayProofVersion;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 清理外部 Gateway Header 并为最终下游请求签发 GatewayProof 的全局过滤器。
 *
 * <p>过滤器保持无状态，所有请求数据只存在于当前 Reactor 链中，因而可安全用于多实例和未来
 * Kubernetes 横向扩容。它绝不记录 token、secret 或 canonical string。GatewayProof 只证明请求
 * 经过可信 Gateway，不能代替 Gateway 与下游各自执行的 JWT 验证。</p>
 */
public final class GatewayProofGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 在 {@link RouteToRequestUrlFilter} 和路由级 StripPrefix 之后运行，从请求读取最终下游路径；
     * 同时远早于真正的 Netty 网络转发，确保新 Header 会被发送给下游。
     */
    public static final int FILTER_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewayProofProperties properties;
    private final GatewayProofSigner signer;
    private final GatewayProofTokenHasher tokenHasher;
    private final GatewayProofNonceGenerator nonceGenerator;
    private final Clock clock;

    /**
     * 创建 GatewayProof 过滤器。
     *
     * @param properties 出站证明配置
     * @param signer Framework 签名器
     * @param tokenHasher Framework token 指纹工具
     * @param nonceGenerator Framework nonce 生成器
     * @param clock UTC 时钟
     */
    public GatewayProofGlobalFilter(
            GatewayProofProperties properties,
            GatewayProofSigner signer,
            GatewayProofTokenHasher tokenHasher,
            GatewayProofNonceGenerator nonceGenerator,
            Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.signer = Objects.requireNonNull(signer, "signer must not be null");
        this.tokenHasher = Objects.requireNonNull(tokenHasher, "tokenHasher must not be null");
        this.nonceGenerator = Objects.requireNonNull(nonceGenerator, "nonceGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 清理不可信 Header，并在启用时生成与最终请求绑定的五个证明 Header。
     *
     * <p>即使签名关闭也必须清理，因为外部客户端不能借由开发配置向下游伪造可信入口证明。
     * 签名异常直接终止 Reactor 链，禁止在缺少有效证明时继续转发。</p>
     *
     * @param exchange 当前请求交换对象
     * @param chain Gateway 过滤器链
     * @return 请求处理完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitized = sanitize(exchange.getRequest());
        if (!properties.isEnabled()) {
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        String timestamp = Long.toString(clock.millis());
        String nonce = nonceGenerator.generate();
        String bearerToken = extractBearerToken(sanitized.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        GatewayProofCanonicalRequest canonicalRequest = new GatewayProofCanonicalRequest(
                GatewayProofVersion.V1,
                properties.getGatewayId(),
                timestamp,
                nonce,
                sanitized.getMethod().name(),
                sanitized.getURI().getRawPath(),
                sanitized.getURI().getRawQuery(),
                tokenHasher.sha256Hex(bearerToken)
        );
        String signature = signer.sign(canonicalRequest, properties.getSecret());
        ServerHttpRequest signed = sanitized.mutate().headers(headers -> {
            headers.set(GatewayProofHeaders.VERSION, GatewayProofVersion.V1);
            headers.set(GatewayProofHeaders.GATEWAY_ID, properties.getGatewayId());
            headers.set(GatewayProofHeaders.TIMESTAMP, timestamp);
            headers.set(GatewayProofHeaders.NONCE, nonce);
            headers.set(GatewayProofHeaders.SIGNATURE, signature);
        }).build();
        return chain.filter(exchange.mutate().request(signed).build());
    }

    /** @return 过滤器顺序 */
    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    private static ServerHttpRequest sanitize(ServerHttpRequest request) {
        return request.mutate().headers(headers -> {
            List<String> untrustedNames = new ArrayList<>();
            headers.forEach((name, values) -> {
                if (GatewayProofHeaders.isGatewayProofHeader(name)) {
                    untrustedNames.add(name);
                }
            });
            untrustedNames.forEach(headers::remove);
        }).build();
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return "";
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? "" : token;
    }
}
