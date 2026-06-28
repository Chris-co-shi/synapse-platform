package com.indigo.synapse.iam.bootstrap;

import com.indigo.synapse.iam.identity.domain.model.User;
import com.indigo.synapse.iam.identity.domain.model.UserCredential;
import com.indigo.synapse.iam.identity.domain.model.UsernameNormalizer;
import com.indigo.synapse.iam.identity.domain.repository.UserCredentialRepository;
import com.indigo.synapse.iam.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * IAM 本地开发管理员初始化服务。
 *
 * <p>服务只在 {@link IamBootstrapProperties#isEnabled()} 为 true 时执行。它通过现有用户与凭据
 * Repository 创建管理员，依赖数据库唯一约束保证多实例或并发启动时不会创建重复账号。</p>
 */
@Service
@RequiredArgsConstructor
public class IamBootstrapService {

    private static final Logger LOG = LoggerFactory.getLogger(IamBootstrapService.class);
    private static final String DEFAULT_DISPLAY_NAME = "Local Administrator";

    private final IamBootstrapProperties properties;
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock = Clock.systemUTC();

    /**
     * 按配置执行本地管理员初始化。
     */
    @Transactional
    public void initialize() {
        if (!properties.isEnabled()) {
            LOG.info("IAM bootstrap admin initialization is disabled");
            return;
        }
        String username = requireText(properties.getUsername(), "synapse.iam.bootstrap.username");
        String password = requireText(properties.getPassword(), "synapse.iam.bootstrap.password");
        String normalizedUsername = UsernameNormalizer.normalize(username);
        if (userRepository.findByNormalizedUsername(normalizedUsername).isPresent()) {
            LOG.info("IAM bootstrap admin already exists: username={}", normalizedUsername);
            return;
        }

        try {
            User savedUser = userRepository.save(User.create(username, DEFAULT_DISPLAY_NAME));
            credentialRepository.save(UserCredential.create(
                    savedUser.id(),
                    passwordEncoder.encode(password),
                    Instant.now(clock)
            ));
            LOG.info("IAM bootstrap admin created: username={}", normalizedUsername);
        } catch (DataIntegrityViolationException ex) {
            // 多实例同时启动时可能由唯一约束阻止重复账号；重新查询确认存在后按幂等成功处理。
            if (userRepository.findByNormalizedUsername(normalizedUsername).isPresent()) {
                LOG.info("IAM bootstrap admin already exists after concurrent initialization: username={}",
                        normalizedUsername);
                return;
            }
            throw ex;
        }
    }

    private static String requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank when IAM bootstrap is enabled");
        }
        return value.trim();
    }
}
