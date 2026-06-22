package com.indigo.synapse.resource.infrastructure.security.gatewayproof;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Redis Adapter fail-closed 与非法 TTL 单元测试。 */
class RedisGatewayProofReplayStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldFailClosedForRedisErrorsNullResultsAndInvalidTtl() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(operations);
        RedisGatewayProofReplayStore store = new RedisGatewayProofReplayStore(template, Duration.ofSeconds(60));

        when(operations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("down"));
        assertThat(store.markIfAbsent("gateway:audience", "nonce", Duration.ofSeconds(30))).isFalse();

        when(operations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(null);
        assertThat(store.markIfAbsent("gateway:audience", "nonce-2", Duration.ofSeconds(30))).isFalse();
        assertThat(store.markIfAbsent("gateway:audience", "nonce-3", Duration.ZERO)).isFalse();
        assertThat(store.markIfAbsent("gateway:audience", "nonce-4", Duration.ofSeconds(-1))).isFalse();
    }
}
