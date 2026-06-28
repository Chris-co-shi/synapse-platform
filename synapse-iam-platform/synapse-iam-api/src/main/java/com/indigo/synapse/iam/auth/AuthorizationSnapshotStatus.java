package com.indigo.synapse.iam.auth;

/**
 * Opaque Access Token Redis 授权快照状态。
 */
public enum AuthorizationSnapshotStatus {

    /**
     * 快照有效，可用于认证。
     */
    ACTIVE,

    /**
     * 快照已被撤销。
     */
    REVOKED,

    /**
     * 快照已过期。
     */
    EXPIRED
}
