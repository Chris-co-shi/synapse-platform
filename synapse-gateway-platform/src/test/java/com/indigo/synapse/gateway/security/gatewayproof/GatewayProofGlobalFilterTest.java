package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.gatewayproof.GatewayProof;
import com.indigo.synapse.security.gatewayproof.GatewayProofCanonicalRequest;
import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import com.indigo.synapse.security.gatewayproof.GatewayProofNonceGenerator;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import com.indigo.synapse.security.gatewayproof.GatewayProofVerificationStatus;
import com.indigo.synapse.security.gatewayproof.HmacSha256GatewayProofSigner;
import com.indigo.synapse.security.gatewayproof.HmacSha256GatewayProofVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayProofGlobalFilter} Header 清理、签名绑定和顺序测试。
 */
class GatewayProofGlobalFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant NOW = Instant.parse("2026-06-20T08:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldSanitizeAllGatewayHeadersEvenWhenProofDisabled() {
        GatewayProofProperties properties = new GatewayProofProperties();
        GatewayProofGlobalFilter filter = newFilter(properties, new HmacSha256GatewayProofSigner());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test")
                .header(GatewayProofHeaders.VERSION, "forged")
                .header("x-synapse-gateway-signature", "forged")
                .header("X-Synapse-Gateway-Future-Field", "forged")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return Mono.empty();
        }).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.keySet()).noneMatch(GatewayProofHeaders::isGatewayProofHeader);
    }

    @Test
    void shouldCreateAllHeadersAndBindBearerTokenWithoutChangingAuthorization() {
        GatewayProofProperties properties = enabledProperties();
        AtomicReference<GatewayProofCanonicalRequest> signedRequest = new AtomicReference<>();
        HmacSha256GatewayProofSigner delegate = new HmacSha256GatewayProofSigner();
        GatewayProofGlobalFilter filter = newFilter(properties, (request, secret) -> {
            signedRequest.set(request);
            return delegate.sign(request, secret);
        });
        String authorization = "bEaReR access-token";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/downstream?b=2&a=1")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return Mono.empty();
        }).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo(authorization);
        assertThat(headers.getFirst(GatewayProofHeaders.VERSION)).isEqualTo("v1");
        assertThat(headers.getFirst(GatewayProofHeaders.GATEWAY_ID)).isEqualTo("synapse-gateway");
        assertThat(headers.getFirst(GatewayProofHeaders.TIMESTAMP)).isEqualTo(Long.toString(CLOCK.millis()));
        assertThat(headers.getFirst(GatewayProofHeaders.NONCE)).isEqualTo(signedRequest.get().nonce());
        assertThat(headers.getFirst(GatewayProofHeaders.SIGNATURE)).isNotBlank();
        assertThat(signedRequest.get().bearerTokenHash())
                .isEqualTo(new GatewayProofTokenHasher().sha256Hex("access-token"));
    }

    @Test
    void shouldTreatMissingOrNonBearerAuthorizationAsEmptyToken() {
        GatewayProofProperties properties = enabledProperties();
        AtomicReference<GatewayProofCanonicalRequest> signedRequest = new AtomicReference<>();
        GatewayProofGlobalFilter filter = newFilter(properties, (request, secret) -> {
            signedRequest.set(request);
            return "signature";
        });

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/test")
                .header(HttpHeaders.AUTHORIZATION, "Basic abc")
                .build()), current -> Mono.empty()).block();

        assertThat(signedRequest.get().bearerTokenHash()).isEmpty();
    }

    @Test
    void shouldRejectRequestWhenSigningFails() {
        GatewayProofProperties properties = enabledProperties();
        GatewayProofGlobalFilter filter = newFilter(properties, (request, secret) -> {
            throw new IllegalStateException("signing failed");
        });
        AtomicBoolean forwarded = new AtomicBoolean();

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> filter.filter(
                MockServerWebExchange.from(MockServerHttpRequest.get("/test").build()),
                current -> {
                    forwarded.set(true);
                    return Mono.empty();
                }).block()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("signing failed");
        assertThat(forwarded).isFalse();
    }

    @Test
    void shouldSignStripPrefixPathAndFrameworkVerifierShouldRejectExternalPath() {
        GatewayProofProperties properties = enabledProperties();
        GatewayProofGlobalFilter filter = newFilter(properties, new HmacSha256GatewayProofSigner());
        StripPrefixGatewayFilterFactory.Config config = new StripPrefixGatewayFilterFactory.Config();
        config.setParts(1);
        GatewayFilter stripPrefix = new StripPrefixGatewayFilterFactory().apply(config);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/iam/test?b=2&a=1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        stripPrefix.filter(exchange, stripped -> filter.filter(stripped, current -> {
            forwarded.set(current);
            return Mono.empty();
        })).block();

        ServerWebExchange current = forwarded.get();
        assertThat(current.getRequest().getURI().getRawPath()).isEqualTo("/test");
        GatewayProof proof = proofFrom(current.getRequest().getHeaders());
        String tokenHash = new GatewayProofTokenHasher().sha256Hex("access-token");
        HmacSha256GatewayProofVerifier verifier = new HmacSha256GatewayProofVerifier(
                Map.of("synapse-gateway", SECRET), Duration.ofSeconds(60), CLOCK, null, false);
        GatewayProofCanonicalRequest downstream = requestFor(proof, "/test", tokenHash);
        GatewayProofCanonicalRequest external = requestFor(proof, "/iam/test", tokenHash);

        assertThat(verifier.verify(proof, downstream).isSuccess()).isTrue();
        assertThat(verifier.verify(proof, external).status())
                .isEqualTo(GatewayProofVerificationStatus.INVALID_SIGNATURE);
        assertThat(filter.getOrder()).isEqualTo(GatewayProofGlobalFilter.FILTER_ORDER);
    }

    private static GatewayProofGlobalFilter newFilter(
            GatewayProofProperties properties,
            com.indigo.synapse.security.gatewayproof.GatewayProofSigner signer) {
        return new GatewayProofGlobalFilter(
                properties,
                signer,
                new GatewayProofTokenHasher(),
                new GatewayProofNonceGenerator(),
                CLOCK
        );
    }

    private static GatewayProofProperties enabledProperties() {
        GatewayProofProperties properties = new GatewayProofProperties();
        properties.setEnabled(true);
        properties.setSecret(SECRET);
        return properties;
    }

    private static GatewayProof proofFrom(HttpHeaders headers) {
        return new GatewayProof(
                headers.getFirst(GatewayProofHeaders.VERSION),
                headers.getFirst(GatewayProofHeaders.GATEWAY_ID),
                headers.getFirst(GatewayProofHeaders.TIMESTAMP),
                headers.getFirst(GatewayProofHeaders.NONCE),
                headers.getFirst(GatewayProofHeaders.SIGNATURE)
        );
    }

    private static GatewayProofCanonicalRequest requestFor(GatewayProof proof, String path, String tokenHash) {
        return new GatewayProofCanonicalRequest(
                proof.version(), proof.gatewayId(), proof.timestamp(), proof.nonce(),
                "GET", path, "b=2&a=1", tokenHash
        );
    }
}
