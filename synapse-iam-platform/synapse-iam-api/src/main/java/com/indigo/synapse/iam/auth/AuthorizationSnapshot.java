package com.indigo.synapse.iam.auth;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Opaque Access Token 授权快照。
 *
 * <p>该对象是 IAM 写入 Redis、Gateway 和 IAM Resource Server 读取验证的共享协议。
 * value 不包含原始 Access Token、Refresh Token、密码、Client Secret、私钥或 GatewayProof
 * canonical string。</p>
 *
 * @param tokenDigest Access Token 的 SHA-256 小写十六进制摘要
 * @param subjectId 主体稳定标识
 * @param principalType 主体类型
 * @param clientId 客户端标识
 * @param sessionId Refresh Token 会话标识
 * @param displayName 展示名称
 * @param tenantId 租户标识
 * @param roles 角色快照
 * @param permissions 权限快照
 * @param issuer 签发方
 * @param audiences 接收方集合
 * @param authorizationVersion 授权版本
 * @param issuedAt 签发时间
 * @param expiresAt 过期时间
 * @param status 快照状态
 */
public record AuthorizationSnapshot(
        String tokenDigest,
        String subjectId,
        TokenPrincipalType principalType,
        String clientId,
        String sessionId,
        String displayName,
        String tenantId,
        Set<String> roles,
        Set<String> permissions,
        String issuer,
        Set<String> audiences,
        long authorizationVersion,
        Instant issuedAt,
        Instant expiresAt,
        AuthorizationSnapshotStatus status
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规范化集合字段并校验核心协议字段。
     */
    public AuthorizationSnapshot {
        tokenDigest = requireText(tokenDigest, "tokenDigest");
        subjectId = requireText(subjectId, "subjectId");
        principalType = requireNonNull(principalType, "principalType");
        clientId = requireText(clientId, "clientId");
        sessionId = requireText(sessionId, "sessionId");
        displayName = requireText(displayName, "displayName");
        issuer = requireText(issuer, "issuer");
        issuedAt = requireNonNull(issuedAt, "issuedAt");
        expiresAt = requireNonNull(expiresAt, "expiresAt");
        status = requireNonNull(status, "status");
        roles = normalize(roles);
        permissions = normalize(permissions);
        audiences = normalize(audiences);
        if (audiences.isEmpty()) {
            throw new IllegalArgumentException("audiences must not be empty");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
        if (authorizationVersion < 0) {
            throw new IllegalArgumentException("authorizationVersion must not be negative");
        }
    }

    /**
     * 判断快照在指定时间点是否有效。
     *
     * @param now 当前时间
     * @return 状态为 ACTIVE 且未过期时返回 true
     */
    public boolean activeAt(Instant now) {
        Instant checkedNow = requireNonNull(now, "now");
        return status == AuthorizationSnapshotStatus.ACTIVE && checkedNow.isBefore(expiresAt);
    }

    /**
     * 判断快照 issuer 和 audience 是否满足当前服务要求。
     *
     * @param expectedIssuer 期望签发方
     * @param acceptedAudiences 当前服务接受的 audience
     * @return issuer 相等且至少一个 audience 匹配时返回 true
     */
    public boolean matches(String expectedIssuer, Set<String> acceptedAudiences) {
        if (!issuer.equals(expectedIssuer)) {
            return false;
        }
        Set<String> normalizedAccepted = normalize(acceptedAudiences);
        if (normalizedAccepted.isEmpty()) {
            return false;
        }
        return audiences.stream().anyMatch(normalizedAccepted::contains);
    }

    private static Set<String> normalize(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : source) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim());
            }
        }
        return Set.copyOf(normalized);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }
}
