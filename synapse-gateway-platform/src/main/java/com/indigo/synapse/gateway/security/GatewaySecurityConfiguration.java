package com.indigo.synapse.gateway.security;

import com.indigo.synapse.oauth2.resource.webflux.config.SynapseResourceServerServerHttpSecurityConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway 唯一安全链的 Platform 级补充策略。
 *
 * <p>JWT 解析、issuer/audience/expiry 验证、统一 401/403 和公开路径仍复用 Framework WebFlux
 * Resource Server 配置器。本配置只在 Framework 的默认认证策略之前增加保留管理命名空间权限，
 * 不在 Gateway 实现 IAM 角色或业务授权。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "synapse.security.resource-server", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class GatewaySecurityConfiguration {

    /** Gateway 保留管理命名空间所需的 Token 权限 authority。 */
    public static final String GATEWAY_ADMIN_AUTHORITY = "PERM_gateway:admin";

    /**
     * 创建唯一有效的 Reactive SecurityWebFilterChain。
     *
     * @param http Reactive Security DSL
     * @param resourceServerConfigurer Framework Resource Server 配置器
     * @return 单一 Gateway 安全链
     */
    @Bean
    public SecurityWebFilterChain gatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            SynapseResourceServerServerHttpSecurityConfigurer resourceServerConfigurer) {
        http.authorizeExchange(exchanges -> exchanges
                .pathMatchers("/gateway/admin/**").hasAuthority(GATEWAY_ADMIN_AUTHORITY));
        return resourceServerConfigurer.configure(http).build();
    }
}
