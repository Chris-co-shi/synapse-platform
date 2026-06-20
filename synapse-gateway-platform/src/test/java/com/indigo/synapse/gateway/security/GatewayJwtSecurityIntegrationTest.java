package com.indigo.synapse.gateway.security;

import com.indigo.synapse.gateway.SynapseGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Gateway Reactive JWT 白名单、默认保护与 Framework 统一错误结构集成测试。
 *
 * <p>测试使用内存 decoder，不访问真实 IAM；协议校验仍调用 Framework 创建的 validator，
 * 因而覆盖 issuer、audience、必填 claim 和主体类型规则。</p>
 */
@SpringBootTest(
        classes = {SynapseGatewayApplication.class, GatewayJwtSecurityIntegrationTest.TestSupport.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "synapse.gateway.proof.enabled=false",
                "synapse.security.resource-server.issuer-uri=https://iam.test",
                "synapse.security.resource-server.jwk-set-uri=https://iam.test/jwks",
                "synapse.security.resource-server.audiences=synapse-platform"
        })
@AutoConfigureWebTestClient
class GatewayJwtSecurityIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAllowHealthAndExplicitIamPublicPathsWithoutUserToken() {
        client().get().uri("/actuator/health").exchange().expectStatus().isOk();
        client().get().uri("/actuator/health/readiness").exchange().expectStatus().isOk();
        assertThat(statusOf("/iam/oauth2/token")).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(statusOf("/iam/oauth2/jwks")).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(statusOf("/iam/.well-known/openid-configuration")).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectMissingAndInvalidTokensWithFrameworkResult() {
        client().get().uri("/test/protected").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                .expectBody()
                .jsonPath("$.code").isEqualTo("OAUTH2_INVALID_TOKEN");
        for (String token : List.of("malformed", "bad-signature")) {
            client().get().uri("/test/protected").headers(headers -> headers.setBearerAuth(token))
                    .exchange().expectStatus().isUnauthorized();
        }
    }

    @Test
    void shouldRejectExpiredIssuerAudienceAndRequiredClaimFailures() {
        for (String token : List.of("expired", "not-before", "wrong-issuer", "wrong-audience", "missing-claim")) {
            client().get().uri("/test/protected").headers(headers -> headers.setBearerAuth(token))
                    .exchange().expectStatus().isUnauthorized();
        }
    }

    @Test
    void shouldForwardValidTokenWithoutBusinessPermissionClaims() {
        client().get().uri("/test/protected").headers(headers -> headers.setBearerAuth("valid"))
                .exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("ok");
    }

    private HttpStatusCode statusOf(String path) {
        return client().get().uri(path).exchange().returnResult(Void.class).getStatus();
    }

    private WebTestClient client() {
        return webTestClient;
    }

    /**
     * 测试专用路由与内存 JWT decoder。
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestSupport {

        /**
         * 提供受保护的本地测试下游，验证合法 Token 确实越过安全链。
         *
         * @return 测试路由
         */
        @Bean
        RouterFunction<ServerResponse> protectedTestRoute() {
            return route(GET("/test/protected"), request -> ServerResponse.ok().bodyValue("ok"));
        }

        /**
         * 创建不访问网络的 decoder，并复用 Framework validator 验证构造的 JWT。
         *
         * @param validator Framework Spring JWT validator
         * @return 测试 decoder
         */
        @Bean
        @Primary
        ReactiveJwtDecoder testReactiveJwtDecoder(
                @Qualifier("synapseReactiveSpringJwtValidator") OAuth2TokenValidator<Jwt> validator) {
            return token -> {
                if ("malformed".equals(token)) {
                    return Mono.error(new BadJwtException("malformed token"));
                }
                if ("bad-signature".equals(token)) {
                    return Mono.error(new BadJwtException("token signature is invalid"));
                }
                Jwt jwt = jwt(token);
                OAuth2TokenValidatorResult result = validator.validate(jwt);
                if (result.hasErrors()) {
                    return Mono.error(new JwtValidationException("token validation failed", result.getErrors()));
                }
                return Mono.just(jwt);
            };
        }

        private static Jwt jwt(String token) {
            Instant now = Instant.now();
            Instant issuedAt = "expired".equals(token) ? now.minusSeconds(120) : now.minusSeconds(10);
            Instant expiresAt = "expired".equals(token) ? now.minusSeconds(60) : now.plusSeconds(300);
            Map<String, Object> claims = new java.util.LinkedHashMap<>();
            if (!"missing-claim".equals(token)) {
                claims.put("sub", "user-1");
            }
            claims.put("iss", "wrong-issuer".equals(token) ? "https://wrong.test" : "https://iam.test");
            claims.put("aud", List.of("wrong-audience".equals(token) ? "other-service" : "synapse-platform"));
            claims.put("iat", issuedAt);
            claims.put("exp", expiresAt);
            if ("not-before".equals(token)) {
                claims.put("nbf", now.plusSeconds(120));
            }
            claims.put("token_type", "ACCESS_TOKEN");
            claims.put("principal_type", "USER");
            claims.put("username", "tester");
            // 故意不放 roles、permissions 或 scope，验证 Gateway 不执行任何业务 authority 判断。
            return new Jwt(token, issuedAt, expiresAt,
                    Map.of("alg", "none"), claims);
        }
    }
}
