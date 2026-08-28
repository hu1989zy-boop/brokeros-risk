package com.brokeros.risk.tradingaccount.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.ServiceActorContextFactory;
import com.brokeros.risk.security.application.port.AuthorizationPort;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMutationPort;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityQueryPort;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMetricsPort;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AttestationReference;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import com.brokeros.risk.tradingaccount.domain.ChangeReason;
import com.brokeros.risk.tradingaccount.domain.ChangeReference;
import com.brokeros.risk.tradingaccount.domain.EligibilityDecision;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountIdentity;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.Test;

class TradingAccountApplicationTests {

    private static final Instant NOW = Instant.parse("2026-08-27T10:00:00Z");
    private static final String UUID_TEXT = "00000000-0000-4000-8000-000000000001";

    @Test
    void mutationAuthorizesBeforeCallingPortAndCarriesDecisionEvidence() {
        AtomicInteger sequence = new AtomicInteger();
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(1);
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 3, 5);
        });
        TradingAccountAuthorityMutationPort port = new StubMutationPort() {
            @Override
            public ScopeProvisioningResult registerScope(
                    RegisterScopeSpec spec,
                    AuthorizedMutationContext context) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                assertThat(context.authorizationDecision().actorVersion()).isEqualTo(3);
                return new ScopeProvisioningResult(
                        new AccountAuthorityScopeRef("aas-" + UUID_TEXT),
                        AuthorityOperationOutcome.CREATED, 0, NOW);
            }
        };
        AuthorityScopeProvisioningService service = new AuthorityScopeProvisioningService(
                new AuthorizedMutationFactory(guard, new ManifestFingerprintFactory(),
                        Clock.fixed(NOW, ZoneOffset.UTC)), port, noOpMetrics());

        ScopeProvisioningResult result = service.register(actorContext(), scopeRegistration());

        assertThat(result.outcome()).isEqualTo(AuthorityOperationOutcome.CREATED);
        assertThat(sequence).hasValue(2);
    }

    @Test
    void authorizationDenialProducesZeroAuthorityInteractions() {
        AtomicInteger portInteractions = new AtomicInteger();
        AuthorizationPort denied = (actor, capability) -> AuthorizationDecision.deny(
                actor.actorRef(), capability, AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null);
        TradingAccountAuthorityMutationPort port = new StubMutationPort() {
            @Override
            public ScopeProvisioningResult registerScope(
                    RegisterScopeSpec spec,
                    AuthorizedMutationContext context) {
                portInteractions.incrementAndGet();
                return null;
            }
        };
        AuthorityScopeProvisioningService service = new AuthorityScopeProvisioningService(
                new AuthorizedMutationFactory(new AuthorizationGuard(denied),
                        new ManifestFingerprintFactory(), Clock.fixed(NOW, ZoneOffset.UTC)), port,
                noOpMetrics());

        assertThatThrownBy(() -> service.register(actorContext(), scopeRegistration()))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(portInteractions).hasValue(0);
    }

    @Test
    void q008FacingEligibilityIsBoundedAndAuthorizationPrecedesRead() {
        AtomicInteger sequence = new AtomicInteger();
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(1);
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        TradingAccountRef ref = new TradingAccountRef("ta-" + UUID_TEXT);
        TradingAccountAuthorityQueryPort queryPort = new StubQueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef queried) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.of(new EligibilityPersistenceView(
                        ref, AuthorityLifecycle.INACTIVE, 2,
                        new AuthorityOperationId(UUID_TEXT),
                        AuthorityLifecycle.ACTIVE, 1,
                        new AuthorityOperationId("00000000-0000-4000-8000-000000000002")));
            }
        };
        TradingAccountReferenceEligibilityService service =
                new TradingAccountReferenceEligibilityService(
                        guard, queryPort, new AuthorityEvidenceFactory());

        var result = service.validateForNewRiskCaseAssociation(actorContext(), ref);

        assertThat(result.decision()).isEqualTo(EligibilityDecision.RECOGNIZED_NOT_ELIGIBLE);
        assertThat(result.authoritySnapshotRef().value()).startsWith("tasv1-");
        assertThat(result.authorityProvenanceRef().value()).startsWith("tapv1-");
        assertThat(result.toString()).doesNotContain("INACTIVE", "aas-");
    }

    @Test
    void q008FacadeMapsActiveAndMissingStatesWithoutRawAuthorityData() {
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1));
        TradingAccountRef activeRef = new TradingAccountRef("ta-" + UUID_TEXT);
        TradingAccountRef missingRef = new TradingAccountRef(
                "ta-00000000-0000-4000-8000-000000000004");
        TradingAccountAuthorityQueryPort queryPort = new StubQueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef queried) {
                if (queried.equals(missingRef)) return Optional.empty();
                return Optional.of(new EligibilityPersistenceView(
                        activeRef, AuthorityLifecycle.ACTIVE, 0,
                        new AuthorityOperationId(UUID_TEXT),
                        AuthorityLifecycle.ACTIVE, 0,
                        new AuthorityOperationId("00000000-0000-4000-8000-000000000002")));
            }
        };
        var service = new TradingAccountReferenceEligibilityService(
                guard, queryPort, new AuthorityEvidenceFactory());

        assertThat(service.validateForNewRiskCaseAssociation(actorContext(), activeRef).decision())
                .isEqualTo(EligibilityDecision.ELIGIBLE_FOR_NEW_ASSOCIATION);
        var missing = service.validateForNewRiskCaseAssociation(actorContext(), missingRef);
        assertThat(missing.decision()).isEqualTo(EligibilityDecision.NOT_RECOGNIZED);
        assertThat(missing.authoritySnapshotRef()).isNull();
        assertThat(missing.authorityProvenanceRef()).isNull();
    }

    @Test
    void unauthorizedReadCannotEnumerateAnyStoredState() {
        AtomicInteger queryInteractions = new AtomicInteger();
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.deny(actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));
        TradingAccountAuthorityQueryPort queryPort = new StubQueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                queryInteractions.incrementAndGet();
                return Optional.empty();
            }
        };
        var service = new TradingAccountReferenceEligibilityService(
                guard, queryPort, new AuthorityEvidenceFactory());

        assertThatThrownBy(() -> service.validateForNewRiskCaseAssociation(
                actorContext(), new TradingAccountRef("ta-" + UUID_TEXT)))
                .isInstanceOf(AuthorizationDeniedException.class)
                .hasMessageNotContaining("ta-")
                .hasMessageNotContaining("not found")
                .hasMessageNotContaining("inactive");
        assertThat(queryInteractions).hasValue(0);
    }

    private ActorContext actorContext() {
        return new ActorContext(
                new ActorRef(UUID_TEXT), ActorType.SERVICE,
                new ExternalPrincipalKey(ServiceActorContextFactory.INTERNAL_SERVICE_ISSUER,
                        "trading-account-reference-provisioner", ActorType.SERVICE),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("00000000-0000-4000-8000-000000000003"),
                "request-1", "0123456789abcdef0123456789abcdef");
    }

    private AuthorityOperationRequest scopeRegistration() {
        return new AuthorityOperationRequest(
                1, new AuthorityOperationId(UUID_TEXT),
                AuthorityOperationType.REGISTER_AUTHORITY_SCOPE,
                null, null, null, null, null,
                new AttestationReference("broker-record", "approval-1"),
                new ChangeReason("Initial registration"),
                new ChangeReference("change-1"));
    }

    private TradingAccountAuthorityMetricsPort noOpMetrics() {
        return new TradingAccountAuthorityMetricsPort() {
            @Override public void recordOperation(AuthorityOperationType type, AuthorityOperationOutcome outcome) { }
            @Override public void recordDuration(AuthorityOperationType type, java.time.Duration duration) { }
        };
    }

    private abstract static class StubMutationPort implements TradingAccountAuthorityMutationPort {
        @Override public ScopeProvisioningResult registerScope(RegisterScopeSpec spec, AuthorizedMutationContext context) { throw new UnsupportedOperationException(); }
        @Override public AccountProvisioningResult registerAccount(RegisterAccountSpec spec, AuthorizedMutationContext context) { throw new UnsupportedOperationException(); }
        @Override public LifecycleChangeResult changeScopeLifecycle(ChangeScopeLifecycleSpec spec, AuthorizedMutationContext context) { throw new UnsupportedOperationException(); }
        @Override public LifecycleChangeResult changeAccountLifecycle(ChangeAccountLifecycleSpec spec, AuthorizedMutationContext context) { throw new UnsupportedOperationException(); }
    }

    private abstract static class StubQueryPort implements TradingAccountAuthorityQueryPort {
        @Override public Optional<CompletedAuthorityOperation> findOperation(AuthorityOperationId id) { return Optional.empty(); }
        @Override public Optional<AuthorityScopeState> findScope(AccountAuthorityScopeRef ref) { return Optional.empty(); }
        @Override public Optional<TradingAccountState> findByExternalIdentity(ExternalAccountIdentity identity) { return Optional.empty(); }
        @Override public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) { return Optional.empty(); }
    }
}
