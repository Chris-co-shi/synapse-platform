package com.indigo.synapse.iam.bootstrap;

import com.indigo.synapse.iam.identity.domain.model.User;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import com.indigo.synapse.iam.identity.domain.repository.UserCredentialRepository;
import com.indigo.synapse.iam.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IAM 本地管理员初始化服务测试。
 */
@ExtendWith(OutputCaptureExtension.class)
class IamBootstrapServiceTest {

    @Test
    void shouldNotExecuteWhenDisabled() {
        Fixture fixture = new Fixture();
        fixture.properties.setEnabled(false);

        fixture.service.initialize();

        assertThat(fixture.users.users).isEmpty();
        assertThat(fixture.credentials.credentials).isEmpty();
    }

    @Test
    void shouldCreateAdminWhenEnabledAndUserMissing() {
        Fixture fixture = new Fixture();
        fixture.enable("admin", "secret-password");

        fixture.service.initialize();

        User user = fixture.users.findByNormalizedUsername("admin").orElseThrow();
        UserCredential credential = fixture.credentials.findByUserId(user.id()).orElseThrow();
        assertThat(user.username()).isEqualTo("admin");
        assertThat(fixture.passwordEncoder.matches("secret-password", credential.credentialHash())).isTrue();
    }

    @Test
    void shouldBeIdempotentWhenExecutedRepeatedly() {
        Fixture fixture = new Fixture();
        fixture.enable("admin", "secret-password");

        fixture.service.initialize();
        fixture.service.initialize();

        assertThat(fixture.users.users).hasSize(1);
        assertThat(fixture.credentials.credentials).hasSize(1);
    }

    @Test
    void shouldFailWhenEnabledWithoutUsername() {
        Fixture fixture = new Fixture();
        fixture.enable(" ", "secret-password");

        assertThatThrownBy(() -> fixture.service.initialize())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("synapse.iam.bootstrap.username");
    }

    @Test
    void shouldFailWhenEnabledWithoutPassword() {
        Fixture fixture = new Fixture();
        fixture.enable("admin", " ");

        assertThatThrownBy(() -> fixture.service.initialize())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("synapse.iam.bootstrap.password");
    }

    @Test
    void shouldNotWriteRawPasswordToLog(CapturedOutput output) {
        Fixture fixture = new Fixture();
        fixture.enable("admin", "secret-password");

        fixture.service.initialize();

        assertThat(output).doesNotContain("secret-password");
    }

    private static final class Fixture {

        private final IamBootstrapProperties properties = new IamBootstrapProperties();
        private final InMemoryUserRepository users = new InMemoryUserRepository();
        private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
        private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        private final IamBootstrapService service =
                new IamBootstrapService(properties, users, credentials, passwordEncoder);

        private void enable(String username, String password) {
            properties.setEnabled(true);
            properties.setUsername(username);
            properties.setPassword(password);
        }
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private long sequence = 1;
        private final Map<String, User> users = new LinkedHashMap<>();

        @Override
        public User save(User user) {
            User saved = user.id() == null || user.id().isBlank()
                    ? new User(String.valueOf(sequence++), user.username(), user.normalizedUsername(),
                    user.displayName(), user.status(), user.revision(), user.createdAt(), user.updatedAt())
                    : user;
            users.put(saved.normalizedUsername(), saved);
            return saved;
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
}
