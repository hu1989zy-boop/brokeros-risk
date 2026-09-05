package com.brokeros.risk.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.port.EvidenceAccessLogPort;
import com.brokeros.risk.evidence.application.port.EvidenceMutationPort;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.evidence.domain.EvidenceOperationType;
import com.brokeros.risk.evidence.domain.EvidenceProvenanceOutcome;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.EvidenceSource;
import com.brokeros.risk.evidence.domain.EvidenceStatus;
import com.brokeros.risk.evidence.domain.ObservationText;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.application.AuthorityEvidenceFactory;
import com.brokeros.risk.tradingaccount.application.AuthorityScopeState;
import com.brokeros.risk.tradingaccount.application.CompletedAuthorityOperation;
import com.brokeros.risk.tradingaccount.application.EligibilityPersistenceView;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.application.TradingAccountState;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityQueryPort;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountIdentity;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.Test;

class EvidenceApplicationTests {

    private static final Instant NOW = Instant.parse("2026-08-29T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";
    private static final String UUID_THREE = "00000000-0000-4000-8000-000000000003";
    private static final String SUBJECT_REF = "ta-" + UUID_ONE;
    private static final String EVIDENCE_REF = "ev-" + UUID_TWO;
    private static final String REPLACEMENT_REF = "ev-" + UUID_THREE;

    @Test
    void recordingUsesExactOrderOwnActorAndAcceptsRecognizedNotEligible() {
        AtomicInteger sequence = new AtomicInteger();
        ActorContext actor = actor(ActorType.HUMAN);
        AuthorizationGuard evidenceGuard = new AuthorizationGuard((context, capability) -> {
            assertThat(context).isSameAs(actor);
            assertThat(capability).isEqualTo(EvidenceCapabilities.RECORD);
            assertThat(sequence.incrementAndGet()).isEqualTo(1);
            return AuthorizationDecision.allow(context.actorRef(), capability, NOW, 1, 1);
        });
        EvidenceQueryPort evidenceQuery = new StubEvidenceQueryPort() {
            @Override
            public Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.empty();
            }
        };
        TradingAccountReferenceEligibilityService eligibility = eligibilityService(
                sequence, actor, AuthorityLifecycle.INACTIVE);
        EvidenceMutationPort mutation = new StubEvidenceMutationPort() {
            @Override
            public EvidenceRecordingResult record(
                    RecordEvidenceSpec spec,
                    AuthorizedMutationContext context) {
                assertThat(sequence.incrementAndGet()).isEqualTo(5);
                assertThat(context.actorContext()).isSameAs(actor);
                assertThat(spec.subjectRef().value()).isEqualTo(SUBJECT_REF);
                return new EvidenceRecordingResult(
                        new EvidenceRef(EVIDENCE_REF),
                        EvidenceOperationOutcome.CREATED, NOW);
            }
        };
        EvidenceRecordingService service = new EvidenceRecordingService(
                evidenceGuard, evidenceQuery, mutation, new EvidenceFingerprintFactory(),
                eligibility, noOpMetrics(), CLOCK);

        EvidenceRecordingResult result = service.record(
                actor, new RecordEvidenceCommand(UUID_ONE, SUBJECT_REF, "observation"));

        assertThat(result.evidenceRef().value()).isEqualTo(EVIDENCE_REF);
        assertThat(sequence).hasValue(5);
    }

    @Test
    void recordingRejectsOnlyNotRecognizedAndMapsQ010Unavailability() {
        AtomicInteger mutations = new AtomicInteger();
        EvidenceMutationPort mutation = new StubEvidenceMutationPort() {
            @Override
            public EvidenceRecordingResult record(
                    RecordEvidenceSpec spec,
                    AuthorizedMutationContext context) {
                mutations.incrementAndGet();
                return null;
            }
        };
        EvidenceRecordingService missing = recordingService(
                emptyEvidenceQuery(), mutation,
                new TradingAccountReferenceEligibilityService(
                        allowAll(), new StubQ010QueryPort(), new AuthorityEvidenceFactory()));

        assertThatThrownBy(() -> missing.record(
                actor(ActorType.HUMAN),
                new RecordEvidenceCommand(UUID_ONE, SUBJECT_REF, "observation")))
                .isInstanceOf(EvidenceException.class)
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_SUBJECT_NOT_RECOGNIZED));
        assertThat(mutations).hasValue(0);

        TradingAccountAuthorityQueryPort unavailablePort = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                throw new TradingAccountAuthorityUnavailableException();
            }
        };
        EvidenceRecordingService unavailable = recordingService(
                emptyEvidenceQuery(), mutation,
                new TradingAccountReferenceEligibilityService(
                        allowAll(), unavailablePort, new AuthorityEvidenceFactory()));
        assertThatThrownBy(() -> unavailable.record(
                actor(ActorType.HUMAN),
                new RecordEvidenceCommand(UUID_ONE, SUBJECT_REF, "observation")))
                .isInstanceOf(EvidenceException.class)
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_SUBJECT_AUTHORITY_UNAVAILABLE));
        assertThat(mutations).hasValue(0);
    }

    @Test
    void serviceActorCannotRecordOrRetrieveMutationReplay() {
        AtomicInteger evidenceQueries = new AtomicInteger();
        AtomicInteger q010Interactions = new AtomicInteger();
        EvidenceQueryPort query = new StubEvidenceQueryPort() {
            @Override
            public Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id) {
                evidenceQueries.incrementAndGet();
                return Optional.empty();
            }
        };
        TradingAccountAuthorityQueryPort q010 = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                q010Interactions.incrementAndGet();
                return Optional.empty();
            }
        };
        EvidenceRecordingService service = recordingService(
                query, new StubEvidenceMutationPort(),
                new TradingAccountReferenceEligibilityService(
                        allowAll(), q010, new AuthorityEvidenceFactory()));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.SERVICE),
                new RecordEvidenceCommand(UUID_ONE, SUBJECT_REF, "observation")))
                .isInstanceOf(EvidenceException.class)
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_ACTOR_TYPE_NOT_PERMITTED));
        assertThat(evidenceQueries).hasValue(0);
        assertThat(q010Interactions).hasValue(0);
    }

    @Test
    void exactRecordReplaySkipsInvalidContentAndQ010ButChangedReplayConflicts() {
        EvidenceFingerprintFactory fingerprints = new EvidenceFingerprintFactory();
        CompletedEvidenceOperation completed = new CompletedEvidenceOperation(
                EvidenceOperationType.RECORD,
                fingerprints.forRecord("not-a-subject", ""),
                new EvidenceRef(EVIDENCE_REF),
                EvidenceOperationOutcome.CREATED, NOW);
        EvidenceQueryPort query = operationQuery(completed);
        AtomicInteger q010Interactions = new AtomicInteger();
        TradingAccountAuthorityQueryPort q010 = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                q010Interactions.incrementAndGet();
                return Optional.empty();
            }
        };
        EvidenceRecordingService service = recordingService(
                query, new StubEvidenceMutationPort(),
                new TradingAccountReferenceEligibilityService(
                        allowAll(), q010, new AuthorityEvidenceFactory()));

        EvidenceRecordingResult replay = service.record(
                actor(ActorType.HUMAN),
                new RecordEvidenceCommand(UUID_ONE, "not-a-subject", ""));
        assertThat(replay.evidenceRef().value()).isEqualTo(EVIDENCE_REF);
        assertThat(q010Interactions).hasValue(0);

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                new RecordEvidenceCommand(UUID_ONE, "not-a-subject", "changed")))
                .isInstanceOf(EvidenceConflictException.class)
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT));
    }

    @Test
    void correctionReplayPrecedesTargetStatusAndNewCorrectionCopiesTargetSubject() {
        EvidenceFingerprintFactory fingerprints = new EvidenceFingerprintFactory();
        CompletedEvidenceOperation completed = new CompletedEvidenceOperation(
                EvidenceOperationType.CORRECT,
                fingerprints.forCorrection(EVIDENCE_REF, "reason", "replacement"),
                new EvidenceRef(REPLACEMENT_REF),
                EvidenceOperationOutcome.CORRECTED, NOW);
        EvidenceCorrectionService replayService = correctionService(
                operationQuery(completed), new StubEvidenceMutationPort(), fingerprints);

        EvidenceCorrectionResult replay = replayService.correct(
                actor(ActorType.HUMAN),
                new CorrectEvidenceCommand(
                        UUID_ONE, EVIDENCE_REF, "reason", "replacement"));
        assertThat(replay.evidenceRef().value()).isEqualTo(REPLACEMENT_REF);

        AtomicInteger targetLoads = new AtomicInteger();
        EvidenceQueryPort newQuery = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                targetLoads.incrementAndGet();
                return Optional.of(activeRecord(EVIDENCE_REF));
            }
        };
        EvidenceMutationPort mutation = new StubEvidenceMutationPort() {
            @Override
            public EvidenceCorrectionResult correct(
                    CorrectEvidenceSpec spec,
                    AuthorizedMutationContext context) {
                assertThat(spec.targetEvidenceRef().value()).isEqualTo(EVIDENCE_REF);
                assertThat(CorrectEvidenceSpec.class.getRecordComponents())
                        .extracting(component -> component.getName())
                        .doesNotContain("subjectRef");
                return new EvidenceCorrectionResult(
                        new EvidenceRef(REPLACEMENT_REF),
                        EvidenceOperationOutcome.CORRECTED, NOW);
            }
        };
        EvidenceCorrectionResult corrected = correctionService(
                newQuery, mutation, fingerprints).correct(
                        actor(ActorType.HUMAN),
                        new CorrectEvidenceCommand(
                                UUID_TWO, EVIDENCE_REF, "reason", "replacement"));
        assertThat(corrected.evidenceRef().value()).isEqualTo(REPLACEMENT_REF);
        assertThat(targetLoads).hasValue(1);
    }

    @Test
    void correctionRejectsNotFoundAndAlreadySupersededWithoutMutation() {
        AtomicInteger mutations = new AtomicInteger();
        EvidenceMutationPort mutation = new StubEvidenceMutationPort() {
            @Override
            public EvidenceCorrectionResult correct(
                    CorrectEvidenceSpec spec,
                    AuthorizedMutationContext context) {
                mutations.incrementAndGet();
                return null;
            }
        };
        EvidenceCorrectionService notFound = correctionService(
                emptyEvidenceQuery(), mutation, new EvidenceFingerprintFactory());
        assertThatThrownBy(() -> notFound.correct(
                actor(ActorType.HUMAN),
                new CorrectEvidenceCommand(UUID_ONE, EVIDENCE_REF, "reason", "replacement")))
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_NOT_FOUND));

        EvidenceRecord superseded = activeRecord(EVIDENCE_REF)
                .supersededBy(new EvidenceRef(REPLACEMENT_REF));
        EvidenceQueryPort supersededQuery = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                return Optional.of(superseded);
            }
        };
        assertThatThrownBy(() -> correctionService(
                supersededQuery, mutation, new EvidenceFingerprintFactory()).correct(
                        actor(ActorType.HUMAN),
                        new CorrectEvidenceCommand(
                                UUID_TWO, EVIDENCE_REF, "reason", "replacement")))
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_ALREADY_SUPERSEDED));
        assertThat(mutations).hasValue(0);
    }

    @Test
    void readsAuthorizeBeforeLookupPermitServiceAndLogBeforeReturningDetail() {
        AtomicInteger sequence = new AtomicInteger();
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(1);
            assertThat(capability).isEqualTo(EvidenceCapabilities.READ);
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        EvidenceRecord record = activeRecord(EVIDENCE_REF);
        EvidenceQueryPort query = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.of(record);
            }
        };
        EvidenceAccessLogPort accessLog = (ref, accessor, occurredAt) ->
                assertThat(sequence.incrementAndGet()).isEqualTo(3);
        EvidenceDetailReadService detail = new EvidenceDetailReadService(
                guard, query, accessLog, noOpMetrics(), CLOCK);

        EvidenceRecord returned = detail.read(actor(ActorType.SERVICE), EVIDENCE_REF);
        assertThat(returned).isSameAs(record);
        assertThat(sequence).hasValue(3);

        EvidenceQueryPort provenanceQuery = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                return Optional.of(record);
            }
        };
        EvidenceProvenanceQueryService provenance = new EvidenceProvenanceQueryService(
                allowAll(), provenanceQuery, noOpMetrics());
        assertThat(provenance.confirmProvenance(
                actor(ActorType.SERVICE), new EvidenceRef(EVIDENCE_REF)).outcome())
                .isEqualTo(EvidenceProvenanceOutcome.RECOGNIZED);
    }

    @Test
    void readDenialAndAccessLogFailureReturnNoContent() {
        AtomicInteger queryInteractions = new AtomicInteger();
        EvidenceQueryPort query = new StubEvidenceQueryPort() {
            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                queryInteractions.incrementAndGet();
                return Optional.of(activeRecord(EVIDENCE_REF));
            }
        };
        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));
        EvidenceDetailReadService deniedRead = new EvidenceDetailReadService(
                denied, query, (ref, actor, time) -> { }, noOpMetrics(), CLOCK);
        assertThatThrownBy(() -> deniedRead.read(actor(ActorType.SERVICE), EVIDENCE_REF))
                .isInstanceOf(AuthorizationDeniedException.class);
        EvidenceProvenanceQueryService deniedProvenance =
                new EvidenceProvenanceQueryService(denied, query, noOpMetrics());
        assertThatThrownBy(() -> deniedProvenance.confirmProvenance(
                actor(ActorType.SERVICE), new EvidenceRef(EVIDENCE_REF)))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(queryInteractions).hasValue(0);

        EvidenceDetailReadService failedAudit = new EvidenceDetailReadService(
                allowAll(), query,
                (ref, actor, time) -> {
                    throw new EvidenceAuthorityUnavailableException();
                }, noOpMetrics(), CLOCK);
        assertThatThrownBy(() -> failedAudit.read(
                actor(ActorType.SERVICE), EVIDENCE_REF))
                .isInstanceOf(EvidenceAuthorityUnavailableException.class);
        assertThat(queryInteractions).hasValue(1);
    }

    private EvidenceRecordingService recordingService(
            EvidenceQueryPort query,
            EvidenceMutationPort mutation,
            TradingAccountReferenceEligibilityService eligibility) {
        return new EvidenceRecordingService(
                allowAll(), query, mutation, new EvidenceFingerprintFactory(),
                eligibility, noOpMetrics(), CLOCK);
    }

    private EvidenceCorrectionService correctionService(
            EvidenceQueryPort query,
            EvidenceMutationPort mutation,
            EvidenceFingerprintFactory fingerprints) {
        return new EvidenceCorrectionService(
                allowAll(), query, mutation, fingerprints, noOpMetrics(), CLOCK);
    }

    private TradingAccountReferenceEligibilityService eligibilityService(
            AtomicInteger sequence,
            ActorContext expectedActor,
            AuthorityLifecycle lifecycle) {
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(3);
            assertThat(actor).isSameAs(expectedActor);
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 2, 3);
        });
        TradingAccountAuthorityQueryPort port = new StubQ010QueryPort() {
            @Override
            public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(4);
                return Optional.of(new EligibilityPersistenceView(
                        ref, lifecycle, 1, new AuthorityOperationId(UUID_ONE),
                        AuthorityLifecycle.ACTIVE, 1,
                        new AuthorityOperationId(UUID_TWO)));
            }
        };
        return new TradingAccountReferenceEligibilityService(
                guard, port, new AuthorityEvidenceFactory());
    }

    private AuthorizationGuard allowAll() {
        return new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1));
    }

    private EvidenceMetricsPort noOpMetrics() {
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
            public void recordAuthorizationDenied(
                    com.brokeros.risk.security.domain.Capability capability) {
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

    private ActorContext actor(ActorType type) {
        return new ActorContext(
                new ActorRef(UUID_ONE), type,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test", "principal-" + type.name().toLowerCase(), type),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString(UUID_THREE), "request-1",
                "0123456789abcdef0123456789abcdef");
    }

    private EvidenceQueryPort emptyEvidenceQuery() {
        return new StubEvidenceQueryPort();
    }

    private EvidenceQueryPort operationQuery(CompletedEvidenceOperation completed) {
        return new StubEvidenceQueryPort() {
            @Override
            public Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id) {
                return Optional.of(completed);
            }

            @Override
            public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
                throw new AssertionError("target lookup must be skipped for an exact replay");
            }
        };
    }

    private EvidenceRecord activeRecord(String ref) {
        return new EvidenceRecord(
                new EvidenceRef(ref), new TradingAccountRef(SUBJECT_REF),
                EvidenceSource.MANUAL, new ObservationText("observation"),
                EvidenceStatus.ACTIVE, new ActorRef(UUID_ONE), NOW, null, null);
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
        public List<EvidenceReferenceSummary> findSummariesBySubject(
                TradingAccountRef subjectRef,
                int limit) {
            return List.of();
        }
    }

    private static class StubEvidenceMutationPort implements EvidenceMutationPort {
        @Override
        public EvidenceRecordingResult record(
                RecordEvidenceSpec spec,
                AuthorizedMutationContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EvidenceCorrectionResult correct(
                CorrectEvidenceSpec spec,
                AuthorizedMutationContext context) {
            throw new UnsupportedOperationException();
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
