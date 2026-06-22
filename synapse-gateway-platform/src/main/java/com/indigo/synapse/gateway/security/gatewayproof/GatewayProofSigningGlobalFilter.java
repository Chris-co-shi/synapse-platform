package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.gatewayproof.GatewayProofCanonicalRequest;
import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import com.indigo.synapse.security.gatewayproof.GatewayProofNonceGenerator;
import com.indigo.synapse.security.gatewayproof.GatewayProofSigner;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import com.indigo.synapse.security.gatewayproof.GatewayProofVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Clock;
import java.util.Objects;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 使用 Framework GatewayProof 协议签发最终下游请求证明。
 *
 * <p>本过滤器在 StripPrefix 等路径改写之后读取当前请求，确保签名绑定下游实际收到的 method、path
 * 和 query；随后在 Netty 网络转发之前写入证明 Header。GatewayProof 只证明请求经过可信 Gateway
 * 且签名内容未被篡改，不能替代 Gateway 或下游的 JWT 验证，也不能替代下游业务权限授权。</p>
 *
 * <p>原始 Bearer Token 必须继续通过 Authorization Header 转发，供下游独立验证 JWT；签名只绑定其
 * Framework SHA-256 指纹。任何失败均 fail closed，日志只记录 routeId、method、path 和异常类型，
 * 不记录 Token、secret、canonical string、signature、token hash 或完整 nonce。</p>
 */
public final class GatewayProofSigningGlobalFilter implements GlobalFilter, Ordered {

    /**
     * 晚于 order 0 的 StripPrefix 和 order 10000 的 RouteToRequestUrlFilter，早于
     * {@code NettyRoutingFilter.ORDER == Ordered.LOWEST_PRECEDENCE}；Filter order 是安全协议的一部分。
     */
    public static final int GATEWAY_PROOF_SIGNING_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;

    private static final Logger LOG = LoggerFactory.getLogger(GatewayProofSigningGlobalFilter.class);
    private static final String BEARER_SCHEME = "Bearer";

    private final GatewayProofProperties properties;
    private final GatewayProofSigner signer;
    private final GatewayProofTokenHasher tokenHasher;
    private final GatewayProofNonceGenerator nonceGenerator;
    private final Clock clock;

    /**
     * 创建出站 GatewayProof 签名过滤器。
     *
     * @param properties 出站证明配置
     * @param signer Framework 签名器
     * @param tokenHasher Framework Token 指纹工具
     * @param nonceGenerator Framework nonce 生成器
     * @param clock 可注入 UTC 时钟，用于生产 UTC 时间戳和确定性测试
     */
    public GatewayProofSigningGlobalFilter(
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
     * 在启用时签发证明；关闭时不修改 Authorization 或生成任何证明 Header。
     *
     * @param exchange 当前请求交换对象
     * @param chain Gateway 过滤器链
     * @return 请求处理完成信号
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        return Mono.defer(() -> {
            ServerHttpRequest signed;
            try {
                signed = sign(exchange);
            } catch (RuntimeException error) {
                logFailure(exchange, error);
                return Mono.error(new GatewayProofSigningException(error));
            }
            return chain.filter(exchange.mutate().request(signed).build());
        });
    }

    /**
     * 返回证明签发顺序。
     *
     * @return RouteToRequestUrlFilter 之后的顺序
     */
    @Override
    public int getOrder() {
        return GATEWAY_PROOF_SIGNING_ORDER;
    }

    private ServerHttpRequest sign(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = Objects.requireNonNull(request.getMethod(), "HTTP method is required");
        URI uri = Objects.requireNonNull(request.getURI(), "request URI is required");
        String path = Objects.requireNonNull(uri.getRawPath(), "request path is required");
        String timestamp = Long.toString(clock.millis());
        String nonce = nonceGenerator.generate();
        String bearerToken = extractBearerToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        GatewayProofCanonicalRequest canonicalRequest = new GatewayProofCanonicalRequest(
                GatewayProofVersion.V1,
                properties.getGatewayId(),
                timestamp,
                nonce,
                method.name(),
                path,
                uri.getRawQuery(),
                tokenHasher.sha256Hex(bearerToken)
        );
        String signature = signer.sign(canonicalRequest, properties.getSecret());
        return request.mutate().headers(headers -> {
            headers.set(GatewayProofHeaders.VERSION, GatewayProofVersion.V1);
            headers.set(GatewayProofHeaders.GATEWAY_ID, properties.getGatewayId());
            headers.set(GatewayProofHeaders.TIMESTAMP, timestamp);
            headers.set(GatewayProofHeaders.NONCE, nonce);
            headers.set(GatewayProofHeaders.SIGNATURE, signature);
        }).build();
    }

    private static String extractBearerToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        String value = authorization.trim();
        if (value.length() <= BEARER_SCHEME.length()
                || !value.regionMatches(true, 0, BEARER_SCHEME, 0, BEARER_SCHEME.length())
                || !Character.isWhitespace(value.charAt(BEARER_SCHEME.length()))) {
            return "";
        }
        int tokenStart = BEARER_SCHEME.length();
        while (tokenStart < value.length() && Character.isWhitespace(value.charAt(tokenStart))) {
            tokenStart++;
        }
        return tokenStart == value.length() ? "" : value.substring(tokenStart).trim();
    }

    private static void logFailure(ServerWebExchange exchange, Throwable error) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String routeId = route == null ? "unknown" : route.getId();
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() == null ? "unknown" : request.getMethod().name();
        String path = request.getURI().getRawPath();
        LOG.error("GatewayProof 签发失败: errorType={}, routeId={}, method={}, path={}",
                error.getClass().getSimpleName(), routeId, method, path);
    }
}
