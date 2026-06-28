package com.indigo.synapse.iam.auth;

/**
 * Opaque Access Token 授权快照 Redis key 规范。
 */
public final class AuthorizationSnapshotKeys {

    /**
     * 授权快照 Redis key 前缀。
     */
    public static final String PREFIX = "synapse:iam:authorization-snapshot:";

    private AuthorizationSnapshotKeys() {
    }

    /**
     * 根据 Access Token 摘要构造 Redis key。
     *
     * @param tokenDigest Access Token SHA-256 摘要
     * @return Redis key
     */
    public static String key(String tokenDigest) {
        if (tokenDigest == null || tokenDigest.isBlank()) {
            throw new IllegalArgumentException("tokenDigest must not be blank");
        }
        return PREFIX + tokenDigest.trim();
    }
}
