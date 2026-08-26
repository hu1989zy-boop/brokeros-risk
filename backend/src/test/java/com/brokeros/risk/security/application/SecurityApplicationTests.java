package com.brokeros.risk.security.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.brokeros.risk.security.application.port.ActorMappingPort;
import com.brokeros.risk.security.application.port.AuthorizationPort;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.MappedActor;
import com.brokeros.risk.security.domain.ProvisioningMetadata;
import com.brokeros.risk.security.domain.VerifiedPrincipal;
import org.junit.jupiter.api.Test;

class SecurityApplicationTests {

    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ActorRef ACTOR_REF =
            new ActorRef("7d90281f-eeb3-4e1f-a70c-d71bd5424ed7");
    private static final Capability CAPABILITY = new Capability("security-test:read");

    @Test
    void mappingCreatesFreshServerContextFromVerifiedPrincipal() {
        ActorMappingService service = new ActorMappingService(
                principal -> new MappedActor(ACTOR_REF, ActorType.HUMAN, 1));
        VerifiedPrincipal principal = humanPrincipal("subject");

        ActorContext first = service.createContext(principal, "request-1", null);
        ActorContext second = service.createContext(principal, "request-2", null);

        assertThat(first.actorRef()).isEqualTo(ACTOR_REF);
        assertThat(first.externalPrincipalKey()).isEqualTo(principal.externalPrincipalKey());
        assertThat(first.executionId()).isNotEqualTo(second.executionId());
    }

    @Test
    void mappingRejectsActorAndPrincipalTypeMismatch() {
        ActorMappingService service = new ActorMappingService(
                principal -> new MappedActor(ACTOR_REF, ActorType.SERVICE, 1));

        assertThatThrownBy(() -> service.createContext(humanPrincipal("subject"), null, null))
                .isInstanceOf(ActorAccessDeniedException.class);
    }

    @Test
    void guardAllowsOnlyExplicitAllowAndPreservesUnavailableFailure() {
        ActorContext context = humanContext();
        AuthorizationPort allowPort = (actorContext, capability) ->
                AuthorizationDecision.allow(ACTOR_REF, capability, NOW, 1, 2);
        AuthorizationPort denyPort = (actorContext, capability) ->
                AuthorizationDecision.deny(
                        ACTOR_REF,
                        capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED,
                        NOW,
                        1L,
                        null);
        AuthorizationPort unavailablePort = (actorContext, capability) -> {
            throw new SecurityDependencyUnavailableException(new IllegalStateException());
        };

        assertThat(new AuthorizationGuard(allowPort).requireAllowed(context, CAPABILITY).isAllowed())
                .isTrue();
        assertThatThrownBy(() -> new AuthorizationGuard(denyPort)
                        .requireAllowed(context, CAPABILITY))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> new AuthorizationGuard(unavailablePort)
                        .requireAllowed(context, CAPABILITY))
                .isInstanceOf(SecurityDependencyUnavailableException.class);
    }

    @Test
    void serviceFactoryRequiresRegisteredDescriptorInstanceAndDatabaseMapping() {
        TestServiceDescriptor registered = new TestServiceDescriptor("settlement-worker");
        TestServiceDescriptor equalButUnregistered = new TestServiceDescriptor("settlement-worker");
        AtomicReference<VerifiedPrincipal> observed = new AtomicReference<>();
        ActorMappingPort mappingPort = principal -> {
            observed.set(principal);
            return new MappedActor(ACTOR_REF, ActorType.SERVICE, 0);
        };
        ServiceActorContextFactory factory = new ServiceActorContextFactory(
                new ActorMappingService(mappingPort),
                Set.of(registered),
                CLOCK);

        ActorContext first = factory.create(registered);
        ActorContext second = factory.create(registered);

        assertThat(first.actorType()).isEqualTo(ActorType.SERVICE);
        assertThat(first.executionId()).isNotEqualTo(second.executionId());
        assertThat(observed.get().authenticationMethod())
                .isEqualTo(AuthenticationMethod.TRUSTED_IN_PROCESS);
        assertThatThrownBy(() -> factory.create(equalButUnregistered))
                .isInstanceOf(ActorAccessDeniedException.class);
    }

    @Test
    void serviceFactoryRejectsGenericSystemDescriptor() {
        TestServiceDescriptor system = new TestServiceDescriptor("system");
        ServiceActorContextFactory factory = new ServiceActorContextFactory(
                new ActorMappingService(principal ->
                        new MappedActor(ACTOR_REF, ActorType.SERVICE, 0)),
                Set.of(system),
                CLOCK);

        assertThatThrownBy(() -> factory.create(system))
                .isInstanceOf(ActorAccessDeniedException.class);
    }

    @Test
    void provisioningManifestRejectsDuplicateOwnershipAndTypeMismatch() {
        ExternalPrincipalKey key =
                new ExternalPrincipalKey("https://issuer.test", "subject", ActorType.HUMAN);
        ActorProvisioningSpec first =
                new ActorProvisioningSpec(ActorType.HUMAN, List.of(key), Set.of());
        ActorProvisioningSpec duplicate =
                new ActorProvisioningSpec(ActorType.HUMAN, List.of(key), Set.of());

        assertThatThrownBy(() -> new ProvisioningManifest(
                        new ProvisioningMetadata("deployment", "manifest-v1"),
                        List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActorProvisioningSpec(
                        ActorType.SERVICE,
                        List.of(key),
                        Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static VerifiedPrincipal humanPrincipal(String subject) {
        return new VerifiedPrincipal(
                new ExternalPrincipalKey("https://issuer.test", subject, ActorType.HUMAN),
                AuthenticationMethod.SIGNED_JWT,
                NOW,
                NOW.plusSeconds(300));
    }

    private static ActorContext humanContext() {
        return new ActorMappingService(
                principal -> new MappedActor(ACTOR_REF, ActorType.HUMAN, 1))
                .createContext(humanPrincipal("subject"), null, null);
    }

    private record TestServiceDescriptor(String serviceCode)
            implements RegisteredServiceDescriptor {
    }
}
