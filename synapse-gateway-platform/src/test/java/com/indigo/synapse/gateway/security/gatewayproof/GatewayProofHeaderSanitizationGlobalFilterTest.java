package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayProofHeaderSanitizationGlobalFilter} 安全清理测试。
 */
class GatewayProofHeaderSanitizationGlobalFilterTest {

    @Test
    void shouldRemoveKnownCaseInsensitiveFutureAndMultiValueHeadersButKeepOthers() {
        GatewayProofHeaderSanitizationGlobalFilter filter = new GatewayProofHeaderSanitizationGlobalFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test")
                .header(GatewayProofHeaders.VERSION, "forged")
                .header("x-synapse-gateway-signature", "forged")
                .header("X-Synapse-Gateway-Future-Field", "first", "second")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .header("X-Normal-Header", "kept")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return Mono.empty();
        }).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.keySet()).noneMatch(GatewayProofHeaders::isGatewayProofHeader);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
        assertThat(headers.getFirst("X-Normal-Header")).isEqualTo("kept");
        assertThat(filter.getOrder())
                .isEqualTo(GatewayProofHeaderSanitizationGlobalFilter.GATEWAY_PROOF_SANITIZATION_ORDER);
    }
}
