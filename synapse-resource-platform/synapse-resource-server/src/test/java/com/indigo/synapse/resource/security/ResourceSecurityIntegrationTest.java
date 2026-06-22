package com.indigo.synapse.resource.security;

import com.indigo.synapse.resource.SynapseResourceApplication;
import com.indigo.synapse.resource.interfaces.rest.ResourceSecurityProbeController;
import com.indigo.synapse.security.gatewayproof.GatewayProofCanonicalRequest;
import com.indigo.synapse.security.gatewayproof.GatewayProofHeaders;
import com.indigo.synapse.security.gatewayproof.GatewayProofTokenHasher;
import com.indigo.synapse.security.gatewayproof.GatewayProofVersion;
import com.indigo.synapse.security.gatewayproof.HmacSha256GatewayProofSigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resource 下游独立 JWT、GatewayProof、权限与 Framework 上下文闭环测试。
 */
@SpringBootTest(
        classes = {SynapseResourceApplication.class, ResourceSecurityIntegrationTest.JwtTestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.config.import=",
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "synapse.security.resource-server.issuer-uri=https://iam.test",
                "synapse.security.resource-server.jwk-set-uri=https://iam.test/oauth2/jwks",
                "synapse.security.resource-server.audiences=synapse-resource-server",
                "synapse.security.resource-server.denylist-enabled=false",
                "synapse.security.gateway-proof.enabled=true",
                "synapse.security.gateway-proof.required=true",
                "synapse.security.gateway-proof.gateway-id=synapse-gateway:synapse-resource-server",
                "synapse.security.gateway-proof.secret=0123456789abcdef0123456789abcdef",
                "synapse.security.gateway-proof.timestamp-skew=60s",
                "synapse.security.gateway-proof.replay-protection-enabled=true",
                "synapse.security.gateway-proof.fail-fast=true"
        })
@AutoConfigureMockMvc
class ResourceSecurityIntegrationTest {

    private static final String ISSUER = "https://iam.test";
    private static final String AUDIENCE = "synapse-resource-server";
    private static final String TRUSTED_GATEWAY_ID = "synapse-gateway:synapse-resource-server";
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final KeyPair TRUSTED_KEY_PAIR = generateKeyPair();
    private static final KeyPair UNTRUSTED_KEY_PAIR = generateKeyPair();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void configureRedisFirstUse() {
        reset(redisTemplate);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
    }

    @Test
    void shouldRejectMissingTokenWith401WhenGatewayProofIsValid() throws Exception {
        mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                        "/internal/security/protected", null, null, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMalformedAndInvalidSignatureTokensWith401() throws Exception {
        for (String token : List.of("malformed.jwt", token(TokenSpec.valid(), UNTRUSTED_KEY_PAIR))) {
            mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                            "/internal/security/protected", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void shouldRejectExpiredWrongIssuerAndWrongAudienceTokensWith401() throws Exception {
        for (TokenSpec spec : List.of(TokenSpec.expiredToken(), TokenSpec.wrongIssuer(), TokenSpec.wrongAudience())) {
            String token = token(spec, TRUSTED_KEY_PAIR);
            mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                            "/internal/security/protected", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void shouldIndependentlyValidateJwtAndExposePrincipalAndOperationContexts() throws Exception {
        String token = token(TokenSpec.valid(), TRUSTED_KEY_PAIR);

        mockMvc.perform(withProof(get("/internal/security/context"), "GET",
                        "/internal/security/context", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID)
                        .header("X-User-Id", "forged-user")
                        .header("X-Tenant-Id", "forged-tenant")
                        .header("X-Initiator-Id", "forged-initiator")
                        .header("X-Roles", "forged-role")
                        .header("X-Permissions", ResourceSecurityProbeController.READ_PERMISSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principalId").value("jwt-user"))
                .andExpect(jsonPath("$.actorId").value("jwt-user"))
                .andExpect(jsonPath("$.initiatorId").value("jwt-user"))
                .andExpect(jsonPath("$.tenantId").value("jwt-tenant"));
    }

    @Test
    void shouldReturn403WhenAuthenticatedPrincipalLacksPermission() throws Exception {
        String token = token(TokenSpec.valid(), TRUSTED_KEY_PAIR);

        mockMvc.perform(withProof(get("/internal/security/permission"), "GET",
                        "/internal/security/permission", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowPermissionEndpointWhenJwtContainsPermission() throws Exception {
        String token = token(TokenSpec.withPermission(), TRUSTED_KEY_PAIR);

        mockMvc.perform(withProof(get("/internal/security/permission"), "GET",
                        "/internal/security/permission", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectTamperedSignatureMethodPathFingerprintTimestampAndAudience() throws Exception {
        String token = token(TokenSpec.valid(), TRUSTED_KEY_PAIR);

        MockHttpServletRequestBuilder signatureTampered = withProof(
                get("/internal/security/protected")
                        .header(GatewayProofHeaders.SIGNATURE, "tampered-signature"),
                "GET", "/internal/security/protected", null, token, uniqueNonce(), now(),
                TRUSTED_GATEWAY_ID, true);
        mockMvc.perform(signatureTampered).andExpect(status().isForbidden());

        mockMvc.perform(withProof(post("/internal/security/protected"), "GET",
                        "/internal/security/protected", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isForbidden());

        mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                        "/internal/security/other", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isForbidden());

        mockMvc.perform(withProof(get("/internal/security/protected")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token),
                        "GET", "/internal/security/protected", null, "different-token", uniqueNonce(), now(),
                        TRUSTED_GATEWAY_ID, false))
                .andExpect(status().isForbidden());

        mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                        "/internal/security/protected", null, token, uniqueNonce(), now() - 120_000,
                        TRUSTED_GATEWAY_ID))
                .andExpect(status().isForbidden());

        mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                        "/internal/security/protected", null, token, uniqueNonce(), now(),
                        "synapse-gateway:another-service"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAcceptNonceOnceAndRejectSecondUse() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(true, false);
        String token = token(TokenSpec.valid(), TRUSTED_KEY_PAIR);
        String nonce = uniqueNonce();

        mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                        "/internal/security/protected", null, token, nonce, now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isOk());
        mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                        "/internal/security/protected", null, token, nonce, now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldFailClosedWhenRedisIsUnavailable() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));
        String token = token(TokenSpec.valid(), TRUSTED_KEY_PAIR);

        mockMvc.perform(withProof(get("/internal/security/protected"), "GET",
                        "/internal/security/protected", null, token, uniqueNonce(), now(), TRUSTED_GATEWAY_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDirectAccessWithoutGatewayProof() throws Exception {
        String token = token(TokenSpec.valid(), TRUSTED_KEY_PAIR);

        mockMvc.perform(get("/internal/security/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder withProof(
            MockHttpServletRequestBuilder request,
            String signedMethod,
            String signedPath,
            String query,
            String signedToken,
            String nonce,
            long timestamp,
            String gatewayId) {
        return withProof(request, signedMethod, signedPath, query, signedToken, nonce, timestamp, gatewayId, true);
    }

    private static MockHttpServletRequestBuilder withProof(
            MockHttpServletRequestBuilder request,
            String signedMethod,
            String signedPath,
            String query,
            String signedToken,
            String nonce,
            long timestamp,
            String gatewayId,
            boolean addAuthorizationHeader) {
        String timestampValue = Long.toString(timestamp);
        GatewayProofCanonicalRequest canonicalRequest = new GatewayProofCanonicalRequest(
                GatewayProofVersion.V1,
                gatewayId,
                timestampValue,
                nonce,
                signedMethod,
                signedPath,
                query,
                new GatewayProofTokenHasher().sha256Hex(signedToken)
        );
        String signature = new HmacSha256GatewayProofSigner().sign(canonicalRequest, SECRET);
        request.header(GatewayProofHeaders.VERSION, GatewayProofVersion.V1)
                .header(GatewayProofHeaders.GATEWAY_ID, gatewayId)
                .header(GatewayProofHeaders.TIMESTAMP, timestampValue)
                .header(GatewayProofHeaders.NONCE, nonce)
                .header(GatewayProofHeaders.SIGNATURE, signature);
        if (addAuthorizationHeader && signedToken != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + signedToken);
        }
        return request;
    }

    private static String token(TokenSpec spec, KeyPair keyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID("test-key")
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(spec.issuer())
                .subject("jwt-user")
                .audience(List.of(spec.audience()))
                .issuedAt(now.minusSeconds(10))
                .notBefore(now.minusSeconds(10))
                .expiresAt(spec.expired() ? now.minusSeconds(300) : now.plusSeconds(300))
                .claim("token_type", "ACCESS_TOKEN")
                .claim("principal_type", "USER")
                .claim("preferred_username", "jwt-user-name")
                .claim("tenant_id", "jwt-tenant")
                .claim("roles", List.of("RESOURCE_READER"))
                .claim("permissions", spec.permissions());
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("test-key").build(), claims.build()))
                .getTokenValue();
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static String uniqueNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("could not generate RSA test key", ex);
        }
    }

    private record TokenSpec(String issuer, String audience, boolean expired, Set<String> permissions) {

        private static TokenSpec valid() {
            return new TokenSpec(ISSUER, AUDIENCE, false, Set.of());
        }

        private static TokenSpec withPermission() {
            return new TokenSpec(ISSUER, AUDIENCE, false,
                    Set.of(ResourceSecurityProbeController.READ_PERMISSION));
        }

        private static TokenSpec expiredToken() {
            return new TokenSpec(ISSUER, AUDIENCE, true, Set.of());
        }

        private static TokenSpec wrongIssuer() {
            return new TokenSpec("https://wrong-issuer.test", AUDIENCE, false, Set.of());
        }

        private static TokenSpec wrongAudience() {
            return new TokenSpec(ISSUER, "another-service", false, Set.of());
        }
    }

    /** Test-only real RSA decoder; production still uses configured IAM JWK endpoint. */
    @TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfiguration {

        @Bean
        @Primary
        JwtDecoder testJwtDecoder(
                @Qualifier("synapseSpringJwtValidator") OAuth2TokenValidator<Jwt> validator) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder
                    .withPublicKey((RSAPublicKey) TRUSTED_KEY_PAIR.getPublic())
                    .build();
            decoder.setJwtValidator(validator);
            return decoder;
        }
    }
}
