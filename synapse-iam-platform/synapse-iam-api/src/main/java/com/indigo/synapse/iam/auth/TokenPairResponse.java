package com.indigo.synapse.iam.auth;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录或刷新成功后的 Opaque Token 响应。
 *
 * @param accessToken 原始 Opaque Access Token，仅返回给合法客户端
 * @param refreshToken 原始 Opaque Refresh Token，仅返回给合法客户端
 * @param tokenType Token 类型，固定为 Bearer
 * @param expiresIn Access Token 剩余有效秒数
 * @param refreshExpiresIn Refresh Token 空闲剩余有效秒数
 */
public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
