package com.brokeros.risk.actionoutcome.application;

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

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionCapabilities;
import com.brokeros.risk.action.application.ActionMetricOperation;
import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionProvenanceOutcome;
import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.ActionSource;
import com.brokeros.risk.action.domain.ActionStatus;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import com.brokeros.risk.action.domain.IntentText;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeAccessLogPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMutationPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeQueryPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationType;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeProvenanceOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSemanticFingerprint;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSource;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;
import com.brokeros.risk.actionoutcome.domain.OutcomeText;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.domain.DecisionRef;
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
import org.junit.jupiter.api.Test;

class ActionOutcomeApplicationTests {

    private static final Instant NOW = Instant.parse("2026-09-01T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String UUID_ONE = "00000000-0000-4000-8000-000000000001";
    private static final String UUID_TWO = "00000000-0000-4000-8000-000000000002";
    private static final String UUID_THREE = "00000000-0000-4000-8000-000000000003";
    private static final String UUID_FOUR = "00000000-0000-4000-8000-000000000004";
    private static final String DECISION_REF = "dec-" + UUID_TWO;
    private static final String ACTION_REF = "act-" + UUID_THREE;
    private static final String ACTION_OUTCOME_REF = "aoc-" + UUID_FOUR;

    @Test
    void recordingUsesCanonicalOrderAndPassesOwnActorToActionAuthority() {
        AtomicInteger sequence = new AtomicInteger();
        ActorContext actor = actor(ActorType.HUMAN);
        AuthorizationGuard guard = new AuthorizationGuard((context, capability) -> {
            assertThat(context).isSameAs(actor);
            int current = sequence.incrementAndGet();
            if (capability.equals(ActionOutcomeCapabilities.RECORD)) {
                assertThat(current).isEqualTo(1);
            } else if (capability.equals(ActionCapabilities.READ)) {
                assertThat(current).isEqualTo(3);
            } else {
                throw new AssertionError("unexpected capability " + capability);
            }
            return AuthorizationDecision.allow(context.actorRef(), capability, NOW, 1, 1);
        });
        ActionOutcomeQueryPort query = new StubActionOutcomeQueryPort() {
            @Override
            public Optional<CompletedActionOutcomeOperation> findOperation(
                    ActionOutcomeOperationId id) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.empty();
            }
        };
        ActionQueryPort actionQuery = new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(4);
                assertThat(ref.value()).isEqualTo(ACTION_REF);
                return Optional.of(actionRecord());
            }
        };
        ActionOutcomeMutationPort mutation = (spec, context) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(5);
            assertThat(context.actorContext()).isSameAs(actor);
            assertThat(spec.actionRef().value()).isEqualTo(ACTION_REF);
            return completed(spec, context);
        };
        ActionOutcomeRecordingService service = service(
                guard, query, mutation, actionService(guard, actionQuery));

        CompletedActionOutcomeOperation result = service.record(
                actor, command(UUID_ONE, ACTION_REF, "observed outcome"));

        assertThat(result.actionOutcomeRef().value()).isEqualTo(ACTION_OUTCOME_REF);
        assertThat(result.actionOutcomeRecord().actionRef().value()).isEqualTo(ACTION_REF);
        assertThat(sequence).hasValue(5);
    }

    @Test
    void exactReplayDoesNotCallActionPortSecondTimeAndChangedReplayConflicts() {
        ActionOutcomeFingerprintFactory fingerprints =
                new ActionOutcomeFingerprintFactory();
        ActionOutcomeSemanticFingerprint fingerprint = fingerprints.forRecord(
                "not-an-action", "");
        CompletedActionOutcomeOperation completed = completed(
                new ActionOutcomeOperationId(UUID_ONE), fingerprint,
                actionOutcomeRecord());
        AtomicInteger actionCalls = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();
        ActionOutcomeQueryPort query = new StubActionOutcomeQueryPort() {
            @Override
            public Optional<CompletedActionOutcomeOperation> findOperation(
                    ActionOutcomeOperationId id) {
                return Optional.of(completed);
            }
        };
        ActionQueryPort actionQuery = new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
                actionCalls.incrementAndGet();
                return Optional.of(actionRecord());
            }
        };
        ActionOutcomeRecordingService service = service(
                allowAll(), query, (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                }, actionService(allowAll(), actionQuery));

        CompletedActionOutcomeOperation replay = service.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, "not-an-action", ""));

        assertThat(replay).isSameAs(completed);
        assertThat(actionCalls).hasValue(0);
        assertThat(mutations).hasValue(0);

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, "not-an-action", "changed")))
                .isInstanceOf(ActionOutcomeConflictException.class)
                .satisfies(error -> assertThat(
                        ((ActionOutcomeException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_OUTCOME_IDEMPOTENCY_CONFLICT));
        assertThat(actionCalls).hasValue(0);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void serviceActorIsRejectedBeforeReplayContentAndActionChecks() {
        AtomicInteger replayChecks = new AtomicInteger();
        AtomicInteger actionCalls = new AtomicInteger();
        ActionOutcomeQueryPort query = new StubActionOutcomeQueryPort() {
            @Override
            public Optional<CompletedActionOutcomeOperation> findOperation(
                    ActionOutcomeOperationId id) {
                replayChecks.incrementAndGet();
                return Optional.empty();
            }
        };
        ActionQueryPort actionQuery = new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
                actionCalls.incrementAndGet();
                return Optional.empty();
            }
        };
        ActionOutcomeRecordingService service = service(
                allowAll(), query, new StubActionOutcomeMutationPort(),
                actionService(allowAll(), actionQuery));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.SERVICE),
                command(UUID_ONE, "not-an-action", "")))
                .isInstanceOf(ActionOutcomeException.class)
                .satisfies(error -> assertThat(
                        ((ActionOutcomeException) error).getResultCode())
                        .isEqualTo(
                                ResultCode.ACTION_OUTCOME_ACTOR_TYPE_NOT_PERMITTED));
        assertThat(replayChecks).hasValue(0);
        assertThat(actionCalls).hasValue(0);
    }

    @Test
    void unrecognizedActionIsRejectedBeforeAnyActionOutcomeWrite() {
        AtomicInteger mutations = new AtomicInteger();
        ActionOutcomeRecordingService service = service(
                allowAll(), new StubActionOutcomeQueryPort(),
                (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                actionService(allowAll(), new StubActionQueryPort()));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, ACTION_REF, "observed outcome")))
                .isInstanceOf(ActionOutcomeException.class)
                .satisfies(error -> assertThat(
                        ((ActionOutcomeException) error).getResultCode())
                        .isEqualTo(
                                ResultCode.ACTION_OUTCOME_ACTION_NOT_RECOGNIZED));
        assertThat(mutations).hasValue(0);
    }

    @Test
    void actionAuthorityFailureIsMappedAndActionReadDenialPreventsWrites() {
        AtomicInteger mutations = new AtomicInteger();
        ActionQueryPort unavailableQuery = new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
                throw new ActionAuthorityUnavailableException();
            }
        };
        ActionOutcomeRecordingService unavailable = service(
                allowAll(), new StubActionOutcomeQueryPort(),
                (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                actionService(allowAll(), unavailableQuery));

        assertThatThrownBy(() -> unavailable.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, ACTION_REF, "observed outcome")))
                .isInstanceOf(ActionOutcomeException.class)
                .satisfies(error -> assertThat(
                        ((ActionOutcomeException) error).getResultCode())
                        .isEqualTo(
                                ResultCode.ACTION_OUTCOME_ACTION_AUTHORITY_UNAVAILABLE));

        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) -> {
            if (capability.equals(ActionCapabilities.READ)) {
                return AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null);
            }
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        ActionOutcomeRecordingService deniedService = service(
                denied, new StubActionOutcomeQueryPort(),
                (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                actionService(denied, actionQueryReturning(actionRecord())));

        assertThatThrownBy(() -> deniedService.record(
                actor(ActorType.HUMAN),
                command(UUID_TWO, ACTION_REF, "observed outcome")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void actionOutcomeAuthorizationDenialPrecedesEveryActionOutcomePort() {
        AtomicInteger queries = new AtomicInteger();
        AtomicInteger mutations = new AtomicInteger();
        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));
        ActionOutcomeQueryPort query = new StubActionOutcomeQueryPort() {
            @Override
            public Optional<CompletedActionOutcomeOperation> findOperation(
                    ActionOutcomeOperationId id) {
                queries.incrementAndGet();
                return Optional.empty();
            }
        };
        ActionOutcomeRecordingService service = service(
                denied, query, (spec, context) -> {
                    mutations.incrementAndGet();
                    return completed(spec, context);
                },
                actionService(allowAll(), actionQueryReturning(actionRecord())));

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                command(UUID_ONE, ACTION_REF, "observed outcome")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(queries).hasValue(0);
        assertThat(mutations).hasValue(0);
    }

    @Test
    void readsAuthorizeBeforeLookupPermitServiceAndAuditBeforeDisclosure() {
        AtomicInteger sequence = new AtomicInteger();
        AuthorizationGuard guard = new AuthorizationGuard((actor, capability) -> {
            assertThat(sequence.incrementAndGet()).isEqualTo(1);
            assertThat(capability).isEqualTo(ActionOutcomeCapabilities.READ);
            return AuthorizationDecision.allow(actor.actorRef(), capability, NOW, 1, 1);
        });
        ActionOutcomeRecord record = actionOutcomeRecord();
        ActionOutcomeQueryPort query = new StubActionOutcomeQueryPort() {
            @Override
            public Optional<ActionOutcomeRecord> findByRef(ActionOutcomeRef ref) {
                assertThat(sequence.incrementAndGet()).isEqualTo(2);
                return Optional.of(record);
            }
        };
        ActionOutcomeAccessLogPort accessLog = (ref, accessor, occurredAt) ->
                assertThat(sequence.incrementAndGet()).isEqualTo(3);
        ActionOutcomeDetailReadService detail = new ActionOutcomeDetailReadService(
                guard, query, accessLog, noOpActionOutcomeMetrics(), CLOCK);

        assertThat(detail.readDetail(
                actor(ActorType.SERVICE), ACTION_OUTCOME_REF)).isSameAs(record);
        assertThat(sequence).hasValue(3);

        ActionOutcomeProvenanceQueryService provenance =
                new ActionOutcomeProvenanceQueryService(
                        allowAll(), actionOutcomeQueryReturning(record),
                        noOpActionOutcomeMetrics());
        assertThat(provenance.confirmProvenance(
                actor(ActorType.SERVICE),
                new ActionOutcomeRef(ACTION_OUTCOME_REF)).outcome())
                .isEqualTo(ActionOutcomeProvenanceOutcome.RECOGNIZED);
    }

    @Test
    void deniedOrUnavailableReadsNeverQueryAndAuditFailureDisclosesNoContent() {
        AtomicInteger queries = new AtomicInteger();
        ActionOutcomeQueryPort query = new StubActionOutcomeQueryPort() {
            @Override
            public Optional<ActionOutcomeRecord> findByRef(ActionOutcomeRef ref) {
                queries.incrementAndGet();
                return Optional.of(actionOutcomeRecord());
            }
        };
        AuthorizationGuard denied = new AuthorizationGuard((actor, capability) ->
                AuthorizationDecision.deny(
                        actor.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));
        assertThatThrownBy(() -> new ActionOutcomeDetailReadService(
                denied, query, (ref, actor, time) -> { },
                noOpActionOutcomeMetrics(), CLOCK)
                .readDetail(actor(ActorType.SERVICE), ACTION_OUTCOME_REF))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThatThrownBy(() -> new ActionOutcomeProvenanceQueryService(
                denied, query, noOpActionOutcomeMetrics()).confirmProvenance(
                        actor(ActorType.SERVICE),
                        new ActionOutcomeRef(ACTION_OUTCOME_REF)))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(queries).hasValue(0);

        AuthorizationGuard unavailable = new AuthorizationGuard((actor, capability) -> {
            throw new SecurityDependencyUnavailableException(
                    new IllegalStateException("down"));
        });
        assertThatThrownBy(() -> new ActionOutcomeDetailReadService(
                unavailable, query, (ref, actor, time) -> { },
                noOpActionOutcomeMetrics(), CLOCK)
                .readDetail(actor(ActorType.SERVICE), ACTION_OUTCOME_REF))
                .isInstanceOf(SecurityDependencyUnavailableException.class);
        assertThat(queries).hasValue(0);

        ActionOutcomeDetailReadService failedAudit =
                new ActionOutcomeDetailReadService(
                        allowAll(), query,
                        (ref, actor, time) -> {
                            throw new ActionOutcomeAuthorityUnavailableException();
                        }, noOpActionOutcomeMetrics(), CLOCK);
        assertThatThrownBy(() -> failedAudit.readDetail(
                actor(ActorType.SERVICE), ACTION_OUTCOME_REF))
                .isInstanceOf(ActionOutcomeAuthorityUnavailableException.class);
        assertThat(queries).hasValue(1);
    }

    private ActionOutcomeRecordingService service(
            AuthorizationGuard guard,
            ActionOutcomeQueryPort query,
            ActionOutcomeMutationPort mutation,
            ActionProvenanceQueryService actionService) {
        return new ActionOutcomeRecordingService(
                guard, query, mutation, new ActionOutcomeFingerprintFactory(),
                actionService, new AuthorizedMutationFactory(CLOCK),
                noOpActionOutcomeMetrics());
    }

    private ActionProvenanceQueryService actionService(
            AuthorizationGuard guard,
            ActionQueryPort query) {
        return new ActionProvenanceQueryService(
                guard, query, noOpActionMetrics());
    }

    private CompletedActionOutcomeOperation completed(
            RecordActionOutcomeSpec spec,
            AuthorizedMutationContext context) {
        ActionOutcomeRecord record = new ActionOutcomeRecord(
                new ActionOutcomeRef(ACTION_OUTCOME_REF),
                spec.actionRef(), spec.outcomeText(), ActionOutcomeSource.MANUAL,
                context.actorContext().actorRef(), context.occurredAt());
        return completed(spec.operationId(), context.fingerprint(), record);
    }

    private CompletedActionOutcomeOperation completed(
            ActionOutcomeOperationId operationId,
            ActionOutcomeSemanticFingerprint fingerprint,
            ActionOutcomeRecord record) {
        return new CompletedActionOutcomeOperation(
                operationId, ActionOutcomeOperationType.RECORD, fingerprint,
                record.actionOutcomeRef(), ActionOutcomeOperationOutcome.CREATED,
                record.recordedAt(), record);
    }

    private ActionOutcomeRecord actionOutcomeRecord() {
        return new ActionOutcomeRecord(
                new ActionOutcomeRef(ACTION_OUTCOME_REF),
                new ActionRef(ACTION_REF),
                new OutcomeText("observed outcome"),
                ActionOutcomeSource.MANUAL,
                new ActorRef(UUID_ONE), NOW);
    }

    private ActionRecord actionRecord() {
        return new ActionRecord(
                new ActionRef(ACTION_REF), new DecisionRef(DECISION_REF),
                new IntentText("intent"), ActionStatus.PROPOSED, ActionSource.MANUAL,
                new ActorRef(UUID_ONE), NOW);
    }

    private RecordActionOutcomeCommand command(
            String operationId,
            String actionRef,
            String outcomeText) {
        return new RecordActionOutcomeCommand(operationId, actionRef, outcomeText);
    }

    private ActionOutcomeQueryPort actionOutcomeQueryReturning(
            ActionOutcomeRecord record) {
        return new StubActionOutcomeQueryPort() {
            @Override
            public Optional<ActionOutcomeRecord> findByRef(ActionOutcomeRef ref) {
                return Optional.of(record);
            }
        };
    }

    private ActionQueryPort actionQueryReturning(ActionRecord record) {
        return new StubActionQueryPort() {
            @Override
            public Optional<ActionRecord> findByRef(ActionRef ref) {
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

    private ActionOutcomeMetricsPort noOpActionOutcomeMetrics() {
        return new ActionOutcomeMetricsPort() {
            @Override
            public void recordOperation(
                    ActionOutcomeMetricOperation operation,
                    ActionOutcomeOperationOutcome outcome) {
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
                    ActionOutcomeMetricOperation operation,
                    Duration duration) {
            }
        };
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

    private static class StubActionOutcomeQueryPort
            implements ActionOutcomeQueryPort {
        @Override
        public Optional<CompletedActionOutcomeOperation> findOperation(
                ActionOutcomeOperationId id) {
            return Optional.empty();
        }

        @Override
        public Optional<ActionOutcomeRecord> findByRef(ActionOutcomeRef ref) {
            return Optional.empty();
        }

        @Override
        public List<ActionOutcomeReferenceSummary> findSummariesByAction(
                ActionRef actionRef,
                int limit) {
            return List.of();
        }
    }

    private static class StubActionOutcomeMutationPort
            implements ActionOutcomeMutationPort {
        @Override
        public CompletedActionOutcomeOperation record(
                RecordActionOutcomeSpec spec,
                AuthorizedMutationContext context) {
            throw new UnsupportedOperationException();
        }
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

        @Override
        public List<com.brokeros.risk.action.application.ActionReferenceSummary>
                findSummariesByDecision(DecisionRef decisionRef, int limit) {
            return List.of();
        }
    }
}
