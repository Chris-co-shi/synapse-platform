package com.indigo.synapse.iam.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * IAM 用户身份主体。
 *
 * @param id 用户主键，新建且尚未持久化时可以为空
 * @param username 用户输入和展示使用的用户名
 * @param normalizedUsername 登录查询和唯一约束使用的规范化用户名
 * @param displayName 展示名称
 * @param status 用户状态
 * @param version 乐观锁版本
 * @param createdAt 创建时间，尚未持久化时可以为空
 * @param updatedAt 更新时间，尚未持久化时可以为空
 */
public record User(
        String id,
        String username,
        String normalizedUsername,
        String displayName,
        UserStatus status,
        int version,
        Instant createdAt,
        Instant updatedAt) {

    private static final int MAX_USERNAME_LENGTH = 64;
    private static final int MAX_DISPLAY_NAME_LENGTH = 128;

    /**
     * 创建用户模型并校验持久化不变量。
     */
    public User {
        username = requireText(username, "username");
        normalizedUsername = requireText(normalizedUsername, "normalizedUsername");
        displayName = requireText(displayName, "displayName");
        status = Objects.requireNonNull(status, "status must not be null");
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("username length must not exceed 64 characters");
        }
        if (normalizedUsername.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("normalizedUsername length must not exceed 64 characters");
        }
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("displayName length must not exceed 128 characters");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    /**
     * 创建尚未持久化的启用用户。
     *
     * @param username 用户名
     * @param displayName 展示名称
     * @return 新用户模型
     */
    public static User create(String username, String displayName) {
        String normalizedUsername = UsernameNormalizer.normalize(username);
        return new User(null, username.trim(), normalizedUsername, displayName.trim(),
                UserStatus.ACTIVE, 0, null, null);
    }

    /**
     * 生成修改展示名称后的新模型，保留主键和乐观锁版本。
     *
     * @param newDisplayName 新展示名称
     * @return 修改后的用户模型
     */
    public User changeDisplayName(String newDisplayName) {
        return new User(id, username, normalizedUsername, newDisplayName, status,
                version, createdAt, updatedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
