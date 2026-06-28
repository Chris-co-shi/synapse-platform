package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.core.error.ErrorCode;
import com.indigo.synapse.web.core.error.ErrorHttpStatusResolver;
import org.springframework.stereotype.Component;

import java.util.OptionalInt;

/**
 * IAM 认证错误码到 HTTP 状态码的映射。
 */
@Component
public class IamAuthHttpStatusResolver implements ErrorHttpStatusResolver {

    @Override
    public OptionalInt resolve(ErrorCode errorCode) {
        if (errorCode == null) {
            return OptionalInt.empty();
        }
        if (IamAuthErrorCode.IAM_AUTH_INFRASTRUCTURE_UNAVAILABLE.code().equals(errorCode.code())) {
            return OptionalInt.of(503);
        }
        if (IamAuthErrorCode.IAM_AUTHENTICATION_FAILED.code().equals(errorCode.code())
                || IamAuthErrorCode.IAM_TOKEN_INVALID.code().equals(errorCode.code())
                || IamAuthErrorCode.IAM_REFRESH_TOKEN_INVALID.code().equals(errorCode.code())) {
            return OptionalInt.of(401);
        }
        return OptionalInt.empty();
    }
}
