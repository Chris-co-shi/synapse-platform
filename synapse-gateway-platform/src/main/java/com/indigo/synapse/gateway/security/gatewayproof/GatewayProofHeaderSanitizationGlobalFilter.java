package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * 清理客户端传入的全部 GatewayProof 可信前缀 Header。
 *
 * <p>任何外部 {@code X-Synapse-Gateway-*} Header 都不可信，包括 Framework 未来新增的字段。
 * 因此本过滤器复用 Framework 的前缀判断，而不是只删除当前五个已知 Header。即使证明签名关闭，
 * 清理也必须继续执行，防止客户端借开发配置伪造可信 Gateway 身份。</p>
 */
public final class GatewayProofHeaderSanitizationGlobalFilter implements GlobalFilter, Ordered {

    /**
     * Header 清理必须早于路由改写、GatewayProof 签名和网络转发；Filter order 是安全协议的一部分。
     */
    public static final int GATEWAY_PROOF_SANITIZATION_ORDER = Ordered.HIGHEST_PRECEDENCE;

    /**
     * 删除全部外部 GatewayProof Header，同时保留 Authorization 和其他普通 Header。
     *
     * @param exchange 当前请求交换对象
     * @param chain Gateway 过滤器链
     * @return 请求处理完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitized = exchange.getRequest().mutate().headers(headers -> {
            List<String> untrustedNames = new ArrayList<>();
            headers.forEach((name, values) -> {
                if (GatewayProofHeaders.isGatewayProofHeader(name)) {
                    untrustedNames.add(name);
                }
            });
            untrustedNames.forEach(headers::remove);
        }).build();
        return chain.filter(exchange.mutate().request(sanitized).build());
    }

    /**
     * 返回 Header 清理顺序。
     *
     * @return 最高优先级
     */
    @Override
    public int getOrder() {
        return GATEWAY_PROOF_SANITIZATION_ORDER;
    }
}
