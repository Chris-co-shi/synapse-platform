package com.indigo.synapse.iam.auth.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.indigo.synapse.iam.auth.domain.RefreshSession;
import com.indigo.synapse.iam.auth.domain.RefreshSessionStatus;
import com.indigo.synapse.mybatisplus.entity.VersionedEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * IAM Refresh Token 会话持久化实体。
 */
@Getter
@Setter
@TableName("iam_refresh_session")
public class IamRefreshSessionEntity extends VersionedEntity {

    private String familyId;

    private String userId;

    private String clientId;

    private String refreshTokenDigest;

    private String accessTokenDigest;

    private String status;

    private Instant issuedAt;

    private Instant idleExpiresAt;

    private Instant absoluteExpiresAt;

    private Instant revokedAt;

    private String revokeReason;

    private String replacedById;

    /**
     * 转换为领域模型。
     *
     * @return 领域模型
     */
    public RefreshSession toDomain() {
        return new RefreshSession(
                getId(),
                familyId,
                userId,
                clientId,
                refreshTokenDigest,
                accessTokenDigest,
                RefreshSessionStatus.valueOf(status),
                issuedAt,
                idleExpiresAt,
                absoluteExpiresAt,
                revokedAt,
                revokeReason,
                replacedById,
                getRevision()
        );
    }

    /**
     * 从领域模型创建实体。
     *
     * @param session 领域模型
     * @return 持久化实体
     */
    public static IamRefreshSessionEntity fromDomain(RefreshSession session) {
        IamRefreshSessionEntity entity = new IamRefreshSessionEntity();
        entity.setId(session.id());
        entity.setFamilyId(session.familyId());
        entity.setUserId(session.userId());
        entity.setClientId(session.clientId());
        entity.setRefreshTokenDigest(session.refreshTokenDigest());
        entity.setAccessTokenDigest(session.accessTokenDigest());
        entity.setStatus(session.status().name());
        entity.setIssuedAt(session.issuedAt());
        entity.setIdleExpiresAt(session.idleExpiresAt());
        entity.setAbsoluteExpiresAt(session.absoluteExpiresAt());
        entity.setRevokedAt(session.revokedAt());
        entity.setRevokeReason(session.revokeReason());
        entity.setReplacedById(session.replacedById());
        entity.setRevision(session.revision());
        return entity;
    }
}
