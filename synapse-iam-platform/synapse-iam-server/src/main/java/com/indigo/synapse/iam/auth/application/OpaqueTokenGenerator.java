package com.indigo.synapse.iam.auth.application;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 高熵 Opaque Token 生成器。
 */
@Component
public class OpaqueTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成 URL 安全、无 padding 的高熵 Opaque Token。
     *
     * @return 原始 Token
     */
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
