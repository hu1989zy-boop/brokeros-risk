package com.brokeros.risk.security.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SecurityDomainTests {

    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");
    private static final ActorRef ACTOR_REF =
            new ActorRef("7d90281f-eeb3-4e1f-a70c-d71bd5424ed7");

    @Test
    void actorRefRequiresCanonicalLowercaseUuidV4() {
        assertThat(ActorRef.generate().value()).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

        assertThatThrownBy(() -> new ActorRef(ACTOR_REF.value().toUpperCase()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActorRef("00000000-0000-1000-8000-000000000000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActorRef("not-an-actor"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void externalPrincipalKeyIsExactCaseSensitiveAndBounded() {
        ExternalPrincipalKey lowercase =
                new ExternalPrincipalKey("https://issuer.test", "Alice", ActorType.HUMAN);
        ExternalPrincipalKey differentCase =
                new ExternalPrincipalKey("https://issuer.test", "alice", ActorType.HUMAN);

        assertThat(lowercase).isNotEqualTo(differentCase);
        assertThat(lowercase.subject()).isEqualTo("Alice");
        assertThatThrownBy(() -> new ExternalPrincipalKey(" ", "subject", ActorType.HUMAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalPrincipalKey(
                        "https://issuer.test",
                        "x".repeat(256),
                        ActorType.HUMAN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void capabilityUsesExactLowercaseModuleActionSyntax() {
        assertThat(new Capability("risk-case:review").value()).isEqualTo("risk-case:review");
        assertThatThrownBy(() -> new Capability("Risk-Case:review"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Capability("risk-case:*"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Capability("admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifiedPrincipalSeparatesHumanJwtAndTrustedService() {
        ExternalPrincipalKey humanKey =
                new ExternalPrincipalKey("https://issuer.test", "subject", ActorType.HUMAN);
        ExternalPrincipalKey serviceKey =
                new ExternalPrincipalKey("urn:brokeros:risk:internal-service", "worker", ActorType.SERVICE);

        assertThat(new VerifiedPrincipal(
                        humanKey,
                        AuthenticationMethod.SIGNED_JWT,
                        NOW,
                        NOW.plusSeconds(300)).principalType())
                .isEqualTo(ActorType.HUMAN);
        assertThat(new VerifiedPrincipal(
                        serviceKey,
                        AuthenticationMethod.TRUSTED_IN_PROCESS,
                        NOW,
                        null).principalType())
                .isEqualTo(ActorType.SERVICE);

        assertThatThrownBy(() -> new VerifiedPrincipal(
                        serviceKey,
                        AuthenticationMethod.SIGNED_JWT,
                        NOW,
                        NOW.plusSeconds(300)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerifiedPrincipal(
                        humanKey,
                        AuthenticationMethod.SIGNED_JWT,
                        NOW,
                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void actorContextKeepsCorrelationSeparateAndRejectsTypeMismatch() {
        ExternalPrincipalKey key =
                new ExternalPrincipalKey("https://issuer.test", "subject", ActorType.HUMAN);
        ActorContext context = new ActorContext(
                ACTOR_REF,
                ActorType.HUMAN,
                key,
                AuthenticationMethod.SIGNED_JWT,
                NOW,
                NOW.plusSeconds(300),
                UUID.randomUUID(),
                "request-123",
                "0af7651916cd43dd8448eb211c80319c");

        assertThat(context.actorRef()).isEqualTo(ACTOR_REF);
        assertThat(context.requestId()).isEqualTo("request-123");
        assertThat(context.traceId()).isNotEqualTo(context.actorRef().value());

        assertThatThrownBy(() -> new ActorContext(
                        ACTOR_REF,
                        ActorType.SERVICE,
                        key,
                        AuthenticationMethod.SIGNED_JWT,
                        NOW,
                        NOW.plusSeconds(300),
                        UUID.randomUUID(),
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authorizationDecisionAllowsOnlyExplicitVersionedGrant() {
        Capability capability = new Capability("security-test:read");
        AuthorizationDecision allow =
                AuthorizationDecision.allow(ACTOR_REF, capability, NOW, 3, 7);
        AuthorizationDecision deny = AuthorizationDecision.deny(
                ACTOR_REF,
                capability,
                AuthorizationReason.CAPABILITY_NOT_GRANTED,
                NOW,
                3L,
                null);

        assertThat(allow.isAllowed()).isTrue();
        assertThat(deny.isAllowed()).isFalse();
        assertThatThrownBy(() -> new AuthorizationDecision(
                        AuthorizationOutcome.ALLOW,
                        ACTOR_REF,
                        capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED,
                        NOW,
                        3L,
                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void foundationEnumsContainNoSystemOrWildcardState() {
        assertThat(ActorType.values()).containsExactly(ActorType.HUMAN, ActorType.SERVICE);
        assertThat(java.util.Arrays.stream(ActorType.values()).map(Enum::name))
                .doesNotContain("SYSTEM", "ADMIN", "SUPERUSER");
    }
}
