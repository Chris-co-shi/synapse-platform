package com.indigo.synapse.iam.auth.infrastructure.gatewayproof;

import com.indigo.synapse.iam.auth.application.IamAuthInfrastructureException;
import com.indigo.synapse.security.gatewayproof.GatewayProofReplayStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 基于 Redis 的 GatewayProof nonce 重放保护存储。
 */
@Component
@RequiredArgsConstructor
public class RedisGatewayProofReplayStore implements GatewayProofReplayStore {

    private static final String PREFIX = "synapse:iam:gateway-proof-nonce:";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean markIfAbsent(String gatewayId, String nonce, Duration ttl) {
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(PREFIX + gatewayId + ":" + nonce, "1", ttl);
            return Boolean.TRUE.equals(success);
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            throw new IamAuthInfrastructureException(ex);
        }
    }
}
