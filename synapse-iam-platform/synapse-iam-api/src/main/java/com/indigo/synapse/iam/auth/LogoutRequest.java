package com.indigo.synapse.iam.auth;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前会话退出请求。
 *
 * @param refreshToken 当前会话的原始 Opaque Refresh Token；为空时只撤销当前 Access Token 快照
 */
public record LogoutRequest(String refreshToken) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
