package com.indigo.synapse.iam.identity.infrastructure.persistence;

import com.indigo.synapse.iam.identity.domain.model.User;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import com.indigo.synapse.iam.identity.domain.repository.UserCredentialRepository;
import com.indigo.synapse.iam.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IAM 身份与认证凭据 PostgreSQL 持久化集成测试。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.config.import=",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.datasource.dynamic.enabled=false",
        "synapse.security.resource-server.enabled=false"
})
@Transactional
class IamIdentityPersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @DynamicPropertySource
    static void registerDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource." + "password", POSTGRESQL::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRESQL::getDriverClassName);
    }

    @Test
    void shouldApplyIdentityMigrationToEmptyDatabase() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('iam_user', 'iam_user_credential')
                """, Integer.class);

        assertThat(tableCount).isEqualTo(2);
    }

    @Test
    void shouldPersistAndQueryUserByNormalizedUsername() {
        User saved = userRepository.save(User.create(" Admin ", "Administrator"));
        User queried = userRepository.findByNormalizedUsername("admin").orElseThrow();

        assertThat(saved.id()).isNotBlank();
        assertThat(saved.version()).isZero();
        assertThat(saved.createdAt()).isNotNull();
        assertThat(queried.id()).isEqualTo(saved.id());
        assertThat(queried.username()).isEqualTo("Admin");
        assertThat(queried.normalizedUsername()).isEqualTo("admin");
        assertThat(userRepository.existsByNormalizedUsername("admin")).isTrue();
    }

    @Test
    void shouldEnforceNormalizedUsernameUniqueness() {
        userRepository.save(User.create("Admin", "Administrator"));

        assertThatThrownBy(() -> userRepository.save(User.create(" admin ", "Other Administrator")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistOnlyOneAuthenticationMaterialPerUser() {
        User user = userRepository.save(User.create("operator", "Operator"));
        credentialRepository.save(UserCredential.create(user.id(), "{test}digest-1", Instant.now()));

        assertThat(credentialRepository.findByUserId(user.id())).isPresent();
        assertThatThrownBy(() -> credentialRepository.save(
                UserCredential.create(user.id(), "{test}digest-2", Instant.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectStaleUserUpdateWithOptimisticLock() {
        User saved = userRepository.save(User.create("reviewer", "Reviewer"));
        User firstSnapshot = userRepository.findById(saved.id()).orElseThrow();
        User staleSnapshot = userRepository.findById(saved.id()).orElseThrow();

        userRepository.save(firstSnapshot.changeDisplayName("Primary Reviewer"));

        assertThatThrownBy(() -> userRepository.save(staleSnapshot.changeDisplayName("Stale Reviewer")))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
