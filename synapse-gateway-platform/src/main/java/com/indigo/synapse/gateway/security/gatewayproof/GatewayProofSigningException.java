package com.indigo.synapse.gateway.security.gatewayproof;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * GatewayProof 出站签发失败异常。
 *
 * <p>签发失败必须关闭转发链，不能降级为空签名或无证明转发。异常消息保持固定且不包含 Token、
 * secret、canonical string、signature 或 nonce 等敏感材料。</p>
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public final class GatewayProofSigningException extends RuntimeException {

    /**
     * 使用非敏感原因创建签发失败异常。
     *
     * @param cause 底层失败原因
     */
    public GatewayProofSigningException(Throwable cause) {
        super("GatewayProof signing failed", cause);
    }
}
