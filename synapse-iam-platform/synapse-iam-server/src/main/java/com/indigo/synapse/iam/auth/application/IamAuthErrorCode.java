package com.indigo.synapse.iam.auth.application;

import com.indigo.synapse.core.error.ErrorCode;

/**
 * IAM 认证闭环错误码。
 */
public enum IamAuthErrorCode implements ErrorCode {

    /**
     * 登录凭据无效或账号状态不允许登录，对外不区分具体原因。
     */
    IAM_AUTHENTICATION_FAILED("IAM_AUTHENTICATION_FAILED", "用户名或密码错误"),

    /**
     * Token 缺失、无效、过期或已撤销。
     */
    IAM_TOKEN_INVALID("IAM_TOKEN_INVALID", "Token 无效"),

    /**
     * Refresh Token 已失效或发生重放。
     */
    IAM_REFRESH_TOKEN_INVALID("IAM_REFRESH_TOKEN_INVALID", "Refresh Token 无效"),

    /**
     * Redis 等认证基础设施不可用。
     */
    IAM_AUTH_INFRASTRUCTURE_UNAVAILABLE("IAM_AUTH_INFRASTRUCTURE_UNAVAILABLE", "认证基础设施不可用");

    private final String code;
    private final String message;

    IamAuthErrorCode(String code, String message) {
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
