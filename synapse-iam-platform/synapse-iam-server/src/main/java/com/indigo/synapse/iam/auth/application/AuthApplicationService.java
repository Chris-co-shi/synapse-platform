package com.indigo.synapse.iam.auth.application;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.AuthorizationSnapshotStatus;
import com.indigo.synapse.iam.auth.CurrentUserResponse;
import com.indigo.synapse.iam.auth.LoginDTO;
import com.indigo.synapse.iam.auth.LogoutRequest;
import com.indigo.synapse.iam.auth.OpaqueTokenDigest;
import com.indigo.synapse.iam.auth.RefreshTokenRequest;
import com.indigo.synapse.iam.auth.TokenPairResponse;
import com.indigo.synapse.iam.auth.TokenPrincipalType;
import com.indigo.synapse.iam.auth.domain.RefreshSession;
import com.indigo.synapse.iam.auth.domain.RefreshSessionRepository;
import com.indigo.synapse.iam.auth.domain.RefreshSessionStatus;
import com.indigo.synapse.iam.auth.infrastructure.redis.AuthorizationSnapshotRepository;
import com.indigo.synapse.iam.identity.domain.model.User;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import com.indigo.synapse.iam.identity.domain.model.UserStatus;
import com.indigo.synapse.iam.identity.domain.model.UsernameNormalizer;
import com.indigo.synapse.iam.identity.domain.repository.UserCredentialRepository;
import com.indigo.synapse.iam.identity.domain.repository.UserRepository;
import com.indigo.synapse.security.context.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * IAM 登录、刷新和退出应用服务。
 */
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String LOGOUT_REASON = "LOGOUT";
    private static final String REFRESH_ROTATED_REASON = "ROTATED";
    private static final String REUSE_REASON = "REFRESH_REUSE";

    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final AuthorizationSnapshotRepository snapshotRepository;
    private final PasswordEncoder passwordEncoder;
    private final OpaqueTokenGenerator tokenGenerator;
    private final IamAuthProperties properties;
    private final Clock clock = Clock.systemUTC();

    /**
     * 用户登录并签发 Opaque Access Token 与 Refresh Token。
     *
     * @param request 登录请求
     * @return Token 响应
     */
    @Transactional
    public TokenPairResponse login(LoginDTO request) {
        String username = request == null ? null : request.getUsername();
        String password = request == null ? null : request.getPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IamAuthenticationFailureException();
        }

        User user = userRepository.findByNormalizedUsername(UsernameNormalizer.normalize(username))
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(IamAuthenticationFailureException::new);
        UserCredential credential = credentialRepository.findByUserId(user.id())
                .orElseThrow(IamAuthenticationFailureException::new);
        if (!passwordEncoder.matches(password, credential.credentialHash())) {
            throw new IamAuthenticationFailureException();
        }

        Instant now = clock.instant();
        String accessToken = tokenGenerator.generate();
        String refreshToken = tokenGenerator.generate();
        String accessDigest = OpaqueTokenDigest.sha256Hex(accessToken);
        String refreshDigest = OpaqueTokenDigest.sha256Hex(refreshToken);
        String sessionId = IdWorker.getIdStr();
        String familyId = UUID.randomUUID().toString();
        Instant accessExpiresAt = now.plus(properties.getAccessTokenTtl());
        Instant refreshIdleExpiresAt = now.plus(properties.getRefreshTokenIdleTtl());
        Instant refreshAbsoluteExpiresAt = now.plus(properties.getRefreshTokenAbsoluteTtl());

        refreshSessionRepository.insert(new RefreshSession(
                sessionId,
                familyId,
                user.id(),
                properties.getClientId(),
                refreshDigest,
                accessDigest,
                RefreshSessionStatus.ACTIVE,
                now,
                refreshIdleExpiresAt,
                refreshAbsoluteExpiresAt,
                null,
                null,
                null,
                0
        ));
        saveSnapshot(user, sessionId, accessDigest, now, accessExpiresAt);
        return tokenResponse(accessToken, refreshToken, now, accessExpiresAt, refreshIdleExpiresAt);
    }

    /**
     * Refresh Token rotation。
     *
     * @param request 刷新请求
     * @return 新 Token 响应
     */
    @Transactional
    public TokenPairResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request == null ? null : request.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IamRefreshTokenInvalidException();
        }
        String oldRefreshDigest = OpaqueTokenDigest.sha256Hex(refreshToken);
        RefreshSession current = refreshSessionRepository.findByRefreshTokenDigest(oldRefreshDigest)
                .orElseThrow(IamRefreshTokenInvalidException::new);
        Instant now = clock.instant();
        if (current.status() != RefreshSessionStatus.ACTIVE) {
            refreshSessionRepository.revokeFamily(
                    current.familyId(), RefreshSessionStatus.REUSE_DETECTED, REUSE_REASON, now);
            snapshotRepository.deleteByDigest(current.accessTokenDigest());
            throw new IamRefreshTokenInvalidException();
        }
        if (!now.isBefore(current.idleExpiresAt()) || !now.isBefore(current.absoluteExpiresAt())) {
            refreshSessionRepository.revokeSession(current.id(), "EXPIRED", now);
            snapshotRepository.deleteByDigest(current.accessTokenDigest());
            throw new IamRefreshTokenInvalidException();
        }
        User user = userRepository.findById(current.userId())
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(IamRefreshTokenInvalidException::new);

        String newAccessToken = tokenGenerator.generate();
        String newRefreshToken = tokenGenerator.generate();
        String newAccessDigest = OpaqueTokenDigest.sha256Hex(newAccessToken);
        String newRefreshDigest = OpaqueTokenDigest.sha256Hex(newRefreshToken);
        String nextSessionId = IdWorker.getIdStr();
        Instant accessExpiresAt = now.plus(properties.getAccessTokenTtl());
        Instant idleExpiresAt = now.plus(properties.getRefreshTokenIdleTtl());
        Instant boundedIdleExpiresAt = idleExpiresAt.isAfter(current.absoluteExpiresAt())
                ? current.absoluteExpiresAt()
                : idleExpiresAt;

        boolean rotated = refreshSessionRepository.rotateActive(current, nextSessionId);
        if (!rotated) {
            refreshSessionRepository.revokeFamily(
                    current.familyId(), RefreshSessionStatus.REUSE_DETECTED, REUSE_REASON, now);
            snapshotRepository.deleteByDigest(current.accessTokenDigest());
            throw new IamRefreshTokenInvalidException();
        }

        refreshSessionRepository.insert(new RefreshSession(
                nextSessionId,
                current.familyId(),
                current.userId(),
                current.clientId(),
                newRefreshDigest,
                newAccessDigest,
                RefreshSessionStatus.ACTIVE,
                now,
                boundedIdleExpiresAt,
                current.absoluteExpiresAt(),
                null,
                null,
                null,
                0
        ));
        snapshotRepository.deleteByDigest(current.accessTokenDigest());
        saveSnapshot(user, nextSessionId, newAccessDigest, now, accessExpiresAt);
        return tokenResponse(newAccessToken, newRefreshToken, now, accessExpiresAt, boundedIdleExpiresAt);
    }

    /**
     * 退出当前会话并撤销当前 Access Token 快照。
     *
     * @param accessToken 当前 Access Token
     * @param request 退出请求
     */
    @Transactional
    public void logout(String accessToken, LogoutRequest request) {
        String accessDigest = OpaqueTokenDigest.sha256Hex(accessToken);
        snapshotRepository.deleteByDigest(accessDigest);
        String refreshToken = request == null ? null : request.refreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        String refreshDigest = OpaqueTokenDigest.sha256Hex(refreshToken);
        refreshSessionRepository.findByRefreshTokenDigest(refreshDigest)
                .ifPresent(session -> refreshSessionRepository.revokeSession(
                        session.id(), LOGOUT_REASON, clock.instant()));
    }

    /**
     * 当前用户响应。
     *
     * @param user 已认证用户主体
     * @return 当前用户响应
     */
    public CurrentUserResponse currentUser(AuthenticatedUser user) {
        return new CurrentUserResponse(
                user.userId(),
                user.username(),
                user.displayName(),
                user.tenantId(),
                user.roles(),
                user.permissions()
        );
    }

    private void saveSnapshot(User user, String sessionId, String accessDigest, Instant issuedAt, Instant expiresAt) {
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                accessDigest,
                user.id(),
                TokenPrincipalType.USER,
                properties.getClientId(),
                sessionId,
                user.displayName(),
                null,
                Set.of(),
                Set.of(),
                properties.getIssuer(),
                properties.getAudiences(),
                0,
                issuedAt,
                expiresAt,
                AuthorizationSnapshotStatus.ACTIVE
        );
        snapshotRepository.save(snapshot, Duration.between(issuedAt, expiresAt));
    }

    private static TokenPairResponse tokenResponse(
            String accessToken,
            String refreshToken,
            Instant now,
            Instant accessExpiresAt,
            Instant refreshExpiresAt) {
        return new TokenPairResponse(
                accessToken,
                refreshToken,
                TOKEN_TYPE,
                Duration.between(now, accessExpiresAt).toSeconds(),
                Duration.between(now, refreshExpiresAt).toSeconds()
        );
    }
}
