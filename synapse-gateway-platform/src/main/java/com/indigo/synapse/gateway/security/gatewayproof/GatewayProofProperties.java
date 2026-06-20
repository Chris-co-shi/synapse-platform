package com.indigo.synapse.gateway.security.gatewayproof;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway 出站 GatewayProof 配置。
 *
 * <p>该配置只控制 Gateway 为下游请求签发可信入口证明，不启用 Framework 的入站
 * GatewayProof 校验。GatewayProof 不能替代 JWT：Gateway 先验证访问令牌，下游服务仍应
 * 独立验证 JWT，并额外验证本证明。</p>
 */
@ConfigurationProperties(prefix = "synapse.gateway.proof")
public class GatewayProofProperties {

    /** 是否为转发请求签发 GatewayProof；关闭时仍会清理外部伪造 Header。 */
    private boolean enabled;

    /** Gateway 的稳定标识，下游服务按此标识选择可信密钥。 */
    private String gatewayId = "synapse-gateway";

    /** HMAC secret，只能通过环境变量或 Secret 管理系统注入，禁止记录到日志。 */
    private String secret = "";

    /** @return 是否启用出站证明签发 */
    public boolean isEnabled() {
        return enabled;
    }

    /** @param enabled 是否启用出站证明签发 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return Gateway 稳定标识 */
    public String getGatewayId() {
        return gatewayId;
    }

    /** @param gatewayId Gateway 稳定标识 */
    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    /** @return HMAC secret */
    public String getSecret() {
        return secret;
    }

    /** @param secret HMAC secret */
    public void setSecret(String secret) {
        this.secret = secret;
    }
}
