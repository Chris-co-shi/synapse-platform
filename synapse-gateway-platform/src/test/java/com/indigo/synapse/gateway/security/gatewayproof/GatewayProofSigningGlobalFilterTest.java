package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.gatewayproof.GatewayProofCanonicalRequest;
import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import com.indigo.synapse.security.gatewayproof.GatewayProofNonceGenerator;
import com.indigo.synapse.security.gatewayproof.GatewayProofSigner;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import com.indigo.synapse.security.gatewayproof.HmacSha256GatewayProofSigner;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * {@link GatewayProofSigningGlobalFilter} 签名材料、Bearer 提取和失败关闭测试。
 */
class GatewayProofSigningGlobalFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-20T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldCreateAllHeadersAndBindFinalRequestWithoutChangingAuthorization() {
        AtomicReference<GatewayProofCanonicalRequest> signedRequest = new AtomicReference<>();
        GatewayProofSigner delegate = new HmacSha256GatewayProofSigner();
        GatewayProofSigningGlobalFilter filter = newFilter((request, secret) -> {
            signedRequest.set(request);
            return delegate.sign(request, secret);
        });
        String authorization = "  bEaReR\t  access-token  ";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .method(org.springframework.http.HttpMethod.POST, "/downstream?b=2&a=1")
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
        assertThat(headers.getFirst(GatewayProofHeaders.NONCE)).isEqualTo("AAAAAAAAAAAAAAAAAAAAAA");
        assertThat(headers.getFirst(GatewayProofHeaders.SIGNATURE)).isNotBlank();
        assertThat(signedRequest.get()).satisfies(request -> {
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.path()).isEqualTo("/downstream");
            assertThat(request.query()).isEqualTo("b=2&a=1");
            assertThat(request.bearerTokenHash())
                    .isEqualTo(new GatewayProofTokenHasher().sha256Hex("access-token"));
        });
        assertThat(filter.getOrder()).isEqualTo(GatewayProofSigningGlobalFilter.GATEWAY_PROOF_SIGNING_ORDER);
    }

    @Test
    void shouldUseEmptyTokenHashForMissingEmptyAndNonBearerAuthorization() {
        for (String authorization : new String[]{null, "Bearer   ", "BearerToken", "Basic abc"}) {
            AtomicReference<GatewayProofCanonicalRequest> signedRequest = new AtomicReference<>();
            GatewayProofSigningGlobalFilter filter = newFilter((request, secret) -> {
                signedRequest.set(request);
                return "signature";
            });
            MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/test");
            if (authorization != null) {
                request.header(HttpHeaders.AUTHORIZATION, authorization);
            }

            filter.filter(MockServerWebExchange.from(request.build()), current -> Mono.empty()).block();

            assertThat(signedRequest.get().bearerTokenHash()).isEmpty();
        }
    }

    @Test
    void shouldNotSignOrChangeAuthorizationWhenDisabled() {
        GatewayProofProperties properties = new GatewayProofProperties();
        AtomicBoolean signerCalled = new AtomicBoolean();
        GatewayProofSigningGlobalFilter filter = newFilter(properties, (request, secret) -> {
            signerCalled.set(true);
            return "signature";
        });
        String authorization = "Bearer original-token";
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/test")
                        .header(HttpHeaders.AUTHORIZATION, authorization).build()), current -> {
            forwarded.set(current);
            return Mono.empty();
        }).block();

        assertThat(signerCalled).isFalse();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo(authorization);
        assertThat(forwarded.get().getRequest().getHeaders().keySet())
                .noneMatch(GatewayProofHeaders::isGatewayProofHeader);
    }

    @Test
    void shouldFailClosedWhenSigningFails() {
        for (GatewayProofSigner signer : new GatewayProofSigner[]{
                (request, secret) -> {
                    throw new IllegalStateException("signing failed");
                }
        }) {
            AtomicBoolean forwarded = new AtomicBoolean();
            GatewayProofSigningGlobalFilter filter = newFilter(signer);

            Throwable failure = catchThrowable(() -> filter.filter(
                    MockServerWebExchange.from(MockServerHttpRequest.get("/test").build()),
                    current -> {
                        forwarded.set(true);
                        return Mono.empty();
                    }).block());

            assertThat(failure).isInstanceOf(GatewayProofSigningException.class)
                    .hasMessage("GatewayProof signing failed");
            assertThat(forwarded).isFalse();
        }
    }

    private static GatewayProofSigningGlobalFilter newFilter(GatewayProofSigner signer) {
        return newFilter(enabledProperties(), signer);
    }

    private static GatewayProofSigningGlobalFilter newFilter(
            GatewayProofProperties properties,
            GatewayProofSigner signer) {
        SecureRandom fixedRandom = new SecureRandom() {
            @Override
            public void nextBytes(byte[] bytes) {
                java.util.Arrays.fill(bytes, (byte) 0);
            }
        };
        return new GatewayProofSigningGlobalFilter(
                properties,
                signer,
                new GatewayProofTokenHasher(),
                new GatewayProofNonceGenerator(fixedRandom),
                CLOCK
        );
    }

    private static GatewayProofProperties enabledProperties() {
        GatewayProofProperties properties = new GatewayProofProperties();
        properties.setEnabled(true);
        properties.setSecret(SECRET);
        return properties;
    }
}
