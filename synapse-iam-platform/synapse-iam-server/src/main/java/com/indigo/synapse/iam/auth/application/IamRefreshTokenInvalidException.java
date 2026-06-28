package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.core.exception.SynapseAuthenticationException;

/**
 * Refresh Token 无效或重放异常。
 */
public class IamRefreshTokenInvalidException extends SynapseAuthenticationException {

    public IamRefreshTokenInvalidException() {
        super(IamAuthErrorCode.IAM_REFRESH_TOKEN_INVALID, IamAuthErrorCode.IAM_REFRESH_TOKEN_INVALID.message());
    }
}
