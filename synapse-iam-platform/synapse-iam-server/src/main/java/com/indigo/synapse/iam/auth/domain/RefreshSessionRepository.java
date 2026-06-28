package com.indigo.synapse.iam.auth.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * Refresh Token 会话仓储端口。
 */
public interface RefreshSessionRepository {

    /**
     * 新增会话。
     *
     * @param session 会话
     * @return 持久化后的会话
     */
    RefreshSession insert(RefreshSession session);

    /**
     * 按 Refresh Token 摘要查询会话。
     *
     * @param refreshTokenDigest Refresh Token 摘要
     * @return 会话
     */
    Optional<RefreshSession> findByRefreshTokenDigest(String refreshTokenDigest);

    /**
     * 原子轮换当前 active 会话。
     *
     * @param current 当前会话
     * @param replacedById 新会话标识
     * @return 是否轮换成功
     */
    boolean rotateActive(RefreshSession current, String replacedById);

    /**
     * 撤销会话 family。
     *
     * @param familyId family 标识
     * @param status 目标状态
     * @param reason 撤销原因
     * @param revokedAt 撤销时间
     */
    void revokeFamily(String familyId, RefreshSessionStatus status, String reason, Instant revokedAt);

    /**
     * 撤销当前会话。
     *
     * @param sessionId 会话主键
     * @param reason 撤销原因
     * @param revokedAt 撤销时间
     */
    void revokeSession(String sessionId, String reason, Instant revokedAt);
}
