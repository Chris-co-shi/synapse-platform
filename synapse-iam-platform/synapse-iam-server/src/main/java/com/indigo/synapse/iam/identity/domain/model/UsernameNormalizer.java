package com.indigo.synapse.iam.identity.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 用户名规范化工具。
 *
 * <p>登录标识统一执行首尾空白清理、Unicode NFKC 规范化和区域无关小写转换，
 * 数据库唯一约束必须作用于规范化结果。</p>
 */
public final class UsernameNormalizer {

    private static final int MAX_USERNAME_LENGTH = 64;

    private UsernameNormalizer() {
    }

    /**
     * 规范化用户名。
     *
     * @param username 原始用户名
     * @return 规范化用户名
     * @throws IllegalArgumentException 用户名为空或长度超限时抛出
     */
    public static String normalize(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username must not be null");
        }
        String normalized = Normalizer.normalize(username.trim(), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (normalized.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("username length must not exceed 64 characters");
        }
        return normalized;
    }
}
