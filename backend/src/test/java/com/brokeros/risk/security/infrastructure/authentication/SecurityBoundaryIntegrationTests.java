package com.brokeros.risk.security.infrastructure.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.brokeros.risk.api.ApiResponse;
import com.brokeros.risk.security.application.ActorAccessDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.SecurityDependencyUnavailableException;
import com.brokeros.risk.security.application.port.ActorContextProvider;
import com.brokeros.risk.security.application.port.ActorMappingPort;
import com.brokeros.risk.security.application.port.AuthorizationPort;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.MappedActor;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "management.health.db.enabled=false",
        "management.health.redis.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityBoundaryIntegrationTests.SecurityBoundaryTestConfiguration.class)
class SecurityBoundaryIntegrationTests {

    private static final String ISSUER = "https://issuer.brokeros.test";
    private static final String AUDIENCE = "brokeros-risk-test";
    private static final String ACTOR_REF = "7d90281f-eeb3-4e1f-a70c-d71bd5424ed7";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestJwtAuthority jwtAuthority;

    @Autowired
    private SecurityTestState testState;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        testState.reset();
    }

    @Test
    void publicHealthRequiresNoCredential() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        assertSecurityContextCleared();
    }

    @Test
    void protectedRouteWithoutCredentialFailsClosed() throws Exception {
        mockMvc.perform(get("/api/security-test/context"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
        assertThat(testState.mappingCalls()).isZero();
        assertSecurityContextCleared();
    }

    @Test
    void validSignedJwtCreatesMappedActorContextAndExplicitAllow() throws Exception {
        String requestId = "security-request-1";

        mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", bearer(jwtAuthority.valid("mapped-user")))
                        .header("X-Request-ID", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actorRef").value(ACTOR_REF))
                .andExpect(jsonPath("$.data.actorType").value("HUMAN"))
                .andExpect(jsonPath("$.data.requestId").value(requestId))
                .andExpect(jsonPath("$.data.executionId").isNotEmpty());

        assertThat(testState.mappingCalls()).isEqualTo(1);
        assertThat(testState.authorizationCalls()).isEqualTo(1);
        assertSecurityContextCleared();
    }

    @Test
    void callerActorHeadersAndPrivilegeClaimsCannotOverrideMappedActorOrGrant() throws Exception {
        String token = jwtAuthority.token(
                "denied-user",
                ISSUER,
                List.of(AUDIENCE),
                Instant.now().minusSeconds(5),
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300),
                Map.of("scope", "admin superuser", "roles", List.of("ADMIN")));

        mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", bearer(token))
                        .header("X-Actor-Id", "00000000-0000-4000-8000-000000000000")
                        .header("X-User-Id", "administrator")
                        .header("X-Username", "administrator"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_DENIED"))
                .andExpect(jsonPath("$.message").value("Authorization is denied"));

        assertThat(testState.authorizationCalls()).isEqualTo(1);
        assertSecurityContextCleared();
    }

    @Test
    void unknownOrDisabledMappingReturnsGenericActorDenialWithoutProvisioning() throws Exception {
        mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", bearer(jwtAuthority.valid("unknown-user"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACTOR_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Actor access is denied"))
                .andExpect(jsonPath("$.content").doesNotExist());

        assertThat(testState.mappingCalls()).isEqualTo(1);
        assertThat(testState.authorizationCalls()).isZero();
        assertSecurityContextCleared();
    }

    @Test
    void mappingDependencyFailureReturnsSafeServiceUnavailable() throws Exception {
        mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", bearer(jwtAuthority.valid("mapping-down"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SECURITY_DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Security dependency is unavailable"));

        assertThat(testState.authorizationCalls()).isZero();
        assertSecurityContextCleared();
    }

    @Test
    void authorizationDependencyFailureReturnsSafeServiceUnavailable() throws Exception {
        mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", bearer(jwtAuthority.valid("authorization-down"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SECURITY_DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Security dependency is unavailable"));

        assertThat(testState.authorizationCalls()).isEqualTo(1);
        assertSecurityContextCleared();
    }

    @Test
    void forgedIssuerAudienceTimeAndSubjectTokensReturnSameGeneric401() throws Exception {
        TestJwtAuthority otherAuthority = TestJwtAuthority.create();
        List<String> tokens = List.of(
                otherAuthority.valid("mapped-user"),
                jwtAuthority.token(
                        "mapped-user",
                        "https://wrong-issuer.test",
                        List.of(AUDIENCE),
                        Instant.now(),
                        Instant.now(),
                        Instant.now().plusSeconds(300),
                        Map.of()),
                jwtAuthority.token(
                        "mapped-user",
                        ISSUER,
                        List.of("wrong-audience"),
                        Instant.now(),
                        Instant.now(),
                        Instant.now().plusSeconds(300),
                        Map.of()),
                jwtAuthority.token(
                        "mapped-user",
                        ISSUER,
                        List.of(AUDIENCE),
                        Instant.now().minusSeconds(300),
                        Instant.now().minusSeconds(300),
                        Instant.now().minusSeconds(120),
                        Map.of()),
                jwtAuthority.token(
                        "mapped-user",
                        ISSUER,
                        List.of(AUDIENCE),
                        Instant.now(),
                        Instant.now().plusSeconds(120),
                        Instant.now().plusSeconds(300),
                        Map.of()),
                jwtAuthority.token(
                        null,
                        ISSUER,
                        List.of(AUDIENCE),
                        Instant.now(),
                        Instant.now(),
                        Instant.now().plusSeconds(300),
                        Map.of()));

        for (String token : tokens) {
            MvcResult result = mockMvc.perform(get("/api/security-test/context")
                            .header("Authorization", bearer(token)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTHENTICATION_INVALID"))
                    .andReturn();
            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain(token, "mapped-user", "issuer");
            assertSecurityContextCleared();
        }
        assertThat(testState.mappingCalls()).isZero();
    }

    @Test
    void malformedBearerValueReturnsGeneric401() throws Exception {
        mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_INVALID"));
        assertSecurityContextCleared();
    }

    @Test
    void sequentialRequestsReceiveFreshExecutionContextsWithoutLeakage() throws Exception {
        String token = jwtAuthority.valid("mapped-user");
        String firstExecutionId = mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertSecurityContextCleared();

        String secondExecutionId = mockMvc.perform(get("/api/security-test/context")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertSecurityContextCleared();

        assertThat(firstExecutionId).isNotEqualTo(secondExecutionId);
    }

    private void assertSecurityContextCleared() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SecurityBoundaryTestConfiguration {

        @Bean
        TestJwtAuthority testJwtAuthority() {
            return TestJwtAuthority.create();
        }

        @Bean
        @Primary
        JwtDecoder testJwtDecoder(TestJwtAuthority authority) {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(authority.publicKey()).build();
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(Duration.ofSeconds(60)),
                    new JwtIssuerValidator(ISSUER),
                    new JwtClaimValidator<List<String>>(
                            JwtClaimNames.AUD,
                            audiences -> audiences != null && audiences.contains(AUDIENCE))));
            return decoder;
        }

        @Bean
        SecurityTestState securityTestState() {
            return new SecurityTestState();
        }

        @Bean
        @Primary
        ActorMappingPort testActorMappingPort(SecurityTestState state) {
            return principal -> {
                state.mappingCalls.incrementAndGet();
                return switch (principal.externalPrincipalKey().subject()) {
                    case "mapped-user", "denied-user", "authorization-down" ->
                            new MappedActor(new ActorRef(ACTOR_REF), ActorType.HUMAN, 4);
                    case "mapping-down" -> throw new SecurityDependencyUnavailableException(
                            new IllegalStateException("synthetic unavailable dependency"));
                    default -> throw new ActorAccessDeniedException();
                };
            };
        }

        @Bean
        @Primary
        AuthorizationPort testAuthorizationPort(SecurityTestState state) {
            return (actorContext, capability) -> {
                state.authorizationCalls.incrementAndGet();
                String subject = actorContext.externalPrincipalKey().subject();
                if ("authorization-down".equals(subject)) {
                    throw new SecurityDependencyUnavailableException(
                            new IllegalStateException("synthetic unavailable dependency"));
                }
                if ("denied-user".equals(subject)) {
                    return AuthorizationDecision.deny(
                            actorContext.actorRef(),
                            capability,
                            AuthorizationReason.CAPABILITY_NOT_GRANTED,
                            Instant.now(),
                            4L,
                            null);
                }
                return AuthorizationDecision.allow(
                        actorContext.actorRef(),
                        capability,
                        Instant.now(),
                        4,
                        9);
            };
        }

        @Bean
        TestProtectedUseCase testProtectedUseCase(AuthorizationGuard authorizationGuard) {
            return new TestProtectedUseCase(authorizationGuard);
        }

        @Bean
        TestSecurityController testSecurityController(
                ActorContextProvider actorContextProvider,
                TestProtectedUseCase useCase) {
            return new TestSecurityController(actorContextProvider, useCase);
        }
    }

    static final class SecurityTestState {

        private final AtomicInteger mappingCalls = new AtomicInteger();
        private final AtomicInteger authorizationCalls = new AtomicInteger();

        int mappingCalls() {
            return mappingCalls.get();
        }

        int authorizationCalls() {
            return authorizationCalls.get();
        }

        void reset() {
            mappingCalls.set(0);
            authorizationCalls.set(0);
        }
    }

    static final class TestProtectedUseCase {

        private static final Capability READ_CAPABILITY =
                new Capability("security-test:read");

        private final AuthorizationGuard authorizationGuard;

        TestProtectedUseCase(AuthorizationGuard authorizationGuard) {
            this.authorizationGuard = authorizationGuard;
        }

        SecurityContextResponse execute(ActorContext actorContext) {
            authorizationGuard.requireAllowed(actorContext, READ_CAPABILITY);
            return new SecurityContextResponse(
                    actorContext.actorRef().value(),
                    actorContext.actorType().name(),
                    actorContext.executionId().toString(),
                    actorContext.requestId());
        }
    }

    @RestController
    @RequestMapping("/api/security-test")
    static final class TestSecurityController {

        private final ActorContextProvider actorContextProvider;
        private final TestProtectedUseCase useCase;

        TestSecurityController(
                ActorContextProvider actorContextProvider,
                TestProtectedUseCase useCase) {
            this.actorContextProvider = actorContextProvider;
            this.useCase = useCase;
        }

        @GetMapping("/context")
        ResponseEntity<ApiResponse<SecurityContextResponse>> context() {
            return ResponseEntity.ok(ApiResponse.success(
                    useCase.execute(actorContextProvider.currentContext())));
        }
    }

    record SecurityContextResponse(
            String actorRef,
            String actorType,
            String executionId,
            String requestId) {
    }

    static final class TestJwtAuthority {

        private final RSAPublicKey publicKey;
        private final NimbusJwtEncoder encoder;

        private TestJwtAuthority(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
            this.publicKey = publicKey;
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID("ephemeral-test-key")
                    .build();
            this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(
                    new JWKSet(rsaKey)));
        }

        static TestJwtAuthority create() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                return new TestJwtAuthority(
                        (RSAPublicKey) keyPair.getPublic(),
                        (RSAPrivateKey) keyPair.getPrivate());
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot create ephemeral test authority", exception);
            }
        }

        RSAPublicKey publicKey() {
            return publicKey;
        }

        String valid(String subject) {
            Instant now = Instant.now();
            return token(
                    subject,
                    ISSUER,
                    List.of(AUDIENCE),
                    now,
                    now.minusSeconds(1),
                    now.plusSeconds(300),
                    Map.of());
        }

        String token(
                String subject,
                String issuer,
                List<String> audiences,
                Instant issuedAt,
                Instant notBefore,
                Instant expiresAt,
                Map<String, Object> extraClaims) {
            JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .audience(audiences)
                    .issuedAt(issuedAt)
                    .notBefore(notBefore)
                    .expiresAt(expiresAt);
            if (subject != null) {
                claims.subject(subject);
            }
            extraClaims.forEach(claims::claim);
            return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
        }
    }
}
