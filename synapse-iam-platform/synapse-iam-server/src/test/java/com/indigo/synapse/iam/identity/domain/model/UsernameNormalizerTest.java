package com.indigo.synapse.iam.identity.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 用户名规范化策略测试。
 */
class UsernameNormalizerTest {

    @Test
    void shouldTrimNormalizeAndLowercaseUsername() {
        assertThat(UsernameNormalizer.normalize("  ＡdMin  ")).isEqualTo("admin");
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UsernameNormalizer.normalize("   "));
    }
}
