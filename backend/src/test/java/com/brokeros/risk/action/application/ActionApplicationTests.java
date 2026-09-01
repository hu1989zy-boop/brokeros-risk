package com.brokeros.risk.action.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.brokeros.risk.action.application.port.ActionAccessLogPort;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.application.port.ActionMutationPort;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionOperationType;
import com.brokeros.risk.action.domain.ActionProvenanceOutcome;
import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.ActionSemanticFingerprint;
import com.brokeros.risk.action.domain.ActionSource;
import com.brokeros.risk.action.domain.ActionStatus;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import com.brokeros.risk.action.domain.IntentText;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionCapabilities;
import com.brokeros.risk.decision.application.DecisionMetricOperation;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.decision.domain.ConclusionText;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.decision.domain.DecisionSource;
import com.brokeros.risk.evidence.domain.EvidenceRef;
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
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.Test;

class ActionApplicationTests {

    private static final Instant NOW = Instant.parse("2026-09-01T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";
    private static final String UUID_THREE = "00000000-0000-4000-8000-000000000003";
    private static final String UUID_FOUR = "00000000-0000-4000-8000-000000000004";
    private static final String DECISION_REF = "dec-" + UUID_TWO;
    private static final String ACTION_REF = "act-" + UUID_THREE;

    @Test
    void recordingUsesCanonicalOrderAndPassesOwnActorToDecisionAuthority() {
        AtomicInteger sequence = new AtomicInteger();
        ActorContext actor = actor(ActorType.HUMAN);
        AuthorizationGuard guard = new AuthorizationGuard((context, capability) -> {
            assertThat(context).isSameAs(actor);
            int current = sequence.incrementAndGet();
            if (capability.equals(ActionCapabilities.RECORD)) {
                assertThat(current).isEqualTo(1);
            } else if (capability.equals(DecisionCapabilities.READ)) {
                assertThat(current).isEqualTo(3);
            } else {
                throw new AssertionError("unexpected capability " + capability);
            }
            return AuthorizationDecision.allow(context.actorRef(), capability, NOW, 1, 1);
        });
        ActionQueryPort query = new StubActionQueryPort() {
            @Override
            public Optional<CompletedActionOperation> findOperation(ActionOperationId id) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.empty();
            }
        };
        DecisionQueryPort decisionQuery = new StubDecisionQueryPort() {
            @Override
            public Optional<DecisionRecord> findByRef(DecisionRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(4);
                assertThat(ref.value()).isEqualTo(DECISION_REF);
                return Optional.of(decisionRecord());
            }
        };
        ActionMutationPort mutation = (spec, context) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(5);
            assertThat(context.actorContext()).isSameAs(actor);
            assertThat(spec.decisionRef().value()).isEqualTo(DECISION_REF);
            return completed(spec, context);
        };
        ActionRecordingService service = service(
                guard, query, mutation, decisionService(guard, decisionQuery));

        CompletedActionOperation result = service.record(
                actor, command(UUID_ONE, DECISION_REF, "intent"));

        assertThat(result.actionRef().value()).isEqualTo(ACTION_REF);
        assertThat(result.actionRecord().status()).isEqualTo(ActionStatus.PROPOSED);
        assertThat(sequence).hasValue(5);
    }

    @Test
    void exactReplayDoesNotCallDecisionPortSecondTimeAndChangedReplayConflicts() {
        ActionFingerprintFactory fingerprints = new ActionFingerprintFactory();
        ActionSemanticFingerprint fingerprint = fingerprints.forRecord(
                "not-a-decision", "");
        CompletedActionOperation completed = completed(
                new ActionOperationId(UUID_ONE), fingerprint, actionRecord());
        AtomicInteger decisionCalls = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();
        ActionQueryPort query = new StubActionQueryPort() {
            @Override
            public Optional<CompletedActionOperation> findOperation(ActionOperationId id) {
                return Optional.of(completed);
            }
        };
        DecisionQueryPort decisionQuery = new StubDecisionQueryPort() {
            @Override
            public Optional<DecisionRecord> findByRef(DecisionRef ref) {
                decisionCalls.incrementAndGet();
                return Optional.of(decisionRecord());
            }
        };
        ActionRecordingService service = service(
                allowAll(), query, (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                }, decisionService(allowAll(), decisionQuery));

        CompletedActionOperation replay = service.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, "not-a-decision", ""));

        assertThat(replay).isSameAs(completed);
        assertThat(decisionCalls).hasValue(0);
        assertThat(mutations).hasValue(0);

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, "not-a-decision", "changed")))
                .isInstanceOf(ActionConflictException.class)
                .satisfies(error -> assertThat(((ActionException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_IDEMPOTENCY_CONFLICT));
        assertThat(decisionCalls).hasValue(0);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void serviceActorIsRejectedBeforeReplayContentAndDecisionChecks() {
        AtomicInteger replayChecks = new AtomicInteger();
        AtomicInteger decisionCalls = new AtomicInteger();
        ActionQueryPort query = new StubActionQueryPort() {
            @Override
            public Optional<CompletedActionOperation> findOperation(ActionOperationId id) {
                replayChecks.incrementAndGet();
                return Optional.empty();
            }
        };
        DecisionQueryPort decisionQuery = new StubDecisionQueryPort() {
            @Override
            public Optional<DecisionRecord> findByRef(DecisionRef ref) {
                decisionCalls.incrementAndGet();
                return Optional.empty();
            }
        };
        ActionRecordingService service = service(
                allowAll(), query, new StubActionMutationPort(),
                decisionService(allowAll(), decisionQuery));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.SERVICE),
                command(UUID_ONE, "not-a-decision", "")))
                .isInstanceOf(ActionException.class)
                .satisfies(error -> assertThat(((ActionException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_ACTOR_TYPE_NOT_PERMITTED));
        assertThat(replayChecks).hasValue(0);
        assertThat(decisionCalls).hasValue(0);
    }

    @Test
    void unrecognizedDecisionIsRejectedBeforeAnyActionWrite() {
        AtomicInteger mutations = new AtomicInteger();
        ActionRecordingService service = service(
                allowAll(), new StubActionQueryPort(),
                (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                decisionService(allowAll(), new StubDecisionQueryPort()));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, DECISION_REF, "intent")))
                .isInstanceOf(ActionException.class)
                .satisfies(error -> assertThat(((ActionException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_DECISION_NOT_RECOGNIZED));
        assertThat(mutations).hasValue(0);
    }

    @Test
    void decisionAuthorityFailureIsMappedAndDecisionReadDenialPreventsWrites() {
        AtomicInteger mutations = new AtomicInteger();
        DecisionQueryPort unavailableQuery = new StubDecisionQueryPort() {
            @Override
            public Optional<DecisionRecord> findByRef(DecisionRef ref) {
                throw new DecisionAuthorityUnavailableException();
            }
        };
        ActionRecordingService unavailable = service(
                allowAll(), new StubActionQueryPort(),
                (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                decisionService(allowAll(), unavailableQuery));

        assertThatThrownBy(() -> unavailable.record(
                actor(ActorType.HUMAN), command(UUID_ONE, DECISION_REF, "intent")))
                .isInstanceOf(ActionException.class)
                .satisfies(error -> assertThat(((ActionException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_DECISION_AUTHORITY_UNAVAILABLE));

        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) -> {
            if (capability.equals(DecisionCapabilities.READ)) {
                return AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null);
            }
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        ActionRecordingService deniedService = service(
                denied, new StubActionQueryPort(),
                (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                decisionService(denied, queryReturning(decisionRecord())));

        assertThatThrownBy(() -> deniedService.record(
                actor(ActorType.HUMAN), command(UUID_TWO, DECISION_REF, "intent")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void actionAuthorizationDenialPrecedesEveryActionPort() {
        AtomicInteger queries = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();
        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));
        ActionQueryPort query = new StubActionQueryPort() {
            @Override
            public Optional<CompletedActionOperation> findOperation(ActionOperationId id) {
                queries.incrementAndGet();
                return Optional.empty();
            }
        };
        ActionRecordingService service = service(
                denied, query, (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                decisionService(allowAll(), queryReturning(decisionRecord())));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN), command(UUID_ONE, DECISION_REF, "intent")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(queries).hasValue(0);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void readsAuthorizeBeforeLookupPermitServiceAndAuditBeforeDisclosure() {
        AtomicInteger sequence = new AtomicInteger();
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(1);
            assertThat(capability).isEqualTo(ActionCapabilities.READ);
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        ActionRecord record = actionRecord();
        ActionQueryPort query = new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.of(record);
            }
        };
        ActionAccessLogPort accessLog = (ref, accessor, occurredAt) ->
                assertThat(sequence.incrementAndGet()).isEqualTo(3);
        ActionDetailReadService detail = new ActionDetailReadService(
                guard, query, accessLog, noOpActionMetrics(), CLOCK);

        assertThat(detail.readDetail(actor(ActorType.SERVICE), ACTION_REF)).isSameAs(record);
        assertThat(sequence).hasValue(3);

        ActionProvenanceQueryService provenance = new ActionProvenanceQueryService(
                allowAll(), queryReturning(record), noOpActionMetrics());
        assertThat(provenance.confirmProvenance(
                actor(ActorType.SERVICE), new ActionRef(ACTION_REF)).outcome())
                .isEqualTo(ActionProvenanceOutcome.RECOGNIZED);
    }

    @Test
    void deniedOrUnavailableReadsNeverQueryAndAuditFailureDisclosesNoContent() {
        AtomicInteger queries = new AtomicInteger();
        ActionQueryPort query = new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
                queries.incrementAndGet();
                return Optional.of(actionRecord());
            }
        };
        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));
        assertThatThrownBy(() -> new ActionDetailReadService(
                denied, query, (ref, actor, time) -> { }, noOpActionMetrics(), CLOCK)
                .readDetail(actor(ActorType.SERVICE), ACTION_REF))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> new ActionProvenanceQueryService(
                denied, query, noOpActionMetrics()).confirmProvenance(
                        actor(ActorType.SERVICE), new ActionRef(ACTION_REF)))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(queries).hasValue(0);

        AuthorizationGuard unavailable = new AuthorizationGuard((actor, capability) -> {
            throw new SecurityDependencyUnavailableException(new IllegalStateException("down"));
        });
        assertThatThrownBy(() -> new ActionDetailReadService(
                unavailable, query, (ref, actor, time) -> { }, noOpActionMetrics(), CLOCK)
                .readDetail(actor(ActorType.SERVICE), ACTION_REF))
                .isInstanceOf(SecurityDependencyUnavailableException.class);
        assertThat(queries).hasValue(0);

        ActionDetailReadService failedAudit = new ActionDetailReadService(
                allowAll(), query,
                (ref, actor, time) -> {
                    throw new ActionAuthorityUnavailableException();
                }, noOpActionMetrics(), CLOCK);
        assertThatThrownBy(() -> failedAudit.readDetail(
                actor(ActorType.SERVICE), ACTION_REF))
                .isInstanceOf(ActionAuthorityUnavailableException.class);
        assertThat(queries).hasValue(1);
    }

    private ActionRecordingService service(
            AuthorizationGuard guard,
            ActionQueryPort query,
            ActionMutationPort mutation,
            DecisionProvenanceQueryService decisionService) {
        return new ActionRecordingService(
                guard, query, mutation, new ActionFingerprintFactory(),
                decisionService, new AuthorizedMutationFactory(CLOCK),
                noOpActionMetrics());
    }

    private DecisionProvenanceQueryService decisionService(
            AuthorizationGuard guard,
            DecisionQueryPort query) {
        return new DecisionProvenanceQueryService(
                guard, query, noOpDecisionMetrics());
    }

    private CompletedActionOperation completed(
            RecordActionSpec spec,
            AuthorizedMutationContext context) {
        ActionRecord record = new ActionRecord(
                new ActionRef(ACTION_REF), spec.decisionRef(), spec.intentText(),
                ActionStatus.PROPOSED, ActionSource.MANUAL,
                context.actorContext().actorRef(), context.occurredAt());
        return completed(spec.operationId(), context.fingerprint(), record);
    }

    private CompletedActionOperation completed(
            ActionOperationId operationId,
            ActionSemanticFingerprint fingerprint,
            ActionRecord record) {
        return new CompletedActionOperation(
                operationId, ActionOperationType.RECORD, fingerprint,
                record.actionRef(), ActionOperationOutcome.CREATED,
                record.recordedAt(), record);
    }

    private ActionRecord actionRecord() {
        return new ActionRecord(
                new ActionRef(ACTION_REF), new DecisionRef(DECISION_REF),
                new IntentText("intent"), ActionStatus.PROPOSED, ActionSource.MANUAL,
                new ActorRef(UUID_ONE), NOW);
    }

    private DecisionRecord decisionRecord() {
        return new DecisionRecord(
                new DecisionRef(DECISION_REF),
                new TradingAccountRef("ta-" + UUID_ONE),
                Set.of(new EvidenceRef("ev-" + UUID_FOUR)),
                new ConclusionText("conclusion"), DecisionSource.MANUAL,
                new ActorRef(UUID_ONE), NOW);
    }

    private RecordActionCommand command(
            String operationId,
            String decisionRef,
            String intentText) {
        return new RecordActionCommand(operationId, decisionRef, intentText);
    }

    private ActionQueryPort queryReturning(ActionRecord record) {
        return new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
                return Optional.of(record);
            }
        };
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

    private ActionMetricsPort noOpActionMetrics() {
        return new ActionMetricsPort() {
            @Override
            public void recordOperation(
                    ActionMetricOperation operation,
                    ActionOperationOutcome outcome) {
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
                    ActionMetricOperation operation,
                    Duration duration) {
            }
        };
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

    private static class StubActionQueryPort implements ActionQueryPort {
        @Override
        public Optional<CompletedActionOperation> findOperation(ActionOperationId id) {
            return Optional.empty();
        }

        @Override
        public Optional<ActionRecord> findByRef(ActionRef ref) {
            return Optional.empty();
        }
    }

    private static class StubActionMutationPort implements ActionMutationPort {
        @Override
        public CompletedActionOperation record(
                RecordActionSpec spec,
                AuthorizedMutationContext context) {
            throw new UnsupportedOperationException();
        }
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
    }
}
