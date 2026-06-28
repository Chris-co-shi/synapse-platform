package com.indigo.synapse.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indigo.synapse.webflux.exception.ReactiveWebErrorResponseWriter;
import com.indigo.synapse.webflux.exception.WebFluxExceptionResponseFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

/**
 * Gateway Opaque Token 安全链配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayAuthProperties.class)
public class GatewaySecurityConfiguration {

    /**
     * 创建 Gateway Opaque Token 验证过滤器。
     *
     * @param redisTemplate Reactive Redis 字符串模板
     * @param objectMapper JSON 编解码器
     * @param properties Gateway 认证配置
     * @param responseFactory WebFlux 错误响应工厂
     * @param responseWriter WebFlux 错误写出器
     * @return Opaque Token WebFilter
     */
    @Bean
    public GatewayOpaqueTokenWebFilter gatewayOpaqueTokenWebFilter(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            GatewayAuthProperties properties,
            WebFluxExceptionResponseFactory responseFactory,
            ReactiveWebErrorResponseWriter responseWriter) {
        return new GatewayOpaqueTokenWebFilter(
                redisTemplate,
                objectMapper,
                properties,
                responseFactory,
                responseWriter);
    }

}
