package com.indigo.synapse.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indigo.synapse.gateway.SynapseGatewayApplication;
import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.AuthorizationSnapshotKeys;
import com.indigo.synapse.iam.auth.AuthorizationSnapshotStatus;
import com.indigo.synapse.iam.auth.OpaqueTokenDigest;
import com.indigo.synapse.iam.auth.TokenPrincipalType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Gateway Reactive Opaque Redis 授权快照验证集成测试。
 */
@SpringBootTest(
        classes = {SynapseGatewayApplication.class, GatewayOpaqueSecurityIntegrationTest.TestSupport.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "synapse.gateway.proof.enabled=false",
                "synapse.gateway.auth.issuer=https://iam.test",
                "synapse.gateway.auth.audiences=synapse-platform"
        })
@AutoConfigureWebTestClient
class GatewayOpaqueSecurityIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private WebTestClient webTestClient;

    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldAllowHealthAndCurrentIamPublicPathsWithoutUserToken() {
        client().get().uri("/actuator/health").exchange().expectStatus().isOk();
        assertPublicEndpoint(HttpMethod.POST, "/iam/auth/login");
        assertPublicEndpoint(HttpMethod.POST, "/iam/auth/refresh");
    }

    @Test
    void shouldRejectMissingInvalidAndUnavailableRedisDifferently() {
        client().get().uri("/test/protected").exchange()
                .expectStatus().isUnauthorized();

        client().get().uri("/test/protected").headers(headers -> headers.setBearerAuth("missing"))
                .exchange().expectStatus().isUnauthorized();

        client().get().uri("/test/protected").headers(headers -> headers.setBearerAuth("redis-down"))
                .exchange().expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.code")
                .isEqualTo("GATEWAY_AUTH_INFRASTRUCTURE_UNAVAILABLE");
    }

    @Test
    void shouldForwardValidOpaqueTokenWithoutBusinessPermissionCheck() {
        client().get().uri("/test/protected").headers(headers -> headers.setBearerAuth("valid"))
                .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("ok");
    }

    @Test
    void shouldProtectBusinessRouteNamespaces() {
        assertThat(applicationContext.getBeansOfType(GatewayOpaqueTokenWebFilter.class)).hasSize(1);
        client().get().uri("/resource/test").exchange().expectStatus().isUnauthorized();
    }

    private WebTestClient client() {
        return webTestClient;
    }

    private void assertPublicEndpoint(HttpMethod method, String path) {
        HttpStatusCode status = client().method(method).uri(path)
                .exchange().returnResult(Void.class).getStatus();
        assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * 测试专用路由与 Reactive Redis。
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestSupport {

        private static final Map<String, String> SNAPSHOTS = new ConcurrentHashMap<>();

        @Bean
        RouterFunction<ServerResponse> protectedTestRoute() {
            return route(GET("/test/protected"), request -> ServerResponse.ok().bodyValue("ok"));
        }

        @Bean
        @Primary
        ReactiveStringRedisTemplate reactiveStringRedisTemplate(ObjectMapper objectMapper) throws Exception {
            SNAPSHOTS.clear();
            SNAPSHOTS.put(AuthorizationSnapshotKeys.key(OpaqueTokenDigest.sha256Hex("valid")),
                    objectMapper.writeValueAsString(snapshot("valid")));
            ReactiveStringRedisTemplate template = Mockito.mock(ReactiveStringRedisTemplate.class);
            ReactiveValueOperations<String, String> operations = Mockito.mock(ReactiveValueOperations.class);
            when(template.opsForValue()).thenReturn(operations);
            when(operations.get(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                if (AuthorizationSnapshotKeys.key(OpaqueTokenDigest.sha256Hex("redis-down")).equals(key)) {
                    return Mono.error(new RedisConnectionFailureException("redis down"));
                }
                return Mono.justOrEmpty(SNAPSHOTS.get(key));
            });
            return template;
        }

        private static AuthorizationSnapshot snapshot(String token) {
            Instant now = Instant.now();
            return new AuthorizationSnapshot(
                    OpaqueTokenDigest.sha256Hex(token),
                    "user-1",
                    TokenPrincipalType.USER,
                    "synapse-console",
                    "session-1",
                    "Tester",
                    null,
                    Set.of(),
                    Set.of(),
                    "https://iam.test",
                    Set.of("synapse-platform"),
                    0,
                    now.minusSeconds(5),
                    now.plusSeconds(300),
                    AuthorizationSnapshotStatus.ACTIVE
            );
        }
    }
}
