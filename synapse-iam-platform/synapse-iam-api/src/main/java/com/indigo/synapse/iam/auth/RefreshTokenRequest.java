package com.indigo.synapse.iam.auth;

import java.io.Serial;
import java.io.Serializable;

/**
 * Refresh Token rotation 请求。
 *
 * @param refreshToken 原始 Opaque Refresh Token
 */
public record RefreshTokenRequest(String refreshToken) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
