package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.core.exception.SynapseException;

/**
 * IAM 认证基础设施不可用异常。
 */
public class IamAuthInfrastructureException extends SynapseException {

    public IamAuthInfrastructureException(Throwable cause) {
        super(IamAuthErrorCode.IAM_AUTH_INFRASTRUCTURE_UNAVAILABLE,
                IamAuthErrorCode.IAM_AUTH_INFRASTRUCTURE_UNAVAILABLE.message(), cause);
    }
}
