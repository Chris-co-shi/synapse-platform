package com.indigo.synapse.iam.identity.domain.model;

/**
 * IAM 用户状态。
 */
public enum UserStatus {

    /**
     * 用户正常启用。
     */
    ACTIVE,

    /**
     * 用户已被管理员禁用。
     */
    DISABLED
}
