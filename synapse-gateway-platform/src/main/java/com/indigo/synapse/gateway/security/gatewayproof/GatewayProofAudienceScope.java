package com.indigo.synapse.gateway.security.gatewayproof;

/**
 * GatewayProof v1 的 Platform audience 作用域编码。
 *
 * <p>Framework v1 canonical string 没有独立 audience 字段，但 gatewayId 本身被签名。Platform 因此
 * 将基础 Gateway 标识与 Spring Cloud Gateway 的服务端路由 ID 编码为
 * {@code <gatewayId>:<routeId>}。routeId 来自可信路由配置，客户端不能决定；下游只信任自身对应的
 * 完整作用域标识，从而让 audience 同时受签名、验签和 Replay Store key 约束。</p>
 */
final class GatewayProofAudienceScope {

    private static final char SEPARATOR = ':';

    private GatewayProofAudienceScope() {
    }

    /**
     * 生成受签名的 audience-scoped Gateway ID。
     *
     * @param gatewayId 基础 Gateway ID
     * @param routeId 服务端路由 ID，同时作为下游 audience
     * @return {@code gatewayId:routeId}
     */
    static String scopedGatewayId(String gatewayId, String routeId) {
        requireSegment(gatewayId, "GatewayProof gateway-id");
        requireSegment(routeId, "GatewayProof route audience");
        return gatewayId + SEPARATOR + routeId;
    }

    /**
     * 校验基础 Gateway ID，避免分隔符歧义。
     *
     * @param gatewayId 基础 Gateway ID
     */
    static void validateBaseGatewayId(String gatewayId) {
        requireSegment(gatewayId, "GatewayProof gateway-id");
    }

    private static void requireSegment(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.indexOf(SEPARATOR) >= 0) {
            throw new IllegalArgumentException(name + " must not contain ':'");
        }
    }
}
