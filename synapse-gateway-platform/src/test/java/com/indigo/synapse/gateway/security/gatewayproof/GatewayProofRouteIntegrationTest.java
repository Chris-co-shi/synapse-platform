package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.gateway.SynapseGatewayApplication;
import com.indigo.synapse.security.gatewayproof.GatewayProof;
import com.indigo.synapse.security.gatewayproof.GatewayProofCanonicalRequest;
import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import com.indigo.synapse.security.gatewayproof.GatewayProofNonceGenerator;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import com.indigo.synapse.security.gatewayproof.HmacSha256GatewayProofVerifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 真实 Spring Cloud Gateway 路由、StripPrefix 和 Framework 验签集成测试。
 *
 * <p>下游使用本地 Reactor Netty 服务，不访问 Nacos、IAM 或其他真实服务。</p>
 */
@SpringBootTest(
        classes = {SynapseGatewayApplication.class, GatewayProofRouteIntegrationTest.TestSupport.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.config.import=",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "synapse.gateway.proof.enabled=true",
                "synapse.gateway.proof.gateway-id=synapse-gateway",
                "synapse.gateway.proof.secret=0123456789abcdef0123456789abcdef",
                "synapse.security.resource-server.enabled=false"
        })
@AutoConfigureWebTestClient
class GatewayProofRouteIntegrationTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String TOKEN = "integration-token";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-20T08:00:00Z"), ZoneOffset.UTC);
    private static final AtomicReference<RecordedRequest> DOWNSTREAM_REQUEST = new AtomicReference<>();
    private static final DisposableServer DOWNSTREAM = HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .handle((request, response) -> {
                Map<String, List<String>> headers = new LinkedHashMap<>();
                request.requestHeaders().forEach(entry -> headers
                        .computeIfAbsent(entry.getKey().toLowerCase(Locale.ROOT), key -> new java.util.ArrayList<>())
                        .add(entry.getValue()));
                DOWNSTREAM_REQUEST.set(new RecordedRequest(request.method().name(), request.uri(), headers));
                return response.status(200).sendString(Mono.just("ok"));
            })
            .bindNow();

    @org.springframework.beans.factory.annotation.Autowired
    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void gatewayRoute(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gateway.server.webflux.routes[0].id", () -> "gateway-proof-test");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].uri",
                () -> "http://127.0.0.1:" + DOWNSTREAM.port());
        registry.add("spring.cloud.gateway.server.webflux.routes[0].predicates[0]", () -> "Path=/iam/**");
        registry.add("spring.cloud.gateway.server.webflux.routes[0].filters[0]", () -> "StripPrefix=1");
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.disposeNow();
    }

    @Test
    void shouldSignStripPrefixPathAndBindEveryRequestComponent() {
        webTestClient.get().uri("/iam/test?b=2&a=1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");

        RecordedRequest recorded = DOWNSTREAM_REQUEST.get();
        assertThat(recorded.method()).isEqualTo("GET");
        assertThat(recorded.uri()).isEqualTo("/test?b=2&a=1");
        GatewayProof proof = recorded.proof();
        assertThat(proof.version()).isEqualTo("v1");
        assertThat(proof.gatewayId()).isEqualTo("synapse-gateway:gateway-proof-test");
        assertThat(proof.timestamp()).isEqualTo(Long.toString(CLOCK.millis()));
        assertThat(proof.nonce()).isEqualTo("AAAAAAAAAAAAAAAAAAAAAA");
        assertThat(proof.signature()).isNotBlank();
        assertThat(recorded.first(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TOKEN);

        String tokenHash = new GatewayProofTokenHasher().sha256Hex(TOKEN);
        HmacSha256GatewayProofVerifier verifier = new HmacSha256GatewayProofVerifier(
                Map.of("synapse-gateway:gateway-proof-test", SECRET), Duration.ofSeconds(60), CLOCK, null, false);
        GatewayProofCanonicalRequest downstream = request(proof, "GET", "/test", "b=2&a=1", tokenHash);

        assertThat(verifier.verify(proof, downstream).isSuccess()).isTrue();
        assertThat(verifier.verify(proof,
                request(proof, "GET", "/iam/test", "b=2&a=1", tokenHash)).isSuccess()).isFalse();
        assertThat(verifier.verify(proof,
                request(proof, "GET", "/test", "a=1&b=3", tokenHash)).isSuccess()).isFalse();
        assertThat(verifier.verify(proof,
                request(proof, "GET", "/test", "b=2&a=1",
                        new GatewayProofTokenHasher().sha256Hex("changed-token"))).isSuccess()).isFalse();
        assertThat(verifier.verify(proof,
                request(proof, "POST", "/test", "b=2&a=1", tokenHash)).isSuccess()).isFalse();
        assertThat(verifier.verify(new GatewayProof(proof.version(), proof.gatewayId(), proof.timestamp(),
                "changed-nonce", proof.signature()), downstream).isSuccess()).isFalse();
        assertThat(verifier.verify(new GatewayProof(proof.version(), proof.gatewayId(),
                Long.toString(CLOCK.millis() + 1), proof.nonce(), proof.signature()), downstream).isSuccess()).isFalse();
    }

    @Test
    void shouldRemoveForgedHeadersAndReplaceThemWithFreshProof() {
        webTestClient.get().uri("/iam/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .header(GatewayProofHeaders.VERSION, "forged-version")
                .header(GatewayProofHeaders.SIGNATURE, "forged-signature", "second-forged-signature")
                .header("X-Synapse-Gateway-Future-Field", "forged-future")
                .header("X-Normal-Header", "kept")
                .exchange()
                .expectStatus().isOk();

        RecordedRequest recorded = DOWNSTREAM_REQUEST.get();
        assertThat(recorded.first(GatewayProofHeaders.VERSION)).isEqualTo("v1");
        assertThat(recorded.values(GatewayProofHeaders.SIGNATURE)).hasSize(1)
                .doesNotContain("forged-signature", "second-forged-signature");
        assertThat(recorded.first("X-Synapse-Gateway-Future-Field")).isNull();
        assertThat(recorded.first("X-Normal-Header")).isEqualTo("kept");
    }

    private static GatewayProofCanonicalRequest request(
            GatewayProof proof,
            String method,
            String path,
            String query,
            String tokenHash) {
        return new GatewayProofCanonicalRequest(
                proof.version(), proof.gatewayId(), proof.timestamp(), proof.nonce(),
                method, path, query, tokenHash
        );
    }

    /**
     * 固定 Clock 与 nonce 的测试配置。
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestSupport {

        /**
         * 仅为隔离 Gateway 路由阶段而放行测试请求；JWT 边界由独立集成测试覆盖。
         *
         * @param http Reactive 安全配置
         * @return 测试专用全放行安全链
         */
        @Bean
        SecurityWebFilterChain gatewayProofTestSecurityWebFilterChain(ServerHttpSecurity http) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                    .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                    .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                    .build();
        }

        /**
         * 固定签发时间。
         *
         * @return 固定 UTC Clock
         */
        @Bean
        @Primary
        Clock fixedGatewayProofClock() {
            return CLOCK;
        }

        /**
         * 固定 nonce，保证集成测试可重复。
         *
         * @return 固定输出的 Framework nonce 生成器
         */
        @Bean
        @Primary
        GatewayProofNonceGenerator fixedGatewayProofNonceGenerator() {
            SecureRandom fixedRandom = new SecureRandom() {
                @Override
                public void nextBytes(byte[] bytes) {
                    java.util.Arrays.fill(bytes, (byte) 0);
                }
            };
            return new GatewayProofNonceGenerator(fixedRandom);
        }
    }

    private record RecordedRequest(String method, String uri, Map<String, List<String>> headers) {

        private String first(String name) {
            List<String> values = values(name);
            return values.isEmpty() ? null : values.getFirst();
        }

        private List<String> values(String name) {
            return headers.getOrDefault(name.toLowerCase(Locale.ROOT), List.of());
        }

        private GatewayProof proof() {
            return new GatewayProof(
                    first(GatewayProofHeaders.VERSION),
                    first(GatewayProofHeaders.GATEWAY_ID),
                    first(GatewayProofHeaders.TIMESTAMP),
                    first(GatewayProofHeaders.NONCE),
                    first(GatewayProofHeaders.SIGNATURE)
            );
        }
    }
}
