package com.indigo.synapse.iam.auth.domain;

/**
 * Refresh Token 会话状态。
 */
public enum RefreshSessionStatus {

    /**
     * 当前 Refresh Token 可用。
     */
    ACTIVE,

    /**
     * 当前 Refresh Token 已成功轮换。
     */
    ROTATED,

    /**
     * 当前会话已主动撤销。
     */
    REVOKED,

    /**
     * 发现旧 Refresh Token 重放，整个 family 已撤销。
     */
    REUSE_DETECTED
}
