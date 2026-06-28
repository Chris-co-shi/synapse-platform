package com.indigo.synapse.iam.auth.domain;

import java.time.Instant;

/**
 * Refresh Token 会话状态模型。
 *
 * @param id 会话主键
 * @param familyId Token family 标识
 * @param userId 用户主键
 * @param clientId 客户端标识
 * @param refreshTokenDigest 当前 Refresh Token 摘要
 * @param accessTokenDigest 当前 Access Token 摘要
 * @param status 状态
 * @param issuedAt 签发时间
 * @param idleExpiresAt 空闲过期时间
 * @param absoluteExpiresAt 绝对过期时间
 * @param revokedAt 撤销时间
 * @param revokeReason 撤销原因
 * @param replacedById 替换后的会话主键
 * @param revision 乐观锁版本
 */
public record RefreshSession(
        String id,
        String familyId,
        String userId,
        String clientId,
        String refreshTokenDigest,
        String accessTokenDigest,
        RefreshSessionStatus status,
        Instant issuedAt,
        Instant idleExpiresAt,
        Instant absoluteExpiresAt,
        Instant revokedAt,
        String revokeReason,
        String replacedById,
        Integer revision
) {
}
