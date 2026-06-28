package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.core.exception.SynapseAuthenticationException;

/**
 * Opaque Token 无效异常。
 */
public class IamTokenInvalidException extends SynapseAuthenticationException {

    public IamTokenInvalidException() {
        super(IamAuthErrorCode.IAM_TOKEN_INVALID, IamAuthErrorCode.IAM_TOKEN_INVALID.message());
    }
}
