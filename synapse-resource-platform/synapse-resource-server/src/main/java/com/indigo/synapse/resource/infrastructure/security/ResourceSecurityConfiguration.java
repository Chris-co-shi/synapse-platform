package com.indigo.synapse.resource.infrastructure.security;

import com.indigo.synapse.oauth2.resource.webmvc.config.SynapseResourceServerConfigurer;
import com.indigo.synapse.resource.interfaces.rest.ResourceSecurityProbeController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource 服务的最小安全策略扩展。
 *
 * <p>Framework 仍负责 JWT、GatewayProof、异常响应和上下文桥接；Platform 仅声明本服务端点所需的
 * 细粒度权限。该 Bean 会替代 Framework 的条件默认链，最终应用中仍只有一个
 * {@link SecurityFilterChain}。</p>
 */
@Configuration(proxyBeanMethods = false)
public class ResourceSecurityConfiguration {

    private static final String READ_AUTHORITY = "PERM_" + ResourceSecurityProbeController.READ_PERMISSION;

    /**
     * 为权限验收端点追加 authority 约束，并复用 Framework Resource Server 配置器完成其余安全链。
     *
     * @param http Servlet 安全构建器
     * @param configurer Framework Resource Server 配置器
     * @return 唯一的 Resource SecurityFilterChain
     * @throws Exception 安全链构建失败
     */
    @Bean
    public SecurityFilterChain resourceSecurityFilterChain(
            HttpSecurity http,
            SynapseResourceServerConfigurer configurer) throws Exception {
        http.authorizeHttpRequests(registry -> registry
                .requestMatchers("/internal/security/permission")
                .hasAuthority(READ_AUTHORITY));
        return configurer.configure(http).build();
    }
}
