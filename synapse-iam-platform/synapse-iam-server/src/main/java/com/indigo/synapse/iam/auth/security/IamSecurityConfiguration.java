package com.indigo.synapse.iam.auth.security;

import com.indigo.synapse.iam.auth.application.AuthorizationSnapshotValidator;
import com.indigo.synapse.iam.auth.application.IamAuthProperties;
import com.indigo.synapse.oauth2.resource.webmvc.gatewayproof.GatewayProofAccessDeniedHandler;
import com.indigo.synapse.oauth2.resource.webmvc.gatewayproof.GatewayProofVerificationFilter;
import com.indigo.synapse.security.autoconfigure.SynapseSecurityProperties;
import com.indigo.synapse.security.gatewayproof.GatewayProofReplayStore;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import com.indigo.synapse.security.gatewayproof.GatewayProofVerifier;
import com.indigo.synapse.security.gatewayproof.HmacSha256GatewayProofVerifier;
import com.indigo.synapse.webmvc.exception.WebErrorResponseWriter;
import com.indigo.synapse.webmvc.exception.WebExceptionResponseFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;
import java.util.List;
import java.util.Map;

/**
 * IAM Opaque Token 与 GatewayProof 安全链配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IamAuthProperties.class)
public class IamSecurityConfiguration {

    private static final List<String> PERMIT_PATHS = List.of(
            "/actuator/health",
            "/actuator/info",
            "/error",
            "/auth/login",
            "/auth/refresh"
    );

    @Bean
    public SecurityFilterChain iamSecurityFilterChain(
            HttpSecurity http,
            ObjectProvider<GatewayProofVerificationFilter> gatewayProofVerificationFilterProvider,
            IamOpaqueTokenAuthenticationFilter opaqueTokenAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(PERMIT_PATHS.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(opaqueTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        GatewayProofVerificationFilter gatewayProofVerificationFilter =
                gatewayProofVerificationFilterProvider.getIfAvailable();
        if (gatewayProofVerificationFilter != null) {
            http.addFilterBefore(gatewayProofVerificationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public IamOpaqueTokenAuthenticationFilter iamOpaqueTokenAuthenticationFilter(
            AuthorizationSnapshotValidator snapshotValidator,
            AuthorizationSnapshotAuthenticationMapper authenticationMapper,
            WebExceptionResponseFactory responseFactory,
            WebErrorResponseWriter responseWriter) {
        return new IamOpaqueTokenAuthenticationFilter(
                PERMIT_PATHS, snapshotValidator, authenticationMapper, responseFactory, responseWriter);
    }

    @Bean
    @ConditionalOnProperty(prefix = "synapse.security.gateway-proof", name = "enabled", havingValue = "true")
    public GatewayProofAccessDeniedHandler gatewayProofAccessDeniedHandler(
            WebExceptionResponseFactory responseFactory,
            WebErrorResponseWriter responseWriter) {
        return new GatewayProofAccessDeniedHandler(responseFactory, responseWriter);
    }

    @Bean
    @ConditionalOnProperty(prefix = "synapse.security.gateway-proof", name = "enabled", havingValue = "true")
    public GatewayProofVerifier gatewayProofVerifier(
            SynapseSecurityProperties securityProperties,
            ObjectProvider<GatewayProofReplayStore> replayStoreProvider) {
        SynapseSecurityProperties.GatewayProof gatewayProof = securityProperties.getGatewayProof();
        return new HmacSha256GatewayProofVerifier(
                Map.of(gatewayProof.getGatewayId(), gatewayProof.getSecret()),
                gatewayProof.getTimestampSkew(),
                Clock.systemUTC(),
                replayStoreProvider.getIfAvailable(),
                gatewayProof.isReplayProtectionEnabled(),
                gatewayProof.isFailFast()
        );
    }

    @Bean
    @ConditionalOnProperty(prefix = "synapse.security.gateway-proof", name = "enabled", havingValue = "true")
    public GatewayProofVerificationFilter gatewayProofVerificationFilter(
            SynapseSecurityProperties securityProperties,
            GatewayProofVerifier gatewayProofVerifier,
            GatewayProofTokenHasher tokenHasher,
            GatewayProofAccessDeniedHandler accessDeniedHandler) {
        return new GatewayProofVerificationFilter(
                securityProperties.getGatewayProof(),
                gatewayProofVerifier,
                tokenHasher,
                accessDeniedHandler
        );
    }
}
