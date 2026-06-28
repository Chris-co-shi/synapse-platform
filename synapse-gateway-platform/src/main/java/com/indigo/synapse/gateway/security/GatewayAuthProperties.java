package com.indigo.synapse.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Gateway Opaque Access Token 验证配置。
 */
@ConfigurationProperties(prefix = "synapse.gateway.auth")
public class GatewayAuthProperties {

    /**
     * 期望的授权快照 issuer。
     */
    private String issuer = "http://127.0.0.1:8100";

    /**
     * Gateway 接受的 audience。
     */
    private Set<String> audiences = new LinkedHashSet<>(Set.of("synapse-platform"));

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Set<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(Set<String> audiences) {
        this.audiences = audiences == null ? Set.of() : new LinkedHashSet<>(audiences);
    }
}
