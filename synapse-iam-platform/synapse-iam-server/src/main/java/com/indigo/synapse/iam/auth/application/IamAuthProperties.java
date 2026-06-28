package com.indigo.synapse.iam.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * IAM 认证与 Opaque Token 配置。
 */
@ConfigurationProperties(prefix = "synapse.iam.auth")
public class IamAuthProperties {

    /**
     * Access Token issuer。
     */
    private String issuer = "http://127.0.0.1:8100";

    /**
     * V1 默认 audience。
     */
    private Set<String> audiences = new LinkedHashSet<>(Set.of("synapse-platform"));

    /**
     * 管理端客户端标识。
     */
    private String clientId = "synapse-console";

    /**
     * Access Token 有效期。
     */
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    /**
     * Refresh Token 空闲有效期。
     */
    private Duration refreshTokenIdleTtl = Duration.ofDays(7);

    /**
     * Refresh Token 绝对有效期。
     */
    private Duration refreshTokenAbsoluteTtl = Duration.ofDays(30);

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenIdleTtl() {
        return refreshTokenIdleTtl;
    }

    public void setRefreshTokenIdleTtl(Duration refreshTokenIdleTtl) {
        this.refreshTokenIdleTtl = refreshTokenIdleTtl;
    }

    public Duration getRefreshTokenAbsoluteTtl() {
        return refreshTokenAbsoluteTtl;
    }

    public void setRefreshTokenAbsoluteTtl(Duration refreshTokenAbsoluteTtl) {
        this.refreshTokenAbsoluteTtl = refreshTokenAbsoluteTtl;
    }
}
