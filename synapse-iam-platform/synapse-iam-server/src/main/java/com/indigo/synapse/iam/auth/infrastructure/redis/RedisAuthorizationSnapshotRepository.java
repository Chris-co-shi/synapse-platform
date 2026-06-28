package com.indigo.synapse.iam.auth.infrastructure.redis;

import com.indigo.synapse.cache.CacheValueCodec;
import com.indigo.synapse.cache.redis.RedisCacheStore;
import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.AuthorizationSnapshotKeys;
import com.indigo.synapse.iam.auth.application.IamAuthInfrastructureException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Framework RedisCacheStore 的授权快照仓储。
 */
@Repository
@RequiredArgsConstructor
public class RedisAuthorizationSnapshotRepository implements AuthorizationSnapshotRepository {

    private final RedisCacheStore redisCacheStore;
    private final CacheValueCodec cacheValueCodec;

    @Override
    public void save(AuthorizationSnapshot snapshot, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        try {
            redisCacheStore.put(
                    AuthorizationSnapshotKeys.key(snapshot.tokenDigest()),
                    cacheValueCodec.encode(snapshot),
                    ttl);
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            throw new IamAuthInfrastructureException(ex);
        }
    }

    @Override
    public Optional<AuthorizationSnapshot> findByDigest(String tokenDigest) {
        try {
            return redisCacheStore.get(AuthorizationSnapshotKeys.key(tokenDigest))
                    .map(value -> cacheValueCodec.decode(value, AuthorizationSnapshot.class));
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            throw new IamAuthInfrastructureException(ex);
        }
    }

    @Override
    public void deleteByDigest(String tokenDigest) {
        try {
            redisCacheStore.evict(AuthorizationSnapshotKeys.key(tokenDigest));
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            throw new IamAuthInfrastructureException(ex);
        }
    }
}
