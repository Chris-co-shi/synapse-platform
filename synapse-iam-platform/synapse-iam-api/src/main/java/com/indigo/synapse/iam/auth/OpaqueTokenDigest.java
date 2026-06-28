package com.indigo.synapse.iam.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Opaque Token 摘要规范。
 *
 * <p>Access Token 和 Refresh Token 均使用 SHA-256 小写十六进制摘要作为 Redis key 或数据库
 * 安全摘要。原始 Token 不进入 Redis value、数据库和日志。</p>
 */
public final class OpaqueTokenDigest {

    private static final String SHA_256 = "SHA-256";

    private OpaqueTokenDigest() {
    }

    /**
     * 计算 Opaque Token 的 SHA-256 小写十六进制摘要。
     *
     * @param token 原始 Token
     * @return SHA-256 小写十六进制摘要
     */
    public static String sha256Hex(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
}
