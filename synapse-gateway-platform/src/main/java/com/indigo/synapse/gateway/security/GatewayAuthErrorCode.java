package com.indigo.synapse.gateway.security;

import com.indigo.synapse.core.error.ErrorCode;

/**
 * Gateway 认证错误码。
 */
public enum GatewayAuthErrorCode implements ErrorCode {

    /**
     * Redis 等认证基础设施不可用。
     */
    GATEWAY_AUTH_INFRASTRUCTURE_UNAVAILABLE(
            "GATEWAY_AUTH_INFRASTRUCTURE_UNAVAILABLE",
            "认证基础设施不可用"
    );

    private final String code;
    private final String message;

    GatewayAuthErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
