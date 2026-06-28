package com.indigo.synapse.iam.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indigo.synapse.iam.auth.AuthorizationSnapshot;
import com.indigo.synapse.iam.auth.LogoutRequest;
import com.indigo.synapse.iam.auth.LoginDTO;
import com.indigo.synapse.iam.auth.OpaqueTokenDigest;
import com.indigo.synapse.iam.auth.RefreshTokenRequest;
import com.indigo.synapse.iam.auth.TokenPairResponse;
import com.indigo.synapse.iam.auth.domain.RefreshSession;
import com.indigo.synapse.iam.auth.domain.RefreshSessionRepository;
import com.indigo.synapse.iam.auth.domain.RefreshSessionStatus;
import com.indigo.synapse.iam.auth.infrastructure.redis.AuthorizationSnapshotRepository;
import com.indigo.synapse.iam.identity.domain.model.User;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import com.indigo.synapse.iam.identity.domain.repository.UserCredentialRepository;
import com.indigo.synapse.iam.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IAM 认证应用服务测试。
 */
class AuthApplicationServiceTest {

    @Test
    void shouldLoginAndCreateSnapshotWithoutRawTokenInValue() throws Exception {
        Fixture fixture = new Fixture();

        TokenPairResponse response = fixture.service.login(new LoginDTO("admin", "secret"));

        String accessDigest = OpaqueTokenDigest.sha256Hex(response.accessToken());
        AuthorizationSnapshot snapshot = fixture.snapshots.snapshots.get(accessDigest);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.tokenDigest()).isEqualTo(accessDigest);
        assertThat(snapshot.subjectId()).isEqualTo("user-1");
        String redisValue = new ObjectMapper().findAndRegisterModules().writeValueAsString(snapshot);
        assertThat(redisValue).doesNotContain(response.accessToken(), response.refreshToken(), "secret");
    }

    @Test
    void shouldRotateRefreshTokenAndInvalidateOldAccessToken() {
        Fixture fixture = new Fixture();
        TokenPairResponse first = fixture.service.login(new LoginDTO("admin", "secret"));
        String oldAccessDigest = OpaqueTokenDigest.sha256Hex(first.accessToken());

        TokenPairResponse second = fixture.service.refresh(new RefreshTokenRequest(first.refreshToken()));

        assertThat(second.accessToken()).isNotEqualTo(first.accessToken());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(fixture.snapshots.snapshots).doesNotContainKey(oldAccessDigest);
        assertThat(fixture.snapshots.snapshots)
                .containsKey(OpaqueTokenDigest.sha256Hex(second.accessToken()));
    }

    @Test
    void shouldRejectOldRefreshTokenReuseAndRevokeFamily() {
        Fixture fixture = new Fixture();
        TokenPairResponse first = fixture.service.login(new LoginDTO("admin", "secret"));
        fixture.service.refresh(new RefreshTokenRequest(first.refreshToken()));

        assertThatThrownBy(() -> fixture.service.refresh(new RefreshTokenRequest(first.refreshToken())))
                .isInstanceOf(IamRefreshTokenInvalidException.class);
        assertThat(fixture.sessions.sessions.values())
                .allMatch(session -> session.status() == RefreshSessionStatus.REUSE_DETECTED);
    }

    @Test
    void shouldLogoutCurrentSessionAndDeleteAccessSnapshot() {
        Fixture fixture = new Fixture();
        TokenPairResponse token = fixture.service.login(new LoginDTO("admin", "secret"));

        fixture.service.logout(token.accessToken(), new LogoutRequest(token.refreshToken()));

        assertThat(fixture.snapshots.snapshots)
                .doesNotContainKey(OpaqueTokenDigest.sha256Hex(token.accessToken()));
        RefreshSession session = fixture.sessions.findByRefreshTokenDigest(
                OpaqueTokenDigest.sha256Hex(token.refreshToken())).orElseThrow();
        assertThat(session.status()).isEqualTo(RefreshSessionStatus.REVOKED);
    }

    @Test
    void shouldReturnInfrastructureErrorWhenRedisUnavailableDuringLogin() {
        Fixture fixture = new Fixture();
        fixture.snapshots.failOnSave = true;

        assertThatThrownBy(() -> fixture.service.login(new LoginDTO("admin", "secret")))
                .isInstanceOf(IamAuthInfrastructureException.class);
    }

    private static final class Fixture {

        private final InMemoryUserRepository users = new InMemoryUserRepository();
        private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
        private final InMemoryRefreshSessionRepository sessions = new InMemoryRefreshSessionRepository();
        private final InMemorySnapshotRepository snapshots = new InMemorySnapshotRepository();
        private final AuthApplicationService service;

        private Fixture() {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            User user = new User("user-1", "admin", "admin", "Administrator",
                    com.indigo.synapse.iam.identity.domain.model.UserStatus.ACTIVE,
                    0, Instant.now(), Instant.now());
            users.users.put("admin", user);
            credentials.credentials.put("user-1",
                    UserCredential.create("user-1", encoder.encode("secret"), Instant.now()));
            IamAuthProperties properties = new IamAuthProperties();
            properties.setIssuer("https://iam.test");
            properties.setClientId("synapse-console");
            properties.setAccessTokenTtl(Duration.ofMinutes(15));
            properties.setRefreshTokenIdleTtl(Duration.ofDays(7));
            properties.setRefreshTokenAbsoluteTtl(Duration.ofDays(30));
            service = new AuthApplicationService(
                    users,
                    credentials,
                    sessions,
                    snapshots,
                    encoder,
                    new OpaqueTokenGenerator(),
                    properties
            );
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final Map<String, User> users = new LinkedHashMap<>();

        @Override
        public User save(User user) {
            users.put(user.normalizedUsername(), user);
            return user;
        }

        @Override
        public Optional<User> findById(String id) {
            return users.values().stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByNormalizedUsername(String normalizedUsername) {
            return Optional.ofNullable(users.get(normalizedUsername));
        }

        @Override
        public boolean existsByNormalizedUsername(String normalizedUsername) {
            return users.containsKey(normalizedUsername);
        }
    }

    private static final class InMemoryCredentialRepository implements UserCredentialRepository {

        private final Map<String, UserCredential> credentials = new LinkedHashMap<>();

        @Override
        public UserCredential save(UserCredential credential) {
            credentials.put(credential.userId(), credential);
            return credential;
        }

        @Override
        public Optional<UserCredential> findByUserId(String userId) {
            return Optional.ofNullable(credentials.get(userId));
        }
    }

    private static final class InMemoryRefreshSessionRepository implements RefreshSessionRepository {

        private final Map<String, RefreshSession> sessions = new LinkedHashMap<>();

        @Override
        public RefreshSession insert(RefreshSession session) {
            sessions.put(session.refreshTokenDigest(), session);
            return session;
        }

        @Override
        public Optional<RefreshSession> findByRefreshTokenDigest(String refreshTokenDigest) {
            return Optional.ofNullable(sessions.get(refreshTokenDigest));
        }

        @Override
        public boolean rotateActive(RefreshSession current, String replacedById) {
            RefreshSession stored = sessions.get(current.refreshTokenDigest());
            if (stored == null || stored.status() != RefreshSessionStatus.ACTIVE) {
                return false;
            }
            sessions.put(current.refreshTokenDigest(), new RefreshSession(
                    current.id(),
                    current.familyId(),
                    current.userId(),
                    current.clientId(),
                    current.refreshTokenDigest(),
                    current.accessTokenDigest(),
                    RefreshSessionStatus.ROTATED,
                    current.issuedAt(),
                    current.idleExpiresAt(),
                    current.absoluteExpiresAt(),
                    current.revokedAt(),
                    "ROTATED",
                    replacedById,
                    current.revision() + 1
            ));
            return true;
        }

        @Override
        public void revokeFamily(String familyId, RefreshSessionStatus status, String reason, Instant revokedAt) {
            sessions.replaceAll((key, session) -> session.familyId().equals(familyId)
                    ? new RefreshSession(session.id(), session.familyId(), session.userId(), session.clientId(),
                    session.refreshTokenDigest(), session.accessTokenDigest(), status, session.issuedAt(),
                    session.idleExpiresAt(), session.absoluteExpiresAt(), revokedAt, reason, session.replacedById(),
                    session.revision())
                    : session);
        }

        @Override
        public void revokeSession(String sessionId, String reason, Instant revokedAt) {
            sessions.replaceAll((key, session) -> session.id().equals(sessionId)
                    ? new RefreshSession(session.id(), session.familyId(), session.userId(), session.clientId(),
                    session.refreshTokenDigest(), session.accessTokenDigest(), RefreshSessionStatus.REVOKED,
                    session.issuedAt(), session.idleExpiresAt(), session.absoluteExpiresAt(), revokedAt, reason,
                    session.replacedById(), session.revision())
                    : session);
        }
    }

    private static final class InMemorySnapshotRepository implements AuthorizationSnapshotRepository {

        private final Map<String, AuthorizationSnapshot> snapshots = new LinkedHashMap<>();
        private boolean failOnSave;

        @Override
        public void save(AuthorizationSnapshot snapshot, Duration ttl) {
            if (failOnSave) {
                throw new IamAuthInfrastructureException(new IllegalStateException("redis unavailable"));
            }
            snapshots.put(snapshot.tokenDigest(), snapshot);
        }

        @Override
        public Optional<AuthorizationSnapshot> findByDigest(String tokenDigest) {
            return Optional.ofNullable(snapshots.get(tokenDigest));
        }

        @Override
        public void deleteByDigest(String tokenDigest) {
            snapshots.remove(tokenDigest);
        }
    }
}
