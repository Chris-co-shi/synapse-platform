package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.core.exception.SynapseAuthenticationException;

/**
 * IAM 对外统一认证失败异常。
 */
public class IamAuthenticationFailureException extends SynapseAuthenticationException {

    public IamAuthenticationFailureException() {
        super(IamAuthErrorCode.IAM_AUTHENTICATION_FAILED, IamAuthErrorCode.IAM_AUTHENTICATION_FAILED.message());
    }
}
