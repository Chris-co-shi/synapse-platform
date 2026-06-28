package com.indigo.synapse.iam.auth;

/**
 * Opaque Token 授权快照中的主体类型。
 *
 * <p>该枚举是 IAM Server、Gateway 和受保护服务共享的稳定协议，不依赖 Spring Security、
 * Redis 或持久化实现。</p>
 */
public enum TokenPrincipalType {

    /**
     * 管理端或外部用户登录后获得的用户 Token。
     */
    USER,

    /**
     * 平台服务或可信机器客户端获得的 Client Token。
     */
    CLIENT
}
