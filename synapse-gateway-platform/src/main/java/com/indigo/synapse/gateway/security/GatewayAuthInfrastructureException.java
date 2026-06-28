package com.indigo.synapse.gateway.security;

import com.indigo.synapse.core.exception.SynapseException;

/**
 * Gateway 认证基础设施不可用异常。
 */
public class GatewayAuthInfrastructureException extends SynapseException {

    public GatewayAuthInfrastructureException(Throwable cause) {
        super(GatewayAuthErrorCode.GATEWAY_AUTH_INFRASTRUCTURE_UNAVAILABLE,
                GatewayAuthErrorCode.GATEWAY_AUTH_INFRASTRUCTURE_UNAVAILABLE.message(), cause);
    }
}
