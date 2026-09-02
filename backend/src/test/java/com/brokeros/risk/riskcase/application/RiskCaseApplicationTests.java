package com.brokeros.risk.riskcase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.audit.domain.AuditRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.riskcase.application.port.ActionOutcomeReferenceQuery;
import com.brokeros.risk.riskcase.application.port.ActionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.DecisionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.EvidenceReferenceQuery;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.application.port.TradingAccountReferenceQuery;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCaseId;
import com.brokeros.risk.riskcase.domain.RiskCasePriority;
import com.brokeros.risk.riskcase.domain.TradingAccountSubjectRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class RiskCaseApplicationTests {

    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final ActorRef ACTOR_REF =
            new ActorRef("50000000-0000-4000-8000-000000000001");
    private static final ActorRef ASSIGNEE_REF =
            new ActorRef("50000000-0000-4000-8000-000000000002");
    private static final TradingAccountRef SUBJECT =
            new TradingAccountRef("ta-60000000-0000-4000-8000-000000000001");
    private static final DecisionRef DECISION =
            new DecisionRef("dec-70000000-0000-4000-8000-000000000001");
    private static final ActionRef ACTION =
            new ActionRef("act-80000000-0000-4000-8000-000000000001");

    private AuthorizationGuard authorizationGuard;
    private RiskCaseRepository repository;
    private AuditRecordWriter auditWriter;
    private RiskCaseMetricsPort metrics;
    private ActorContext actorContext;

    @BeforeEach
    void setUp() {
        authorizationGuard = mock(AuthorizationGuard.class);
        repository = mock(RiskCaseRepository.class);
        auditWriter = mock(AuditRecordWriter.class);
        metrics = mock(RiskCaseMetricsPort.class);
        actorContext = actorContext();
    }

    @Test
    void authorizationDenialOccursBeforeAnyCaseLoadOrProviderCall() {
        TradingAccountReferenceQuery subjectQuery = mock(TradingAccountReferenceQuery.class);
        DecisionReferenceQuery decisionQuery = mock(DecisionReferenceQuery.class);
        doThrow(new AuthorizationDeniedException()).when(authorizationGuard)
                .requireAllowed(actorContext, RiskCaseCapabilities.CREATE);
        RiskCaseCreationService service = creationService(subjectQuery, decisionQuery);

        assertThatThrownBy(() -> service.create(actorContext, manualCommand()))
                .isInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(repository, subjectQuery, decisionQuery, auditWriter);
    }

    @Test
    void exactCreateReplaySkipsProvidersAndDifferentPayloadConflicts() {
        RiskCase existing = persistedOpen();
        RiskCaseFingerprintFactory fingerprints = new RiskCaseFingerprintFactory();
        CreateRiskCaseCommand command = manualCommand();
        byte[] keyHash = fingerprints.idempotencyKeyHash(command.idempotencyKey());
        byte[] requestHash = fingerprints.requestHash(command);
        when(repository.findByCreationKey(ACTOR_REF, keyHash))
                .thenReturn(Optional.of(new RiskCaseCreationRecord(existing, requestHash)));
        TradingAccountReferenceQuery subjectQuery = mock(TradingAccountReferenceQuery.class);
        DecisionReferenceQuery decisionQuery = mock(DecisionReferenceQuery.class);
        RiskCaseCreationService service = creationService(subjectQuery, decisionQuery);

        assertThat(service.create(actorContext, command).caseNumber())
                .isEqualTo(existing.snapshot().caseNumber());
        verifyNoInteractions(subjectQuery, decisionQuery, auditWriter);

        CreateRiskCaseCommand changed = new CreateRiskCaseCommand(
                "MANUAL", "TRADING_ACCOUNT", SUBJECT.value(), "changed",
                "NORMAL", null, command.idempotencyKey());
        assertThatThrownBy(() -> service.create(actorContext, changed))
                .isInstanceOf(RiskCaseException.class)
                .extracting(error -> ((RiskCaseException) error).getResultCode())
                .isEqualTo(ResultCode.RISK_CASE_IDEMPOTENCY_CONFLICT);
    }

    @Test
    void createUsesAuthorizationThenReplayThenSubjectProviderAndAtomicWrites() {
        when(repository.findByCreationKey(any(), any())).thenReturn(Optional.empty());
        when(repository.insertRoot(any(), any(), any())).thenAnswer(invocation -> {
            RiskCase riskCase = invocation.getArgument(0);
            riskCase.markPersisted(new RiskCaseId(1));
            return riskCase;
        });
        TradingAccountReferenceQuery subjectQuery = mock(TradingAccountReferenceQuery.class);
        DecisionReferenceQuery decisionQuery = mock(DecisionReferenceQuery.class);
        RiskCaseCreationService service = creationService(subjectQuery, decisionQuery);

        service.create(actorContext, manualCommand());

        InOrder order = inOrder(authorizationGuard, repository, subjectQuery, auditWriter);
        order.verify(authorizationGuard)
                .requireAllowed(actorContext, RiskCaseCapabilities.CREATE);
        order.verify(repository).findByCreationKey(any(), any());
        order.verify(subjectQuery).requireEligibleForNewCase(actorContext, SUBJECT);
        order.verify(repository).insertRoot(any(), any(), any());
        order.verify(repository).appendTransition(any());
        order.verify(auditWriter).append(any());
    }

    @Test
    void trustedActorContextSuppliesAssignmentAndAuditActor() {
        RiskCase riskCase = persistedOpen();
        when(repository.findByCaseNumber(riskCase.snapshot().caseNumber()))
                .thenReturn(Optional.of(riskCase));
        when(repository.updateRoot(any(), anyLong())).thenReturn(1);
        RiskCaseCommandService service = commandService();

        service.changeAssignment(actorContext, riskCase.snapshot().caseNumber().value(),
                ASSIGNEE_REF.value(), "assign", 1);

        ArgumentCaptor<AuditRecord> audit = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditWriter).append(audit.capture());
        assertThat(audit.getValue().actorRef()).isEqualTo(ACTOR_REF);
        assertThat(riskCase.snapshot().assignment().assignedBy()).isEqualTo(ACTOR_REF);
    }

    @Test
    void associateActionRejectsOriginatingDecisionNotAssociatedBeforeWrites() {
        RiskCase riskCase = reviewCase();
        when(repository.findByCaseNumber(riskCase.snapshot().caseNumber()))
                .thenReturn(Optional.of(riskCase));
        when(repository.isDecisionAssociated(riskCase.snapshot().id(), DECISION))
                .thenReturn(false);
        when(repository.findEffectiveAction(any(), any())).thenReturn(Optional.empty());
        ActionReferenceQuery actionQuery = mock(ActionReferenceQuery.class);
        when(actionQuery.requireRecognized(actorContext, ACTION))
                .thenReturn(new ActionReferenceQuery.RecognizedAction(ACTION, DECISION));
        RiskCaseAssociationService service = associationService(
                actionQuery, mock(ActionOutcomeReferenceQuery.class));

        assertThatThrownBy(() -> service.associateAction(actorContext,
                riskCase.snapshot().caseNumber().value(), ACTION.value(), "associate", 3))
                .isInstanceOf(RiskCaseException.class)
                .extracting(error -> ((RiskCaseException) error).getResultCode())
                .isEqualTo(ResultCode.RISK_CASE_INVARIANT_VIOLATION);

        verify(repository, never()).updateRoot(any(), anyLong());
        verifyNoInteractions(auditWriter);
    }

    @Test
    void outcomePertainingToAnotherActionIsRejectedBeforeWrites() {
        RiskCase riskCase = actionRequiredCase();
        ActionRef otherAction = new ActionRef("act-80000000-0000-4000-8000-000000000002");
        ActionOutcomeRef outcome =
                new ActionOutcomeRef("aoc-90000000-0000-4000-8000-000000000001");
        when(repository.findByCaseNumber(riskCase.snapshot().caseNumber()))
                .thenReturn(Optional.of(riskCase));
        when(repository.findEffectiveAction(riskCase.snapshot().id(), ACTION))
                .thenReturn(Optional.of(new RiskCaseRepository.EffectiveAction(
                        1, ACTION, DECISION, null)));
        ActionOutcomeReferenceQuery outcomeQuery = mock(ActionOutcomeReferenceQuery.class);
        when(outcomeQuery.requireRecognized(actorContext, outcome))
                .thenReturn(new ActionOutcomeReferenceQuery.RecognizedActionOutcome(
                        outcome, otherAction));
        RiskCaseAssociationService service = associationService(
                mock(ActionReferenceQuery.class), outcomeQuery);

        assertThatThrownBy(() -> service.recordActionOutcomeReference(
                actorContext, riskCase.snapshot().caseNumber().value(), ACTION.value(),
                outcome.value(), "outcome", 6))
                .isInstanceOf(RiskCaseException.class)
                .extracting(error -> ((RiskCaseException) error).getResultCode())
                .isEqualTo(ResultCode.RISK_CASE_INVARIANT_VIOLATION);

        verify(repository, never()).updateRoot(any(), anyLong());
        verifyNoInteractions(auditWriter);
    }

    @Test
    void accessAuditFailurePreventsDetailDisclosure() {
        RiskCase riskCase = persistedOpen();
        when(repository.findByCaseNumber(riskCase.snapshot().caseNumber()))
                .thenReturn(Optional.of(riskCase));
        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditWriter).append(any());
        RiskCaseQueryService service = new RiskCaseQueryService(
                authorizationGuard, repository, auditWriter, new RiskCaseAuditFactory(),
                metrics, fixedClock(), transactionManager());

        assertThatThrownBy(() -> service.detail(
                actorContext, riskCase.snapshot().caseNumber().value()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("audit unavailable");
    }

    private RiskCaseCreationService creationService(
            TradingAccountReferenceQuery subjectQuery,
            DecisionReferenceQuery decisionQuery) {
        return new RiskCaseCreationService(
                authorizationGuard, subjectQuery, decisionQuery, repository, auditWriter,
                () -> new CaseNumber("RC-a0000000-0000-4000-8000-000000000001"),
                new RiskCaseFingerprintFactory(), new RiskCaseAuditFactory(), metrics,
                fixedClock(), transactionManager());
    }

    private RiskCaseCommandService commandService() {
        return new RiskCaseCommandService(
                authorizationGuard, repository, auditWriter, new RiskCaseAuditFactory(),
                metrics, fixedClock(), transactionManager());
    }

    private RiskCaseAssociationService associationService(
            ActionReferenceQuery actionQuery,
            ActionOutcomeReferenceQuery outcomeQuery) {
        return new RiskCaseAssociationService(
                authorizationGuard, mock(EvidenceReferenceQuery.class),
                mock(DecisionReferenceQuery.class), actionQuery, outcomeQuery,
                repository, auditWriter, new RiskCaseAuditFactory(), metrics,
                fixedClock(), transactionManager());
    }

    private RiskCase actionRequiredCase() {
        RiskCase riskCase = reviewCase();
        riskCase.associateDecision(DECISION, ACTOR_REF, "decision", NOW, 3);
        riskCase.associateAction(true, ACTOR_REF, NOW, 4);
        riskCase.markActionRequired(true, ACTOR_REF, "required", NOW, 5);
        return riskCase;
    }

    private RiskCase reviewCase() {
        RiskCase riskCase = persistedOpen();
        riskCase.assign(ASSIGNEE_REF, ACTOR_REF, "assign", NOW, 1);
        riskCase.beginReview(ACTOR_REF, "begin", NOW, 2);
        return riskCase;
    }

    private RiskCase persistedOpen() {
        RiskCase riskCase = RiskCase.openManual(
                new CaseNumber("RC-a0000000-0000-4000-8000-000000000001"),
                new TradingAccountSubjectRef(SUBJECT), "manual",
                RiskCasePriority.NORMAL, ACTOR_REF, NOW);
        riskCase.markPersisted(new RiskCaseId(1));
        return riskCase;
    }

    private CreateRiskCaseCommand manualCommand() {
        return new CreateRiskCaseCommand(
                "MANUAL", "TRADING_ACCOUNT", SUBJECT.value(), "manual",
                "NORMAL", null, "idempotency-key-0001");
    }

    private ActorContext actorContext() {
        return new ActorContext(
                ACTOR_REF, ActorType.HUMAN,
                new ExternalPrincipalKey("issuer", "subject", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("b0000000-0000-4000-8000-000000000001"),
                "request-1", "00000000000000000000000000000001");
    }

    private Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
