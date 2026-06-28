package com.indigo.synapse.gateway.security;

import com.indigo.synapse.core.error.ErrorCode;
import com.indigo.synapse.web.core.error.ErrorHttpStatusResolver;
import org.springframework.stereotype.Component;

import java.util.OptionalInt;

/**
 * Gateway 认证错误码 HTTP 状态映射。
 */
@Component
public class GatewayAuthHttpStatusResolver implements ErrorHttpStatusResolver {

    @Override
    public OptionalInt resolve(ErrorCode errorCode) {
        if (errorCode != null
                && GatewayAuthErrorCode.GATEWAY_AUTH_INFRASTRUCTURE_UNAVAILABLE.code().equals(errorCode.code())) {
            return OptionalInt.of(503);
        }
        return OptionalInt.empty();
    }
}
