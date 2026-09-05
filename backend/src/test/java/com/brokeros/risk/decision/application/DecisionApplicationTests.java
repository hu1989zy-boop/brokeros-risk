package com.brokeros.risk.decision.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.port.DecisionAccessLogPort;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.application.port.DecisionMutationPort;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.decision.domain.ConclusionText;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.domain.DecisionOperationType;
import com.brokeros.risk.decision.domain.DecisionProvenanceOutcome;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.decision.domain.DecisionSemanticFingerprint;
import com.brokeros.risk.decision.domain.DecisionSource;
import com.brokeros.risk.evidence.application.CompletedEvidenceOperation;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceCapabilities;
import com.brokeros.risk.evidence.application.EvidenceMetricOperation;
import com.brokeros.risk.evidence.application.EvidenceProvenanceQueryService;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.EvidenceSource;
import com.brokeros.risk.evidence.domain.EvidenceStatus;
import com.brokeros.risk.evidence.domain.ObservationText;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.SecurityDependencyUnavailableException;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.application.AuthorityEvidenceFactory;
import com.brokeros.risk.tradingaccount.application.AuthorityScopeState;
import com.brokeros.risk.tradingaccount.application.CompletedAuthorityOperation;
import com.brokeros.risk.tradingaccount.application.EligibilityPersistenceView;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountCapabilities;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.application.TradingAccountState;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityQueryPort;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountIdentity;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.Test;

class DecisionApplicationTests {

    private static final Instant NOW = Instant.parse("2026-08-31T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";
    private static final String UUID_THREE = "00000000-0000-4000-8000-000000000003";
    private static final String UUID_FOUR = "00000000-0000-4000-8000-000000000004";
    private static final String SUBJECT_REF = "ta-" + UUID_ONE;
    private static final String EVIDENCE_REF = "ev-" + UUID_TWO;
    private static final String REPLACEMENT_REF = "ev-" + UUID_THREE;
    private static final String DECISION_REF = "dec-" + UUID_FOUR;

    @Test
    void recordingUsesCanonicalOrderOwnActorDeduplicatesAndAcceptsRecognizedNotEligibleAndSuperseded() {
        AtomicInteger sequence = new AtomicInteger();
        ActorContext actor = actor(ActorType.HUMAN);
        AuthorizationGuard guard = new AuthorizationGuard((context, capability) -> {
            assertThat(context).isSameAs(actor);
            int current = sequence.incrementAndGet();
            if (capability.equals(DecisionCapabilities.RECORD)) {
                assertThat(current).isEqualTo(1);
            } else if (capability.equals(TradingAccountCapabilities.READ)) {
                assertThat(current).isEqualTo(3);
            } else if (capability.equals(EvidenceCapabilities.READ)) {
                assertThat(current).isEqualTo(5);
            } else {
                throw new AssertionError("unexpected capability " + capability);
            }
            return AuthorizationDecision.allow(context.actorRef(), capability, NOW, 1, 1);
        });
        DecisionQueryPort query = new StubDecisionQueryPort() {
            @Override
            public Optional<CompletedDecisionOperation> findOperation(DecisionOperationId id) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.empty();
            }
        };
        TradingAccountAuthorityQueryPort q010 = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(4);
                return Optional.of(eligibilityView(ref, AuthorityLifecycle.INACTIVE));
            }
        };
        EvidenceQueryPort q011 = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(6);
                return Optional.of(evidence(EvidenceStatus.SUPERSEDED));
            }
        };
        DecisionMutationPort mutation = (spec, context) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(7);
            assertThat(context.actorContext()).isSameAs(actor);
            assertThat(spec.evidenceRefs()).extracting(EvidenceRef::value)
                    .containsExactly(EVIDENCE_REF);
            return completed(spec, context);
        };
        DecisionRecordingService service = service(
                guard, query, mutation,
                new TradingAccountReferenceEligibilityService(
                        guard, q010, new AuthorityEvidenceFactory()),
                new EvidenceProvenanceQueryService(guard, q011, noOpEvidenceMetrics()));

        CompletedDecisionOperation result = service.record(
                actor,
                new RecordDecisionCommand(
                        UUID_ONE, SUBJECT_REF,
                        List.of(EVIDENCE_REF, EVIDENCE_REF), "conclusion"));

        assertThat(result.decisionRef().value()).isEqualTo(DECISION_REF);
        assertThat(sequence).hasValue(7);
    }

    @Test
    void emptyEvidenceIsRejectedBeforeEitherExternalAuthorityCall() {
        AtomicInteger q010Interactions = new AtomicInteger();
        AtomicInteger q011Interactions = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();
        TradingAccountAuthorityQueryPort q010 = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                q010Interactions.incrementAndGet();
                return Optional.empty();
            }
        };
        EvidenceQueryPort q011 = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                q011Interactions.incrementAndGet();
                return Optional.empty();
            }
        };
        DecisionRecordingService service = service(
                allowAll(), new StubDecisionQueryPort(), (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                new TradingAccountReferenceEligibilityService(
                        allowAll(), q010, new AuthorityEvidenceFactory()),
                new EvidenceProvenanceQueryService(
                        allowAll(), q011, noOpEvidenceMetrics()));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                new RecordDecisionCommand(UUID_ONE, SUBJECT_REF, List.of(), "conclusion")))
                .isInstanceOf(DecisionException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_CONTENT_INVALID));
        assertThat(q010Interactions).hasValue(0);
        assertThat(q011Interactions).hasValue(0);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void serviceActorIsRejectedBeforeReplayCheckAndExternalCalls() {
        AtomicInteger decisionQueries = new AtomicInteger();
        AtomicInteger q010Interactions = new AtomicInteger();
        AtomicInteger q011Interactions = new AtomicInteger();
        DecisionQueryPort query = new StubDecisionQueryPort() {
            @Override
            public Optional<CompletedDecisionOperation> findOperation(DecisionOperationId id) {
                decisionQueries.incrementAndGet();
                return Optional.empty();
            }
        };
        DecisionRecordingService service = service(
                allowAll(), query, new StubDecisionMutationPort(),
                eligibilityWithInteractions(q010Interactions, AuthorityLifecycle.ACTIVE),
                evidenceServiceWithInteractions(q011Interactions, EvidenceStatus.ACTIVE));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.SERVICE), command(UUID_ONE)))
                .isInstanceOf(DecisionException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_ACTOR_TYPE_NOT_PERMITTED));
        assertThat(decisionQueries).hasValue(0);
        assertThat(q010Interactions).hasValue(0);
        assertThat(q011Interactions).hasValue(0);
    }

    @Test
    void exactReplaySkipsInvalidContentQ010AndQ011WhileChangedReplayConflicts() {
        DecisionFingerprintFactory fingerprints = new DecisionFingerprintFactory();
        DecisionSemanticFingerprint fingerprint = fingerprints.forRecord(
                "not-a-subject", List.of("not-evidence"), "");
        CompletedDecisionOperation completed = completed(
                new DecisionOperationId(UUID_ONE), fingerprint, record());
        AtomicInteger q010Interactions = new AtomicInteger();
        AtomicInteger q011Interactions = new AtomicInteger();
        DecisionQueryPort query = new StubDecisionQueryPort() {
            @Override
            public Optional<CompletedDecisionOperation> findOperation(DecisionOperationId id) {
                return Optional.of(completed);
            }
        };
        DecisionRecordingService service = service(
                allowAll(), query, new StubDecisionMutationPort(),
                eligibilityWithInteractions(q010Interactions, AuthorityLifecycle.ACTIVE),
                evidenceServiceWithInteractions(q011Interactions, EvidenceStatus.ACTIVE));

        CompletedDecisionOperation replay = service.record(
                actor(ActorType.HUMAN),
                new RecordDecisionCommand(
                        UUID_ONE, "not-a-subject", List.of("not-evidence"), ""));
        assertThat(replay).isSameAs(completed);
        assertThat(q010Interactions).hasValue(0);
        assertThat(q011Interactions).hasValue(0);

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                new RecordDecisionCommand(
                        UUID_ONE, "not-a-subject", List.of("not-evidence"), "changed")))
                .isInstanceOf(DecisionConflictException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_IDEMPOTENCY_CONFLICT));
        assertThat(q010Interactions).hasValue(0);
        assertThat(q011Interactions).hasValue(0);
    }

    @Test
    void subjectRejectsOnlyNotRecognizedAndMapsAuthorityUnavailability() {
        AtomicInteger mutations = new AtomicInteger();
        DecisionMutationPort mutation = (spec, context) -> {
            mutations.incrementAndGet();
            return completed(spec, context);
        };
        DecisionRecordingService missing = service(
                allowAll(), new StubDecisionQueryPort(), mutation,
                new TradingAccountReferenceEligibilityService(
                        allowAll(), new StubQ010QueryPort(), new AuthorityEvidenceFactory()),
                evidenceServiceWithInteractions(
                        new AtomicInteger(), EvidenceStatus.ACTIVE));

        assertThatThrownBy(() -> missing.record(actor(ActorType.HUMAN), command(UUID_ONE)))
                .isInstanceOf(DecisionException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_SUBJECT_NOT_RECOGNIZED));

        TradingAccountAuthorityQueryPort unavailable = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                throw new TradingAccountAuthorityUnavailableException();
            }
        };
        DecisionRecordingService unavailableService = service(
                allowAll(), new StubDecisionQueryPort(), mutation,
                new TradingAccountReferenceEligibilityService(
                        allowAll(), unavailable, new AuthorityEvidenceFactory()),
                evidenceServiceWithInteractions(
                        new AtomicInteger(), EvidenceStatus.ACTIVE));
        assertThatThrownBy(() -> unavailableService.record(
                actor(ActorType.HUMAN), command(UUID_TWO)))
                .isInstanceOf(DecisionException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_SUBJECT_AUTHORITY_UNAVAILABLE));
        assertThat(mutations).hasValue(0);
    }

    @Test
    void evidenceRejectsOnlyNotFoundAndMapsAuthorityUnavailability() {
        AtomicInteger mutations = new AtomicInteger();
        DecisionMutationPort mutation = (spec, context) -> {
            mutations.incrementAndGet();
            return completed(spec, context);
        };
        EvidenceProvenanceQueryService missing = new EvidenceProvenanceQueryService(
                allowAll(), new StubEvidenceQueryPort(), noOpEvidenceMetrics());
        DecisionRecordingService missingService = service(
                allowAll(), new StubDecisionQueryPort(), mutation,
                eligibilityWithInteractions(new AtomicInteger(), AuthorityLifecycle.ACTIVE),
                missing);
        assertThatThrownBy(() -> missingService.record(
                actor(ActorType.HUMAN), command(UUID_ONE)))
                .isInstanceOf(DecisionException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_EVIDENCE_NOT_RECOGNIZED));

        EvidenceQueryPort unavailable = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                throw new EvidenceAuthorityUnavailableException();
            }
        };
        DecisionRecordingService unavailableService = service(
                allowAll(), new StubDecisionQueryPort(), mutation,
                eligibilityWithInteractions(new AtomicInteger(), AuthorityLifecycle.ACTIVE),
                new EvidenceProvenanceQueryService(
                        allowAll(), unavailable, noOpEvidenceMetrics()));
        assertThatThrownBy(() -> unavailableService.record(
                actor(ActorType.HUMAN), command(UUID_TWO)))
                .isInstanceOf(DecisionException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_EVIDENCE_AUTHORITY_UNAVAILABLE));
        assertThat(mutations).hasValue(0);
    }

    @Test
    void crossModuleCapabilityDenialsPreventDecisionMutation() {
        AtomicInteger mutations = new AtomicInteger();
        DecisionMutationPort mutation = (spec, context) -> {
            mutations.incrementAndGet();
            return completed(spec, context);
        };
        AuthorizationGuard q010Denied = new AuthorizationGuard((actor, capability) -> {
            if (capability.equals(TradingAccountCapabilities.READ)) {
                return AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null);
            }
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        DecisionRecordingService q010DeniedService = service(
                q010Denied, new StubDecisionQueryPort(), mutation,
                eligibility(q010Denied, AuthorityLifecycle.ACTIVE),
                evidenceServiceWithInteractions(new AtomicInteger(), EvidenceStatus.ACTIVE));
        assertThatThrownBy(() -> q010DeniedService.record(
                actor(ActorType.HUMAN), command(UUID_ONE)))
                .isInstanceOf(AuthorizationDeniedException.class);

        AuthorizationGuard q011Denied = new AuthorizationGuard((actor, capability) -> {
            if (capability.equals(EvidenceCapabilities.READ)) {
                return AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null);
            }
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        DecisionRecordingService q011DeniedService = service(
                q011Denied, new StubDecisionQueryPort(), mutation,
                eligibility(q011Denied, AuthorityLifecycle.ACTIVE),
                new EvidenceProvenanceQueryService(
                        q011Denied,
                        new StubEvidenceQueryPort() {
                            @Override
                            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                                return Optional.of(evidence(EvidenceStatus.ACTIVE));
                            }
                        }, noOpEvidenceMetrics()));
        assertThatThrownBy(() -> q011DeniedService.record(
                actor(ActorType.HUMAN), command(UUID_TWO)))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void readsAuthorizeBeforeLookupPermitServiceAndAuditBeforeDisclosure() {
        AtomicInteger sequence = new AtomicInteger();
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(1);
            assertThat(capability).isEqualTo(DecisionCapabilities.READ);
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        DecisionRecord record = record();
        DecisionQueryPort query = new StubDecisionQueryPort() {
            @Override
            public Optional<DecisionRecord> findByRef(DecisionRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.of(record);
            }
        };
        DecisionAccessLogPort accessLog = (ref, accessor, occurredAt) ->
                assertThat(sequence.incrementAndGet()).isEqualTo(3);
        DecisionDetailReadService detail = new DecisionDetailReadService(
                guard, query, accessLog, noOpDecisionMetrics(), CLOCK);

        assertThat(detail.readDetail(actor(ActorType.SERVICE), DECISION_REF)).isSameAs(record);
        assertThat(sequence).hasValue(3);

        DecisionProvenanceQueryService provenance = new DecisionProvenanceQueryService(
                allowAll(), queryReturning(record), noOpDecisionMetrics());
        assertThat(provenance.confirmProvenance(
                actor(ActorType.SERVICE), new DecisionRef(DECISION_REF)).outcome())
                .isEqualTo(DecisionProvenanceOutcome.RECOGNIZED);
    }

    @Test
    void deniedOrUnavailableReadNeverQueriesAndAuditFailureReturnsNoContent() {
        AtomicInteger queries = new AtomicInteger();
        DecisionQueryPort query = new StubDecisionQueryPort() {
            @Override
            public Optional<DecisionRecord> findByRef(DecisionRef ref) {
                queries.incrementAndGet();
                return Optional.of(record());
            }
        };
        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));
        assertThatThrownBy(() -> new DecisionDetailReadService(
                denied, query, (ref, actor, time) -> { }, noOpDecisionMetrics(), CLOCK)
                .readDetail(actor(ActorType.SERVICE), DECISION_REF))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> new DecisionProvenanceQueryService(
                denied, query, noOpDecisionMetrics()).confirmProvenance(
                        actor(ActorType.SERVICE), new DecisionRef(DECISION_REF)))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(queries).hasValue(0);

        AuthorizationGuard unavailable = new AuthorizationGuard((actor, capability) -> {
            throw new SecurityDependencyUnavailableException(new IllegalStateException("down"));
        });
        assertThatThrownBy(() -> new DecisionDetailReadService(
                unavailable, query, (ref, actor, time) -> { }, noOpDecisionMetrics(), CLOCK)
                .readDetail(actor(ActorType.SERVICE), DECISION_REF))
                .isInstanceOf(SecurityDependencyUnavailableException.class);
        assertThat(queries).hasValue(0);

        DecisionDetailReadService failedAudit = new DecisionDetailReadService(
                allowAll(), query,
                (ref, actor, time) -> {
                    throw new DecisionAuthorityUnavailableException();
                }, noOpDecisionMetrics(), CLOCK);
        assertThatThrownBy(() -> failedAudit.readDetail(
                actor(ActorType.SERVICE), DECISION_REF))
                .isInstanceOf(DecisionAuthorityUnavailableException.class);
        assertThat(queries).hasValue(1);
    }

    private DecisionRecordingService service(
            AuthorizationGuard guard,
            DecisionQueryPort query,
            DecisionMutationPort mutation,
            TradingAccountReferenceEligibilityService eligibility,
            EvidenceProvenanceQueryService evidenceService) {
        return new DecisionRecordingService(
                guard, query, mutation, new DecisionFingerprintFactory(),
                eligibility, evidenceService,
                new AuthorizedMutationFactory(CLOCK), noOpDecisionMetrics());
    }

    private TradingAccountReferenceEligibilityService eligibilityWithInteractions(
            AtomicInteger interactions,
            AuthorityLifecycle lifecycle) {
        TradingAccountAuthorityQueryPort q010 = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                interactions.incrementAndGet();
                return Optional.of(eligibilityView(ref, lifecycle));
            }
        };
        return new TradingAccountReferenceEligibilityService(
                allowAll(), q010, new AuthorityEvidenceFactory());
    }

    private TradingAccountReferenceEligibilityService eligibility(
            AuthorizationGuard guard,
            AuthorityLifecycle lifecycle) {
        TradingAccountAuthorityQueryPort q010 = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                return Optional.of(eligibilityView(ref, lifecycle));
            }
        };
        return new TradingAccountReferenceEligibilityService(
                guard, q010, new AuthorityEvidenceFactory());
    }

    private EvidenceProvenanceQueryService evidenceServiceWithInteractions(
            AtomicInteger interactions,
            EvidenceStatus status) {
        EvidenceQueryPort q011 = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                interactions.incrementAndGet();
                return Optional.of(evidence(status));
            }
        };
        return new EvidenceProvenanceQueryService(
                allowAll(), q011, noOpEvidenceMetrics());
    }

    private EligibilityPersistenceView eligibilityView(
            TradingAccountRef ref,
            AuthorityLifecycle lifecycle) {
        return new EligibilityPersistenceView(
                ref, lifecycle, 1, new AuthorityOperationId(UUID_ONE),
                AuthorityLifecycle.ACTIVE, 1, new AuthorityOperationId(UUID_TWO));
    }

    private EvidenceRecord evidence(EvidenceStatus status) {
        return new EvidenceRecord(
                new EvidenceRef(EVIDENCE_REF),
                new TradingAccountRef("ta-" + UUID_THREE),
                EvidenceSource.MANUAL,
                new ObservationText("observation"),
                status,
                new ActorRef(UUID_ONE),
                NOW,
                null,
                status == EvidenceStatus.SUPERSEDED
                        ? new EvidenceRef(REPLACEMENT_REF) : null);
    }

    private CompletedDecisionOperation completed(
            RecordDecisionSpec spec,
            AuthorizedMutationContext context) {
        DecisionRecord record = new DecisionRecord(
                new DecisionRef(DECISION_REF), spec.subjectRef(), spec.evidenceRefs(),
                spec.conclusionText(), DecisionSource.MANUAL,
                context.actorContext().actorRef(), context.occurredAt());
        return completed(spec.operationId(), context.fingerprint(), record);
    }

    private CompletedDecisionOperation completed(
            DecisionOperationId operationId,
            DecisionSemanticFingerprint fingerprint,
            DecisionRecord record) {
        return new CompletedDecisionOperation(
                operationId, DecisionOperationType.RECORD, fingerprint,
                record.decisionRef(), DecisionOperationOutcome.CREATED,
                record.recordedAt(), record);
    }

    private DecisionRecord record() {
        return new DecisionRecord(
                new DecisionRef(DECISION_REF), new TradingAccountRef(SUBJECT_REF),
                Set.of(new EvidenceRef(EVIDENCE_REF)), new ConclusionText("conclusion"),
                DecisionSource.MANUAL, new ActorRef(UUID_ONE), NOW);
    }

    private RecordDecisionCommand command(String operationId) {
        return new RecordDecisionCommand(
                operationId, SUBJECT_REF, List.of(EVIDENCE_REF), "conclusion");
    }

    private DecisionQueryPort queryReturning(DecisionRecord record) {
        return new StubDecisionQueryPort() {
            @Override
            public Optional<DecisionRecord> findByRef(DecisionRef ref) {
                return Optional.of(record);
            }
        };
    }

    private AuthorizationGuard allowAll() {
        return new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1));
    }

    private ActorContext actor(ActorType type) {
        return new ActorContext(
                new ActorRef(UUID_ONE), type,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test",
                        "principal-" + type.name().toLowerCase(), type),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString(UUID_FOUR), "request-1",
                "0123456789abcdef0123456789abcdef");
    }

    private DecisionMetricsPort noOpDecisionMetrics() {
        return new DecisionMetricsPort() {
            @Override
            public void recordOperation(
                    DecisionMetricOperation operation,
                    DecisionOperationOutcome outcome) {
            }

            @Override
            public void recordConflict(ResultCode category) {
            }

            @Override
            public void recordAuthorizationDenied(Capability capability) {
            }

            @Override
            public void recordAccessRead(String outcome) {
            }

            @Override
            public void recordDuration(
                    DecisionMetricOperation operation,
                    Duration duration) {
            }
        };
    }

    private EvidenceMetricsPort noOpEvidenceMetrics() {
        return new EvidenceMetricsPort() {
            @Override
            public void recordOperation(
                    EvidenceMetricOperation operation,
                    EvidenceOperationOutcome outcome) {
            }

            @Override
            public void recordConflict(ResultCode category) {
            }

            @Override
            public void recordAuthorizationDenied(Capability capability) {
            }

            @Override
            public void recordAccessRead(String outcome) {
            }

            @Override
            public void recordDuration(
                    EvidenceMetricOperation operation,
                    Duration duration) {
            }
        };
    }

    private static class StubDecisionQueryPort implements DecisionQueryPort {
        @Override
        public Optional<CompletedDecisionOperation> findOperation(DecisionOperationId id) {
            return Optional.empty();
        }

        @Override
        public Optional<DecisionRecord> findByRef(DecisionRef ref) {
            return Optional.empty();
        }

        @Override
        public List<DecisionReferenceSummary> findSummariesBySubject(
                TradingAccountRef subjectRef,
                int limit) {
            return List.of();
        }
    }

    private static class StubDecisionMutationPort implements DecisionMutationPort {
        @Override
        public CompletedDecisionOperation record(
                RecordDecisionSpec spec,
                AuthorizedMutationContext context) {
            throw new UnsupportedOperationException();
        }
    }

    private static class StubEvidenceQueryPort implements EvidenceQueryPort {
        @Override
        public Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id) {
            return Optional.empty();
        }

        @Override
        public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
            return Optional.empty();
        }

        @Override
        public List<com.brokeros.risk.evidence.application.EvidenceReferenceSummary>
                findSummariesBySubject(TradingAccountRef subjectRef, int limit) {
            return List.of();
        }
    }

    private static class StubQ010QueryPort implements TradingAccountAuthorityQueryPort {
        @Override
        public Optional<CompletedAuthorityOperation> findOperation(AuthorityOperationId id) {
            return Optional.empty();
        }

        @Override
        public Optional<AuthorityScopeState> findScope(AccountAuthorityScopeRef ref) {
            return Optional.empty();
        }

        @Override
        public Optional<TradingAccountState> findByExternalIdentity(
                ExternalAccountIdentity identity) {
            return Optional.empty();
        }

        @Override
        public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
            return Optional.empty();
        }
    }
}
