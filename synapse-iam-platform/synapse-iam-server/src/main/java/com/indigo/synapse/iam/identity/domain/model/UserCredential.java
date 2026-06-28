package com.indigo.synapse.iam.identity.domain.model;

import java.time.Instant;

/**
 * IAM 用户认证凭据与失败状态。
 *
 * @param id 凭据主键，新建且尚未持久化时可以为空
 * @param userId 所属用户主键
 * @param credentialHash 编码器生成的不可逆凭据摘要
 * @param changedAt 凭据最后修改时间
 * @param failedAttempts 连续认证失败次数
 * @param lockedUntil 临时锁定截止时间，未锁定时为空
 * @param revision 乐观锁版本
 * @param createdAt 创建时间，尚未持久化时可以为空
 * @param updatedAt 更新时间，尚未持久化时可以为空
 */
public record UserCredential(
        String id,
        String userId,
        String credentialHash,
        Instant changedAt,
        int failedAttempts,
        Instant lockedUntil,
        int revision,
        Instant createdAt,
        Instant updatedAt) {

    private static final int MAX_CREDENTIAL_HASH_LENGTH = 255;

    /**
     * 创建凭据模型并校验持久化不变量。
     */
    public UserCredential {
        userId = requireText(userId, "userId");
        credentialHash = requireText(credentialHash, "credentialHash");
        if (credentialHash.length() > MAX_CREDENTIAL_HASH_LENGTH) {
            throw new IllegalArgumentException("credentialHash length must not exceed 255 characters");
        }
        if (changedAt == null) {
            throw new IllegalArgumentException("changedAt must not be null");
        }
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("failedAttempts must not be negative");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }

    /**
     * 创建尚未持久化的初始认证凭据。
     *
     * @param userId 用户主键
     * @param credentialHash 不可逆凭据摘要
     * @param changedAt 凭据修改时间
     * @return 新凭据模型
     */
    public static UserCredential create(String userId, String credentialHash, Instant changedAt) {
        return new UserCredential(null, userId, credentialHash, changedAt,
                0, null, 0, null, null);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
