package com.indigo.synapse.resource.infrastructure.security.gatewayproof;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 使用真实 Redis 验证 SET NX + TTL、多实例共享和 audience key 作用域。
 */
class RedisGatewayProofReplayStoreIntegrationTest {

    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate firstTemplate;
    private static StringRedisTemplate secondTemplate;

    @BeforeAll
    static void startRedis() {
        boolean available = DockerClientFactory.instance().isDockerAvailable();
        if (!available && Boolean.getBoolean("synapse.test.redis.required")) {
            fail("Docker is required for the mandatory Redis GatewayProof test job");
        }
        Assumptions.assumeTrue(available, "Docker is unavailable; local Redis integration test skipped");
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        firstTemplate = template(connectionFactory);
        secondTemplate = template(connectionFactory);
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (REDIS.isRunning()) {
            REDIS.stop();
        }
    }

    @Test
    void shouldAtomicallyAcceptFirstUseRejectReplayAndCapTtlAcrossInstances() {
        RedisGatewayProofReplayStore first = new RedisGatewayProofReplayStore(firstTemplate, Duration.ofSeconds(60));
        RedisGatewayProofReplayStore second = new RedisGatewayProofReplayStore(secondTemplate, Duration.ofSeconds(60));

        assertThat(first.markIfAbsent("synapse-gateway:synapse-resource-server", "same-nonce",
                Duration.ofMinutes(5))).isTrue();
        assertThat(second.markIfAbsent("synapse-gateway:synapse-resource-server", "same-nonce",
                Duration.ofMinutes(5))).isFalse();

        Set<String> keys = firstTemplate.keys("synapse:security:gateway-proof:replay:*");
        assertThat(keys).hasSize(1);
        Long ttlMillis = firstTemplate.getExpire(keys.iterator().next(), TimeUnit.MILLISECONDS);
        assertThat(ttlMillis).isPositive().isLessThanOrEqualTo(60_000L);
    }

    @Test
    void shouldScopeSameNonceByAudienceEncodedGatewayId() {
        RedisGatewayProofReplayStore store = new RedisGatewayProofReplayStore(firstTemplate, Duration.ofSeconds(60));

        assertThat(store.markIfAbsent("synapse-gateway:synapse-resource-server", "audience-nonce",
                Duration.ofSeconds(30))).isTrue();
        assertThat(store.markIfAbsent("synapse-gateway:another-service", "audience-nonce",
                Duration.ofSeconds(30))).isTrue();
    }

    private static StringRedisTemplate template(LettuceConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }
}
