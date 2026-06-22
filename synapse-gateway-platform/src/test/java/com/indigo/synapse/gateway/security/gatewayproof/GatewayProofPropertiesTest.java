package com.indigo.synapse.gateway.security.gatewayproof;

import com.indigo.synapse.security.autoconfigure.SynapseSecurityAutoConfiguration;
import com.indigo.synapse.security.gatewayproof.GatewayProofSigner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GatewayProofProperties} 启动期约束测试。
 */
class GatewayProofPropertiesTest {

    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SynapseSecurityAutoConfiguration.class))
            .withUserConfiguration(GatewayProofConfiguration.class);

    @Test
    void shouldUseSafeDefaultsWhenDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            GatewayProofProperties properties = context.getBean(GatewayProofProperties.class);
            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getGatewayId()).isEqualTo("synapse-gateway");
            assertThat(properties.getSecret()).isEmpty();
        });
    }

    @Test
    void shouldRejectBlankGatewayIdWhenEnabled() {
        contextRunner.withPropertyValues(
                "synapse.gateway.proof.enabled=true",
                "synapse.gateway.proof.gateway-id= ",
                "synapse.gateway.proof.secret=" + VALID_SECRET
        ).run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRejectBlankSecretWhenEnabled() {
        contextRunner.withPropertyValues("synapse.gateway.proof.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRejectShortSecretWhenEnabled() {
        String invalidSecret = "secret-value-that-must-not-leak";
        contextRunner.withPropertyValues(
                "synapse.gateway.proof.enabled=true",
                "synapse.gateway.proof.secret=" + invalidSecret
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(stackMessages(context.getStartupFailure())).doesNotContain(invalidSecret);
        });
    }

    @Test
    void shouldAcceptValidConfigurationWhenEnabled() {
        contextRunner.withPropertyValues(
                "synapse.gateway.proof.enabled=true",
                "synapse.gateway.proof.secret=" + VALID_SECRET
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GatewayProofSigner.class);
            assertThat(context).hasSingleBean(GatewayProofHeaderSanitizationGlobalFilter.class);
            assertThat(context).hasSingleBean(GatewayProofSigningGlobalFilter.class);
        });
    }

    private static String stackMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            messages.append(current.getMessage()).append('\n');
            current = current.getCause();
        }
        return messages.toString();
    }
}
