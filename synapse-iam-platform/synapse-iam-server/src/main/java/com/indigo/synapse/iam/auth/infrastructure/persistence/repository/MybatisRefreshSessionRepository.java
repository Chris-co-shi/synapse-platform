package com.indigo.synapse.iam.auth.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.indigo.synapse.iam.auth.domain.RefreshSession;
import com.indigo.synapse.iam.auth.domain.RefreshSessionRepository;
import com.indigo.synapse.iam.auth.domain.RefreshSessionStatus;
import com.indigo.synapse.iam.auth.infrastructure.persistence.entity.IamRefreshSessionEntity;
import com.indigo.synapse.iam.auth.infrastructure.persistence.mapper.IamRefreshSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的 Refresh Token 会话仓储。
 */
@Repository
@RequiredArgsConstructor
public class MybatisRefreshSessionRepository implements RefreshSessionRepository {

    private final IamRefreshSessionMapper refreshSessionMapper;

    @Override
    public RefreshSession insert(RefreshSession session) {
        IamRefreshSessionEntity entity = IamRefreshSessionEntity.fromDomain(session);
        refreshSessionMapper.insert(entity);
        return entity.toDomain();
    }

    @Override
    public Optional<RefreshSession> findByRefreshTokenDigest(String refreshTokenDigest) {
        IamRefreshSessionEntity entity = refreshSessionMapper.selectOne(
                Wrappers.<IamRefreshSessionEntity>lambdaQuery()
                        .eq(IamRefreshSessionEntity::getRefreshTokenDigest, refreshTokenDigest));
        return Optional.ofNullable(entity).map(IamRefreshSessionEntity::toDomain);
    }

    @Override
    public boolean rotateActive(RefreshSession current, String replacedById) {
        IamRefreshSessionEntity entity = new IamRefreshSessionEntity();
        entity.setStatus(RefreshSessionStatus.ROTATED.name());
        entity.setReplacedById(replacedById);
        entity.setRevision(current.revision());
        int updated = refreshSessionMapper.update(entity,
                Wrappers.<IamRefreshSessionEntity>lambdaUpdate()
                        .eq(IamRefreshSessionEntity::getId, current.id())
                        .eq(IamRefreshSessionEntity::getStatus, RefreshSessionStatus.ACTIVE.name())
                        .eq(IamRefreshSessionEntity::getRefreshTokenDigest, current.refreshTokenDigest())
                        .eq(IamRefreshSessionEntity::getRevision, current.revision()));
        return updated == 1;
    }

    @Override
    public void revokeFamily(String familyId, RefreshSessionStatus status, String reason, Instant revokedAt) {
        IamRefreshSessionEntity entity = new IamRefreshSessionEntity();
        entity.setStatus(status.name());
        entity.setRevokedAt(revokedAt);
        entity.setRevokeReason(reason);
        refreshSessionMapper.update(entity,
                Wrappers.<IamRefreshSessionEntity>lambdaUpdate()
                        .eq(IamRefreshSessionEntity::getFamilyId, familyId));
    }

    @Override
    public void revokeSession(String sessionId, String reason, Instant revokedAt) {
        IamRefreshSessionEntity entity = new IamRefreshSessionEntity();
        entity.setStatus(RefreshSessionStatus.REVOKED.name());
        entity.setRevokedAt(revokedAt);
        entity.setRevokeReason(reason);
        refreshSessionMapper.update(entity,
                Wrappers.<IamRefreshSessionEntity>lambdaUpdate()
                        .eq(IamRefreshSessionEntity::getId, sessionId)
                        .eq(IamRefreshSessionEntity::getStatus, RefreshSessionStatus.ACTIVE.name()));
    }
}
