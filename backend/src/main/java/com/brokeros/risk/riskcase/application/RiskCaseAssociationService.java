package com.brokeros.risk.riskcase.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.application.port.ActionOutcomeReferenceQuery;
import com.brokeros.risk.riskcase.application.port.ActionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.DecisionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.EvidenceReferenceQuery;
import com.brokeros.risk.riskcase.application.port.RiskCaseConflictKind;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCasePersistenceConflictException;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.domain.ActionAssociationEvent;
import com.brokeros.risk.riskcase.domain.ActionAssociationEventType;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.DecisionAssociation;
import com.brokeros.risk.riskcase.domain.DecisionSelectionRecord;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEvent;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEventRef;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEventType;
import com.brokeros.risk.riskcase.domain.InvestigationNote;
import com.brokeros.risk.riskcase.domain.InvestigationNoteRef;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCaseDomainException;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.riskcase.domain.RiskCaseText;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class RiskCaseAssociationService {

    private final AuthorizationGuard authorizationGuard;
    private final EvidenceReferenceQuery evidenceQuery;
    private final DecisionReferenceQuery decisionQuery;
    private final ActionReferenceQuery actionQuery;
    private final ActionOutcomeReferenceQuery outcomeQuery;
    private final RiskCaseRepository repository;
    private final AuditRecordWriter auditWriter;
    private final RiskCaseAuditFactory auditFactory;
    private final RiskCaseMetricsPort metrics;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public RiskCaseAssociationService(
            AuthorizationGuard authorizationGuard,
            EvidenceReferenceQuery evidenceQuery,
            DecisionReferenceQuery decisionQuery,
            ActionReferenceQuery actionQuery,
            ActionOutcomeReferenceQuery outcomeQuery,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory auditFactory,
            RiskCaseMetricsPort metrics,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.evidenceQuery = Objects.requireNonNull(evidenceQuery);
        this.decisionQuery = Objects.requireNonNull(decisionQuery);
        this.actionQuery = Objects.requireNonNull(actionQuery);
        this.outcomeQuery = Objects.requireNonNull(outcomeQuery);
        this.repository = Objects.requireNonNull(repository);
        this.auditWriter = Objects.requireNonNull(auditWriter);
        this.auditFactory = Objects.requireNonNull(auditFactory);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public EvidenceAssociationEvent associateEvidence(
            ActorContext actorContext,
            String caseNumber,
            String evidenceRef,
            String reason,
            String source,
            long expectedVersion) {
        EvidenceRef parsedEvidence = evidenceRef(evidenceRef);
        return execute(actorContext, caseNumber, (riskCase, before, occurredAt) -> {
            evidenceQuery.requireRecognized(actorContext, parsedEvidence);
            if (repository.findEffectiveEvidence(before.id(), parsedEvidence).isPresent()) {
                throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
            }
            riskCase.associateEvidence(actorContext.actorRef(), occurredAt, expectedVersion);
            RiskCaseSnapshot after = riskCase.snapshot();
            reserve(after, expectedVersion);
            EvidenceAssociationEvent event = repository.appendEvidence(
                    new EvidenceAssociationEvent(null, eventRef(), after.id(), after.version(),
                            EvidenceAssociationEventType.ATTACHED, parsedEvidence, null, null,
                            RiskCaseText.require(reason, 1000, "reason"),
                            RiskCaseText.require(source, 64, "source"),
                            actorContext.actorRef(), occurredAt));
            audit(after, before, actorContext, occurredAt,
                    "RISK_CASE_EVIDENCE_ASSOCIATED", "EVIDENCE", evidenceRef, reason);
            return event;
        });
    }

    public EvidenceAssociationEvent changeEvidenceDisposition(
            ActorContext actorContext,
            String caseNumber,
            String priorEventRef,
            String disposition,
            String replacementEvidenceRef,
            String reason,
            String source,
            long expectedVersion) {
        EvidenceAssociationEventType eventType = evidenceEventType(disposition);
        if (eventType == EvidenceAssociationEventType.ATTACHED) {
            throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
        }
        EvidenceRef replacement = replacementEvidenceRef == null
                ? null
                : evidenceRef(replacementEvidenceRef);
        return execute(actorContext, caseNumber, (riskCase, before, occurredAt) -> {
            EvidenceAssociationEvent prior = repository.findEvidenceEvent(
                            before.id(), evidenceEventRef(priorEventRef))
                    .orElseThrow(() -> new RiskCaseException(
                            ResultCode.RISK_CASE_REFERENCE_NOT_FOUND));
            if (repository.evidenceEventHasDisposition(prior.id())) {
                throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
            }
            if (eventType == EvidenceAssociationEventType.SUPERSEDED) {
                if (replacement == null) {
                    throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
                }
                evidenceQuery.requireRecognized(actorContext, replacement);
            } else if (replacement != null) {
                throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
            }
            riskCase.changeEvidenceDisposition(
                    actorContext.actorRef(), occurredAt, expectedVersion);
            RiskCaseSnapshot after = riskCase.snapshot();
            reserve(after, expectedVersion);
            EvidenceAssociationEvent event = repository.appendEvidence(
                    new EvidenceAssociationEvent(null, eventRef(), after.id(), after.version(),
                            eventType, prior.evidenceRef(), prior.id(), replacement,
                            RiskCaseText.require(reason, 1000, "reason"),
                            RiskCaseText.require(source, 64, "source"),
                            actorContext.actorRef(), occurredAt));
            audit(after, before, actorContext, occurredAt,
                    "RISK_CASE_EVIDENCE_" + eventType.name(), "EVIDENCE",
                    prior.evidenceRef().value(), reason);
            return event;
        });
    }

    public RiskCaseSnapshot associateDecision(
            ActorContext actorContext,
            String caseNumber,
            String decisionRef,
            String reason,
            long expectedVersion) {
        DecisionRef parsedDecision = decisionRef(decisionRef);
        try {
            return execute(actorContext, caseNumber, (riskCase, before, occurredAt) -> {
                decisionQuery.requireRecognized(actorContext, parsedDecision);
                if (repository.isDecisionAssociated(before.id(), parsedDecision)) {
                    throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
                }
                DecisionSelectionRecord selection = riskCase.associateDecision(
                        parsedDecision, actorContext.actorRef(), reason,
                        occurredAt, expectedVersion);
                RiskCaseSnapshot after = riskCase.snapshot();
                reserve(after, expectedVersion);
                repository.appendDecisionAssociation(new DecisionAssociation(
                        null, after.id(), after.version(), parsedDecision,
                        actorContext.actorRef(), RiskCaseText.require(reason, 1000, "reason"),
                        occurredAt));
                repository.appendDecisionSelection(selection);
                audit(after, before, actorContext, occurredAt,
                        "RISK_CASE_DECISION_ASSOCIATED", "DECISION", decisionRef, reason);
                return after;
            });
        } catch (RiskCasePersistenceConflictException exception) {
            if (exception.kind() == RiskCaseConflictKind.PRIMARY_DECISION) {
                metrics.recordConflict("PRIMARY_DECISION");
                throw new RiskCaseException(
                        ResultCode.RISK_CASE_PRIMARY_DECISION_CONFLICT, exception);
            }
            throw exception;
        }
    }

    public RiskCaseSnapshot selectCurrentDecision(
            ActorContext actorContext,
            String caseNumber,
            String decisionRef,
            String reason,
            long expectedVersion) {
        DecisionRef parsedDecision = decisionRef(decisionRef);
        return execute(actorContext, caseNumber, (riskCase, before, occurredAt) -> {
            decisionQuery.requireRecognized(actorContext, parsedDecision);
            if (!repository.isDecisionAssociated(before.id(), parsedDecision)) {
                throw new RiskCaseException(ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
            }
            DecisionSelectionRecord selection = riskCase.selectCurrentDecision(
                    parsedDecision, actorContext.actorRef(), reason,
                    occurredAt, expectedVersion);
            RiskCaseSnapshot after = riskCase.snapshot();
            reserve(after, expectedVersion);
            repository.appendDecisionSelection(selection);
            audit(after, before, actorContext, occurredAt,
                    "RISK_CASE_DECISION_SELECTED", "DECISION", decisionRef, reason);
            return after;
        });
    }

    public ActionAssociationEvent associateAction(
            ActorContext actorContext,
            String caseNumber,
            String actionRef,
            String reason,
            long expectedVersion) {
        ActionRef parsedAction = actionRef(actionRef);
        return execute(actorContext, caseNumber, (riskCase, before, occurredAt) -> {
            ActionReferenceQuery.RecognizedAction recognized =
                    actionQuery.requireRecognized(actorContext, parsedAction);
            boolean originAssociated = repository.isDecisionAssociated(
                    before.id(), recognized.decisionRef());
            if (repository.findEffectiveAction(before.id(), parsedAction).isPresent()) {
                throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
            }
            riskCase.associateAction(originAssociated, actorContext.actorRef(),
                    occurredAt, expectedVersion);
            RiskCaseSnapshot after = riskCase.snapshot();
            reserve(after, expectedVersion);
            ActionAssociationEvent event = repository.appendAction(
                    new ActionAssociationEvent(null, after.id(), after.version(),
                            ActionAssociationEventType.ACTION_ASSOCIATED, parsedAction,
                            recognized.decisionRef(), null, null,
                            RiskCaseText.require(reason, 1000, "reason"),
                            actorContext.actorRef(), occurredAt));
            audit(after, before, actorContext, occurredAt,
                    "RISK_CASE_ACTION_ASSOCIATED", "ACTION", actionRef, reason);
            return event;
        });
    }

    public ActionAssociationEvent recordActionOutcomeReference(
            ActorContext actorContext,
            String caseNumber,
            String actionRef,
            String outcomeRef,
            String reason,
            long expectedVersion) {
        ActionRef parsedAction = actionRef(actionRef);
        ActionOutcomeRef parsedOutcome = actionOutcomeRef(outcomeRef);
        return execute(actorContext, caseNumber, (riskCase, before, occurredAt) -> {
            ActionOutcomeReferenceQuery.RecognizedActionOutcome recognized =
                    outcomeQuery.requireRecognized(actorContext, parsedOutcome);
            RiskCaseRepository.EffectiveAction action = repository
                    .findEffectiveAction(before.id(), parsedAction)
                    .orElse(null);
            boolean pertains = recognized.actionRef().equals(parsedAction);
            if (action != null && action.outcomeRef() != null) {
                throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
            }
            riskCase.recordActionOutcomeReference(action != null, pertains,
                    actorContext.actorRef(), occurredAt, expectedVersion);
            RiskCaseSnapshot after = riskCase.snapshot();
            reserve(after, expectedVersion);
            ActionAssociationEvent event = repository.appendAction(
                    new ActionAssociationEvent(null, after.id(), after.version(),
                            ActionAssociationEventType.OUTCOME_REFERENCED, parsedAction,
                            action.decisionRef(), parsedOutcome, action.eventId(),
                            RiskCaseText.require(reason, 1000, "reason"),
                            actorContext.actorRef(), occurredAt));
            audit(after, before, actorContext, occurredAt,
                    "RISK_CASE_ACTION_OUTCOME_REFERENCED", "ACTION_OUTCOME",
                    outcomeRef, reason);
            return event;
        });
    }

    public InvestigationNote addNote(
            ActorContext actorContext,
            String caseNumber,
            String content,
            long expectedVersion) {
        return note(actorContext, caseNumber, null, content, expectedVersion);
    }

    public InvestigationNote correctNote(
            ActorContext actorContext,
            String caseNumber,
            String priorNoteRef,
            String content,
            long expectedVersion) {
        return note(actorContext, caseNumber, priorNoteRef, content, expectedVersion);
    }

    private InvestigationNote note(
            ActorContext actorContext,
            String caseNumber,
            String priorNoteRef,
            String content,
            long expectedVersion) {
        requireAuthorized(actorContext, RiskCaseCapabilities.NOTE);
        InvestigationNoteRef priorRef = priorNoteRef == null
                ? null
                : noteRef(priorNoteRef);
        return executeAuthorized(actorContext, caseNumber, (riskCase, before, occurredAt) -> {
            InvestigationNote prior = priorRef == null
                    ? null
                    : repository.findNote(before.id(), priorRef)
                            .orElseThrow(() -> new RiskCaseException(
                                    ResultCode.RISK_CASE_REFERENCE_NOT_FOUND));
            if (prior != null && repository.noteHasCorrection(prior.id())) {
                throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
            }
            if (prior == null) {
                riskCase.addInvestigationNote(
                        actorContext.actorRef(), occurredAt, expectedVersion);
            } else {
                riskCase.correctInvestigationNote(
                        actorContext.actorRef(), occurredAt, expectedVersion);
            }
            RiskCaseSnapshot after = riskCase.snapshot();
            reserve(after, expectedVersion);
            InvestigationNote note = repository.appendNote(new InvestigationNote(
                    null, noteRef(), after.id(), after.version(),
                    RiskCaseText.require(content, 4000, "note content"),
                    prior == null ? null : prior.id(), actorContext.actorRef(), occurredAt));
            audit(after, before, actorContext, occurredAt,
                    prior == null ? "RISK_CASE_NOTE_ADDED" : "RISK_CASE_NOTE_CORRECTED",
                    "NOTE", note.noteRef().value(),
                    prior == null ? "investigation note added" : "investigation note corrected");
            return note;
        });
    }

    private <T> T execute(
            ActorContext actorContext,
            String rawCaseNumber,
            AssociationWork<T> work) {
        requireAuthorized(actorContext, RiskCaseCapabilities.ASSOCIATE);
        return executeAuthorized(actorContext, rawCaseNumber, work);
    }

    private <T> T executeAuthorized(
            ActorContext actorContext,
            String rawCaseNumber,
            AssociationWork<T> work) {
        long started = System.nanoTime();
        try {
            CaseNumber caseNumber = caseNumber(rawCaseNumber);
            T result = transactionTemplate.execute(status -> {
                RiskCase riskCase = repository.findByCaseNumber(caseNumber)
                        .orElseThrow(() -> new RiskCaseException(ResultCode.RISK_CASE_NOT_FOUND));
                RiskCaseSnapshot before = riskCase.snapshot();
                return work.apply(riskCase, before, clock.instant());
            });
            if (result == null) {
                throw new IllegalStateException("association transaction returned no result");
            }
            metrics.recordSuccess(RiskCaseMetricOperation.ASSOCIATE);
            return result;
        } catch (RiskCaseDomainException exception) {
            throw RiskCaseErrors.translate(exception);
        } finally {
            metrics.recordDuration(RiskCaseMetricOperation.ASSOCIATE,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private void reserve(RiskCaseSnapshot after, long expectedVersion) {
        if (repository.updateRoot(after, expectedVersion) != 1) {
            metrics.recordConflict("VERSION");
            throw new RiskCaseException(ResultCode.RISK_CASE_VERSION_CONFLICT);
        }
    }

    private void audit(
            RiskCaseSnapshot after,
            RiskCaseSnapshot before,
            ActorContext actorContext,
            Instant occurredAt,
            String operation,
            String affectedType,
            String affectedRef,
            String reason) {
        auditWriter.append(auditFactory.material(before, after, actorContext, occurredAt,
                operation, affectedType, affectedRef, reason));
    }

    private void requireAuthorized(ActorContext actorContext,
            com.brokeros.risk.security.domain.Capability capability) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(actorContext, capability);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(capability);
            throw exception;
        }
    }

    private CaseNumber caseNumber(String value) {
        try {
            return new CaseNumber(value);
        } catch (IllegalArgumentException exception) {
            throw new RiskCaseException(ResultCode.RISK_CASE_NOT_FOUND, exception);
        }
    }

    private EvidenceRef evidenceRef(String value) {
        try {
            return new EvidenceRef(value);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private DecisionRef decisionRef(String value) {
        try {
            return new DecisionRef(value);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private ActionRef actionRef(String value) {
        try {
            return new ActionRef(value);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private ActionOutcomeRef actionOutcomeRef(String value) {
        try {
            return new ActionOutcomeRef(value);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private EvidenceAssociationEventType evidenceEventType(String value) {
        try {
            return EvidenceAssociationEventType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private EvidenceAssociationEventRef evidenceEventRef(String value) {
        try {
            return new EvidenceAssociationEventRef(value);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private EvidenceAssociationEventRef eventRef() {
        return new EvidenceAssociationEventRef(UUID.randomUUID().toString());
    }

    private InvestigationNoteRef noteRef(String value) {
        try {
            return new InvestigationNoteRef(value);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private InvestigationNoteRef noteRef() {
        return new InvestigationNoteRef(UUID.randomUUID().toString());
    }

    @FunctionalInterface
    private interface AssociationWork<T> {
        T apply(RiskCase riskCase, RiskCaseSnapshot before, Instant occurredAt);
    }
}
