package com.brokeros.risk.riskcase.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.domain.AssignmentChangeRecord;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.PriorityChangeRecord;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCaseDomainException;
import com.brokeros.risk.riskcase.domain.RiskCasePriority;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.riskcase.domain.TransitionRecord;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.Capability;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class RiskCaseCommandService {

    private final AuthorizationGuard authorizationGuard;
    private final RiskCaseRepository repository;
    private final AuditRecordWriter auditWriter;
    private final RiskCaseAuditFactory auditFactory;
    private final RiskCaseMetricsPort metrics;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public RiskCaseCommandService(
            AuthorizationGuard authorizationGuard,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory auditFactory,
            RiskCaseMetricsPort metrics,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.repository = Objects.requireNonNull(repository);
        this.auditWriter = Objects.requireNonNull(auditWriter);
        this.auditFactory = Objects.requireNonNull(auditFactory);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public RiskCaseSnapshot changeAssignment(
            ActorContext actorContext,
            String caseNumber,
            String assigneeRef,
            String reason,
            long expectedVersion) {
        return execute(actorContext, RiskCaseCapabilities.ASSIGN, caseNumber, expectedVersion,
                (riskCase, occurredAt) -> {
                    AssignmentChangeRecord record = assigneeRef == null
                            ? riskCase.unassign(actorContext.actorRef(), reason,
                                    occurredAt, expectedVersion)
                            : riskCase.assign(actorRef(assigneeRef), actorContext.actorRef(),
                                    reason, occurredAt, expectedVersion);
                    return new MutationPlan(
                            () -> repository.appendAssignment(record),
                            "RISK_CASE_ASSIGNED", "ACTOR", assigneeRef, reason);
                });
    }

    public RiskCaseSnapshot beginReview(
            ActorContext actorContext, String caseNumber, String reason, long expectedVersion) {
        return transition(actorContext, RiskCaseCapabilities.REVIEW, caseNumber,
                expectedVersion, "RISK_CASE_REVIEW_STARTED", reason,
                (riskCase, occurredAt) -> riskCase.beginReview(
                        actorContext.actorRef(), reason, occurredAt, expectedVersion));
    }

    public RiskCaseSnapshot markActionRequired(
            ActorContext actorContext, String caseNumber, String reason, long expectedVersion) {
        return execute(actorContext, RiskCaseCapabilities.REVIEW, caseNumber, expectedVersion,
                (riskCase, occurredAt) -> {
                    RiskCaseSnapshot before = riskCase.snapshot();
                    boolean hasAction = before.currentDecisionRef() != null
                            && repository.hasActionForDecision(
                                    before.id(), before.currentDecisionRef());
                    TransitionRecord record = riskCase.markActionRequired(
                            hasAction, actorContext.actorRef(), reason,
                            occurredAt, expectedVersion);
                    return new MutationPlan(
                            () -> repository.appendTransition(record),
                            "RISK_CASE_ACTION_REQUIRED", null, null, reason);
                });
    }

    public RiskCaseSnapshot returnToReview(
            ActorContext actorContext, String caseNumber, String reason, long expectedVersion) {
        return transition(actorContext, RiskCaseCapabilities.REVIEW, caseNumber,
                expectedVersion, "RISK_CASE_RETURNED_TO_REVIEW", reason,
                (riskCase, occurredAt) -> riskCase.returnToReview(
                        actorContext.actorRef(), reason, occurredAt, expectedVersion));
    }

    public RiskCaseSnapshot changePriority(
            ActorContext actorContext,
            String caseNumber,
            String priority,
            String reason,
            long expectedVersion) {
        return execute(actorContext, RiskCaseCapabilities.REVIEW, caseNumber, expectedVersion,
                (riskCase, occurredAt) -> {
                    PriorityChangeRecord record = riskCase.changePriority(
                            priority(priority), actorContext.actorRef(), reason,
                            occurredAt, expectedVersion);
                    return new MutationPlan(
                            () -> repository.appendPriority(record),
                            "RISK_CASE_PRIORITY_CHANGED", "PRIORITY", priority, reason);
                });
    }

    public RiskCaseSnapshot close(
            ActorContext actorContext, String caseNumber, String reason, long expectedVersion) {
        return transition(actorContext, RiskCaseCapabilities.CLOSE, caseNumber,
                expectedVersion, "RISK_CASE_CLOSED", reason,
                (riskCase, occurredAt) -> riskCase.close(
                        actorContext.actorRef(), reason, occurredAt, expectedVersion));
    }

    public RiskCaseSnapshot cancel(
            ActorContext actorContext,
            String caseNumber,
            String reason,
            String duplicateCaseNumber,
            long expectedVersion) {
        return execute(actorContext, RiskCaseCapabilities.CANCEL, caseNumber, expectedVersion,
                (riskCase, occurredAt) -> {
                    if (duplicateCaseNumber != null) {
                        CaseNumber duplicate = caseNumber(duplicateCaseNumber);
                        if (duplicate.equals(riskCase.snapshot().caseNumber())
                                || repository.findByCaseNumber(duplicate).isEmpty()) {
                            throw new RiskCaseException(
                                    ResultCode.RISK_CASE_INVARIANT_VIOLATION);
                        }
                    }
                    TransitionRecord record = riskCase.cancel(
                            actorContext.actorRef(), reason, occurredAt, expectedVersion);
                    return new MutationPlan(
                            () -> repository.appendTransition(record),
                            "RISK_CASE_CANCELLED", "RISK_CASE", duplicateCaseNumber, reason);
                });
    }

    public RiskCaseSnapshot resumeResolved(
            ActorContext actorContext,
            String caseNumber,
            String reason,
            String assigneeRef,
            long expectedVersion) {
        return reopen(actorContext, caseNumber, reason, assigneeRef, expectedVersion, false);
    }

    public RiskCaseSnapshot reopenClosed(
            ActorContext actorContext,
            String caseNumber,
            String reason,
            String assigneeRef,
            long expectedVersion) {
        return reopen(actorContext, caseNumber, reason, assigneeRef, expectedVersion, true);
    }

    private RiskCaseSnapshot reopen(
            ActorContext actorContext,
            String caseNumber,
            String reason,
            String assigneeRef,
            long expectedVersion,
            boolean closed) {
        return execute(actorContext, RiskCaseCapabilities.REOPEN, caseNumber, expectedVersion,
                (riskCase, occurredAt) -> {
                    ActorRef supplied = assigneeRef == null ? null : actorRef(assigneeRef);
                    RiskCaseSnapshot before = riskCase.snapshot();
                    TransitionRecord transition = closed
                            ? riskCase.reopenClosedCase(supplied, actorContext.actorRef(),
                                    reason, occurredAt, expectedVersion)
                            : riskCase.resumeResolvedCase(supplied, actorContext.actorRef(),
                                    reason, occurredAt, expectedVersion);
                    AssignmentChangeRecord assignmentRecord = supplied == null
                            || (before.assignment() != null
                            && supplied.equals(before.assignment().assignee()))
                            ? null
                            : new AssignmentChangeRecord(before.id(), transition.caseVersion(),
                                    before.assignment() == null
                                            ? null
                                            : before.assignment().assignee(),
                                    supplied, actorContext.actorRef(), reason, occurredAt);
                    return new MutationPlan(() -> {
                        repository.appendTransition(transition);
                        if (assignmentRecord != null) {
                            repository.appendAssignment(assignmentRecord);
                        }
                        repository.appendDecisionSelection(new com.brokeros.risk.riskcase.domain.DecisionSelectionRecord(
                                before.id(), transition.caseVersion(), before.currentDecisionRef(),
                                null, actorContext.actorRef(), reason, occurredAt));
                    }, closed ? "RISK_CASE_CLOSED_REOPENED"
                            : "RISK_CASE_RESOLUTION_REOPENED",
                            "ACTOR", assigneeRef, reason);
                });
    }

    private RiskCaseSnapshot transition(
            ActorContext actorContext,
            Capability capability,
            String caseNumber,
            long expectedVersion,
            String operationCode,
            String reason,
            BiFunction<RiskCase, Instant, TransitionRecord> transition) {
        return execute(actorContext, capability, caseNumber, expectedVersion,
                (riskCase, occurredAt) -> {
                    TransitionRecord record = transition.apply(riskCase, occurredAt);
                    return new MutationPlan(
                            () -> repository.appendTransition(record),
                            operationCode, null, null, reason);
                });
    }

    private RiskCaseSnapshot execute(
            ActorContext actorContext,
            Capability capability,
            String rawCaseNumber,
            long expectedVersion,
            BiFunction<RiskCase, Instant, MutationPlan> operation) {
        long started = System.nanoTime();
        requireAuthorized(actorContext, capability);
        CaseNumber parsedCaseNumber = caseNumber(rawCaseNumber);
        try {
            RiskCaseSnapshot result = transactionTemplate.execute(status -> {
                RiskCase riskCase = repository.findByCaseNumber(parsedCaseNumber)
                        .orElseThrow(() -> new RiskCaseException(ResultCode.RISK_CASE_NOT_FOUND));
                RiskCaseSnapshot before = riskCase.snapshot();
                Instant occurredAt = clock.instant();
                MutationPlan plan = operation.apply(riskCase, occurredAt);
                RiskCaseSnapshot after = riskCase.snapshot();
                if (repository.updateRoot(after, expectedVersion) != 1) {
                    throw new RiskCaseException(ResultCode.RISK_CASE_VERSION_CONFLICT);
                }
                plan.historyWrite().run();
                auditWriter.append(auditFactory.material(
                        before, after, actorContext, occurredAt, plan.operationCode(),
                        plan.affectedRefType(), plan.affectedRef(), plan.reason()));
                return after;
            });
            if (result == null) {
                throw new IllegalStateException("risk case command returned no result");
            }
            metrics.recordSuccess(RiskCaseMetricOperation.COMMAND);
            return result;
        } catch (RiskCaseDomainException exception) {
            if (exception.error() == com.brokeros.risk.riskcase.domain.RiskCaseDomainError.VERSION_CONFLICT) {
                metrics.recordConflict("VERSION");
            }
            throw RiskCaseErrors.translate(exception);
        } finally {
            metrics.recordDuration(RiskCaseMetricOperation.COMMAND,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private void requireAuthorized(ActorContext actorContext, Capability capability) {
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

    private ActorRef actorRef(String value) {
        try {
            return new ActorRef(value);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private RiskCasePriority priority(String value) {
        try {
            return RiskCasePriority.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private record MutationPlan(
            Runnable historyWrite,
            String operationCode,
            String affectedRefType,
            String affectedRef,
            String reason) {
    }
}
