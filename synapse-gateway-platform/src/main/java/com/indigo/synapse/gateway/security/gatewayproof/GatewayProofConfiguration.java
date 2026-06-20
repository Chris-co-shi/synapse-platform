package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.gatewayproof.GatewayProofNonceGenerator;
import com.indigo.synapse.security.gatewayproof.GatewayProofSecretValidator;
import com.indigo.synapse.security.gatewayproof.GatewayProofSigner;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * GatewayProof 出站签发配置。
 *
 * <p>配置启动时执行 fail-fast 校验，避免 beta/prd 在密钥缺失时静默降级为无证明转发。
 * 签名器、token 指纹和 nonce 均复用 Framework，Platform 不重复实现密码算法。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayProofProperties.class)
public class GatewayProofConfiguration {

    /**
     * 提供 UTC 时钟。测试可以覆盖该 Bean，以确定性验证时间戳和签名。
     *
     * @return UTC 时钟
     */
    @Bean
    public Clock gatewayProofClock() {
        return Clock.systemUTC();
    }

    /**
     * 创建无状态的 GatewayProof 全局过滤器。
     *
     * @param properties 出站证明配置
     * @param signer Framework 签名器
     * @param tokenHasher Framework token 指纹工具
     * @param nonceGenerator Framework 安全 nonce 生成器
     * @param gatewayProofClock UTC 时钟
     * @return GatewayProof 全局过滤器
     */
    @Bean
    public GatewayProofGlobalFilter gatewayProofGlobalFilter(
            GatewayProofProperties properties,
            GatewayProofSigner signer,
            GatewayProofTokenHasher tokenHasher,
            GatewayProofNonceGenerator nonceGenerator,
            Clock gatewayProofClock) {
        validate(properties);
        return new GatewayProofGlobalFilter(properties, signer, tokenHasher, nonceGenerator, gatewayProofClock);
    }

    private static void validate(GatewayProofProperties properties) {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getGatewayId() == null || properties.getGatewayId().isBlank()) {
            throw new IllegalStateException("GatewayProof gateway-id must not be blank when proof is enabled");
        }
        try {
            GatewayProofSecretValidator.requireValid(properties.getSecret());
        } catch (IllegalArgumentException ex) {
            // 不拼接配置值，避免启动异常间接泄漏 secret。
            throw new IllegalStateException("GatewayProof secret is invalid when proof is enabled", ex);
        }
    }
}
