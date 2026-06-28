package com.indigo.synapse.iam.auth.infrastructure.redis;

import com.indigo.synapse.iam.auth.AuthorizationSnapshot;

import java.time.Duration;
import java.util.Optional;

/**
 * Opaque Access Token 授权快照 Redis 仓储。
 */
public interface AuthorizationSnapshotRepository {

    /**
     * 保存授权快照。
     *
     * @param snapshot 快照
     * @param ttl Redis TTL
     */
    void save(AuthorizationSnapshot snapshot, Duration ttl);

    /**
     * 按 Access Token 摘要读取授权快照。
     *
     * @param tokenDigest Access Token 摘要
     * @return 快照
     */
    Optional<AuthorizationSnapshot> findByDigest(String tokenDigest);

    /**
     * 删除授权快照。
     *
     * @param tokenDigest Access Token 摘要
     */
    void deleteByDigest(String tokenDigest);
}
