package com.indigo.synapse.resource.infrastructure.security.gatewayproof;

import com.indigo.synapse.security.autoconfigure.SynapseSecurityProperties;
import com.indigo.synapse.security.gatewayproof.GatewayProofReplayStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Redis GatewayProof nonce 重放保护 Adapter。
 *
 * <p>{@link StringRedisTemplate#opsForValue()} 的带 TTL {@code setIfAbsent} 在 Redis 中以单条
 * {@code SET key value NX PX ttl}（或等价原子 SET 过期语义）执行，不存在 {@code exists + save}
 * 竞争窗口。生产多实例共享同一 Redis，因此同一个 audience-scoped gatewayId 与 nonce 只有首次标记成功。</p>
 *
 * <p>Framework 传入的 TTL 会再次限制在 GatewayProof timestamp window 以内。任何 Redis 异常、空返回或
 * 非法 TTL 均返回 {@code false}，由验签器拒绝请求，实现 fail closed。日志不包含 nonce、key 或 secret。</p>
 */
@Component
public final class RedisGatewayProofReplayStore implements GatewayProofReplayStore {

    private static final Logger LOG = LoggerFactory.getLogger(RedisGatewayProofReplayStore.class);
    private static final String KEY_PREFIX = "synapse:security:gateway-proof:replay:";
    private static final String MARKER = "1";

    private final StringRedisTemplate redisTemplate;
    private final Duration maximumTtl;

    /**
     * 创建生产 Redis Adapter。
     *
     * @param redisTemplate Spring Data Redis 原子命令入口
     * @param securityProperties GatewayProof 有效窗口配置
     */
    @Autowired
    public RedisGatewayProofReplayStore(
            StringRedisTemplate redisTemplate,
            SynapseSecurityProperties securityProperties) {
        this(redisTemplate, securityProperties.getGatewayProof().getTimestampSkew());
    }

    RedisGatewayProofReplayStore(StringRedisTemplate redisTemplate, Duration maximumTtl) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.maximumTtl = Objects.requireNonNull(maximumTtl, "maximumTtl must not be null");
    }

    /**
     * 原子标记 audience-scoped Gateway ID 与 nonce。
     *
     * @param gatewayId Framework 传入的可信 Gateway 标识；Platform 已编码 route audience
     * @param nonce 一次性随机值
     * @param ttl Framework 根据 timestamp 剩余窗口计算的 TTL
     * @return 首次使用且 Redis 写入成功返回 true；重复、异常或非法参数均返回 false
     */
    @Override
    public boolean markIfAbsent(String gatewayId, String nonce, Duration ttl) {
        Duration effectiveTtl = effectiveTtl(ttl);
        if (gatewayId == null || gatewayId.isBlank() || nonce == null || nonce.isBlank() || effectiveTtl == null) {
            return false;
        }
        try {
            Boolean stored = redisTemplate.opsForValue().setIfAbsent(
                    redisKey(gatewayId, nonce), MARKER, effectiveTtl);
            return Boolean.TRUE.equals(stored);
        } catch (RuntimeException ex) {
            LOG.error("GatewayProof Redis replay store failed closed: errorType={}",
                    ex.getClass().getSimpleName());
            return false;
        }
    }

    private Duration effectiveTtl(Duration requestedTtl) {
        if (requestedTtl == null || requestedTtl.isZero() || requestedTtl.isNegative()
                || maximumTtl.isZero() || maximumTtl.isNegative()) {
            return null;
        }
        Duration ttl = requestedTtl.compareTo(maximumTtl) > 0 ? maximumTtl : requestedTtl;
        return ttl.isZero() || ttl.isNegative() ? null : ttl;
    }

    private static String redisKey(String gatewayId, String nonce) {
        return KEY_PREFIX + sha256Hex(gatewayId) + ':' + sha256Hex(nonce);
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
