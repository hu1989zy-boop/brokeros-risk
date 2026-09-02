package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public final class RiskCase {

    private RiskCaseId id;
    private final CaseNumber caseNumber;
    private final TradingAccountSubjectRef subjectRef;
    private final CaseIntakeSource intakeSource;
    private final String intakeSummary;
    private RiskCaseStatus status;
    private RiskCasePriority priority;
    private Assignment assignment;
    private DecisionRef currentDecisionRef;
    private ResolutionCycleNumber currentCycle;
    private final ActorRef createdBy;
    private final Instant createdAt;
    private ActorRef updatedBy;
    private Instant updatedAt;
    private long version;

    private RiskCase(
            RiskCaseId id,
            CaseNumber caseNumber,
            TradingAccountSubjectRef subjectRef,
            CaseIntakeSource intakeSource,
            String intakeSummary,
            RiskCaseStatus status,
            RiskCasePriority priority,
            Assignment assignment,
            DecisionRef currentDecisionRef,
            ResolutionCycleNumber currentCycle,
            ActorRef createdBy,
            Instant createdAt,
            ActorRef updatedBy,
            Instant updatedAt,
            long version) {
        this.id = id;
        this.caseNumber = Objects.requireNonNull(caseNumber);
        this.subjectRef = Objects.requireNonNull(subjectRef);
        this.intakeSource = Objects.requireNonNull(intakeSource);
        this.intakeSummary = RiskCaseText.require(intakeSummary, 1000, "intakeSummary");
        this.status = Objects.requireNonNull(status);
        this.priority = Objects.requireNonNull(priority);
        this.assignment = assignment;
        this.currentDecisionRef = currentDecisionRef;
        this.currentCycle = Objects.requireNonNull(currentCycle);
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedBy = Objects.requireNonNull(updatedBy);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        this.version = version;
        validateCurrentState();
    }

    public static RiskCase openManual(
            CaseNumber caseNumber,
            TradingAccountSubjectRef subjectRef,
            String intakeSummary,
            RiskCasePriority priority,
            ActorRef actor,
            Instant occurredAt) {
        return open(caseNumber, subjectRef, CaseIntakeSource.MANUAL, intakeSummary,
                priority, null, actor, occurredAt);
    }

    public static RiskCase openDecisionDriven(
            CaseNumber caseNumber,
            TradingAccountSubjectRef subjectRef,
            String intakeSummary,
            RiskCasePriority priority,
            DecisionRef decisionRef,
            ActorRef actor,
            Instant occurredAt) {
        return open(caseNumber, subjectRef, CaseIntakeSource.DECISION_DRIVEN,
                intakeSummary, priority, Objects.requireNonNull(decisionRef), actor, occurredAt);
    }

    private static RiskCase open(
            CaseNumber caseNumber,
            TradingAccountSubjectRef subjectRef,
            CaseIntakeSource intakeSource,
            String intakeSummary,
            RiskCasePriority priority,
            DecisionRef decisionRef,
            ActorRef actor,
            Instant occurredAt) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        return new RiskCase(null, caseNumber, subjectRef, intakeSource, intakeSummary,
                RiskCaseStatus.OPEN, priority, null, decisionRef,
                new ResolutionCycleNumber(1), actor, occurredAt, actor, occurredAt, 1);
    }

    public static RiskCase rehydrate(RiskCaseSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (snapshot.id() == null) {
            throw new IllegalArgumentException("persisted snapshot requires id");
        }
        return new RiskCase(
                snapshot.id(), snapshot.caseNumber(), snapshot.subjectRef(),
                snapshot.intakeSource(), snapshot.intakeSummary(), snapshot.status(),
                snapshot.priority(), snapshot.assignment(), snapshot.currentDecisionRef(),
                snapshot.currentCycle(), snapshot.createdBy(), snapshot.createdAt(),
                snapshot.updatedBy(), snapshot.updatedAt(), snapshot.version());
    }

    public void markPersisted(RiskCaseId persistedId) {
        if (id != null) {
            throw new IllegalStateException("risk case identity is immutable");
        }
        id = Objects.requireNonNull(persistedId, "persistedId must not be null");
    }

    public AssignmentChangeRecord assign(
            ActorRef assignee,
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN, RiskCaseStatus.IN_REVIEW, RiskCaseStatus.ACTION_REQUIRED);
        requireReason(reason);
        Objects.requireNonNull(assignee, "assignee must not be null");
        ActorRef previous = assignment == null ? null : assignment.assignee();
        if (assignee.equals(previous)) {
            invariant("assignee must change");
        }
        assignment = new Assignment(assignee, actor, occurredAt);
        touch(actor, occurredAt);
        return new AssignmentChangeRecord(requireId(), version, previous, assignee,
                actor, reason, occurredAt);
    }

    public AssignmentChangeRecord unassign(
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN);
        requireReason(reason);
        if (assignment == null) {
            invariant("case is not assigned");
        }
        ActorRef previous = assignment.assignee();
        assignment = null;
        touch(actor, occurredAt);
        return new AssignmentChangeRecord(requireId(), version, previous, null,
                actor, reason, occurredAt);
    }

    public TransitionRecord beginReview(
            ActorRef actor, String reason, Instant occurredAt, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN);
        requireReason(reason);
        requireAssignment();
        return transition(RiskCaseStatus.IN_REVIEW, RiskCaseTransitionOperation.BEGIN_REVIEW,
                actor, reason, occurredAt);
    }

    public void associateEvidence(ActorRef actor, Instant occurredAt, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN, RiskCaseStatus.IN_REVIEW, RiskCaseStatus.ACTION_REQUIRED);
        touch(actor, occurredAt);
    }

    public void changeEvidenceDisposition(
            ActorRef actor, Instant occurredAt, long expectedVersion) {
        associateEvidence(actor, occurredAt, expectedVersion);
    }

    public DecisionSelectionRecord associateDecision(
            DecisionRef decisionRef,
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN, RiskCaseStatus.IN_REVIEW, RiskCaseStatus.ACTION_REQUIRED);
        requireReason(reason);
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        DecisionRef previous = currentDecisionRef;
        if (decisionRef.equals(previous)) {
            invariant("decision is already current");
        }
        currentDecisionRef = decisionRef;
        touch(actor, occurredAt);
        return new DecisionSelectionRecord(requireId(), version, previous, decisionRef,
                actor, reason, occurredAt);
    }

    public DecisionSelectionRecord selectCurrentDecision(
            DecisionRef decisionRef,
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        return associateDecision(decisionRef, actor, reason, occurredAt, expectedVersion);
    }

    public void associateAction(
            boolean originatingDecisionAssociated,
            ActorRef actor,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.IN_REVIEW, RiskCaseStatus.ACTION_REQUIRED);
        if (!originatingDecisionAssociated) {
            invariant("action must originate from an associated decision");
        }
        touch(actor, occurredAt);
    }

    public void recordActionOutcomeReference(
            boolean actionAssociated,
            boolean outcomePertainsToAction,
            ActorRef actor,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.ACTION_REQUIRED);
        if (!actionAssociated || !outcomePertainsToAction) {
            invariant("outcome must pertain to an associated action");
        }
        touch(actor, occurredAt);
    }

    public TransitionRecord markActionRequired(
            boolean hasActionForCurrentDecision,
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.IN_REVIEW);
        requireReason(reason);
        requireAssignment();
        if (currentDecisionRef == null || !hasActionForCurrentDecision) {
            invariant("current decision and associated action are required");
        }
        return transition(RiskCaseStatus.ACTION_REQUIRED,
                RiskCaseTransitionOperation.MARK_ACTION_REQUIRED,
                actor, reason, occurredAt);
    }

    public TransitionRecord returnToReview(
            ActorRef actor, String reason, Instant occurredAt, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.ACTION_REQUIRED);
        requireReason(reason);
        return transition(RiskCaseStatus.IN_REVIEW,
                RiskCaseTransitionOperation.RETURN_TO_REVIEW,
                actor, reason, occurredAt);
    }

    public PriorityChangeRecord changePriority(
            RiskCasePriority newPriority,
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN, RiskCaseStatus.IN_REVIEW,
                RiskCaseStatus.ACTION_REQUIRED, RiskCaseStatus.RESOLVED);
        requireReason(reason);
        Objects.requireNonNull(newPriority, "newPriority must not be null");
        RiskCasePriority previous = priority;
        if (previous == newPriority) {
            invariant("priority must change");
        }
        priority = newPriority;
        touch(actor, occurredAt);
        return new PriorityChangeRecord(requireId(), version, previous, newPriority,
                actor, reason, occurredAt);
    }

    public void addInvestigationNote(
            ActorRef actor, Instant occurredAt, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN, RiskCaseStatus.IN_REVIEW,
                RiskCaseStatus.ACTION_REQUIRED, RiskCaseStatus.RESOLVED);
        touch(actor, occurredAt);
    }

    public void correctInvestigationNote(
            ActorRef actor, Instant occurredAt, long expectedVersion) {
        addInvestigationNote(actor, occurredAt, expectedVersion);
    }

    public TransitionRecord resolve(
            boolean everyRequiredActionHasOutcome,
            ActorRef actor,
            String summary,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.IN_REVIEW, RiskCaseStatus.ACTION_REQUIRED);
        RiskCaseText.require(summary, 2000, "resolution summary");
        if (currentDecisionRef == null) {
            invariant("current decision is required for resolution");
        }
        if (status == RiskCaseStatus.ACTION_REQUIRED && !everyRequiredActionHasOutcome) {
            invariant("every required action must have an outcome reference");
        }
        return transition(RiskCaseStatus.RESOLVED, RiskCaseTransitionOperation.RESOLVE,
                actor, summary, occurredAt);
    }

    public TransitionRecord close(
            ActorRef actor, String reason, Instant occurredAt, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.RESOLVED);
        requireReason(reason);
        return transition(RiskCaseStatus.CLOSED, RiskCaseTransitionOperation.CLOSE,
                actor, reason, occurredAt);
    }

    public TransitionRecord cancel(
            ActorRef actor, String reason, Instant occurredAt, long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.OPEN, RiskCaseStatus.IN_REVIEW, RiskCaseStatus.ACTION_REQUIRED);
        requireReason(reason);
        return transition(RiskCaseStatus.CANCELLED, RiskCaseTransitionOperation.CANCEL,
                actor, reason, occurredAt);
    }

    public TransitionRecord resumeResolvedCase(
            ActorRef suppliedAssignee,
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.RESOLVED);
        return reopen(RiskCaseTransitionOperation.RESUME_RESOLVED,
                suppliedAssignee, actor, reason, occurredAt);
    }

    public TransitionRecord reopenClosedCase(
            ActorRef suppliedAssignee,
            ActorRef actor,
            String reason,
            Instant occurredAt,
            long expectedVersion) {
        requireVersion(expectedVersion);
        requireStatus(RiskCaseStatus.CLOSED);
        return reopen(RiskCaseTransitionOperation.REOPEN_CLOSED,
                suppliedAssignee, actor, reason, occurredAt);
    }

    private TransitionRecord reopen(
            RiskCaseTransitionOperation operation,
            ActorRef suppliedAssignee,
            ActorRef actor,
            String reason,
            Instant occurredAt) {
        requireReason(reason);
        if (suppliedAssignee != null) {
            assignment = new Assignment(suppliedAssignee, actor, occurredAt);
        }
        requireAssignment();
        RiskCaseStatus from = status;
        currentCycle = currentCycle.next();
        currentDecisionRef = null;
        status = RiskCaseStatus.IN_REVIEW;
        touch(actor, occurredAt);
        return new TransitionRecord(requireId(), version, currentCycle, operation,
                from, status, reason, actor, occurredAt);
    }

    private TransitionRecord transition(
            RiskCaseStatus target,
            RiskCaseTransitionOperation operation,
            ActorRef actor,
            String reason,
            Instant occurredAt) {
        RiskCaseStatus from = status;
        status = target;
        touch(actor, occurredAt);
        return new TransitionRecord(requireId(), version, currentCycle, operation,
                from, target, reason, actor, occurredAt);
    }

    private void touch(ActorRef actor, Instant occurredAt) {
        updatedBy = Objects.requireNonNull(actor, "actor must not be null");
        updatedAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        version = Math.addExact(version, 1);
        validateCurrentState();
    }

    private void requireVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new RiskCaseDomainException(
                    RiskCaseDomainError.VERSION_CONFLICT, "risk case version is stale");
        }
    }

    private void requireStatus(RiskCaseStatus... allowed) {
        for (RiskCaseStatus candidate : allowed) {
            if (candidate == status) {
                return;
            }
        }
        throw new RiskCaseDomainException(
                RiskCaseDomainError.INVALID_TRANSITION,
                "operation is not permitted from " + status);
    }

    private void requireAssignment() {
        if (assignment == null) {
            invariant("assignment is required");
        }
    }

    private void requireReason(String reason) {
        try {
            RiskCaseText.require(reason, 1000, "reason");
        } catch (IllegalArgumentException exception) {
            throw new RiskCaseDomainException(
                    RiskCaseDomainError.INVARIANT_VIOLATION, exception.getMessage());
        }
    }

    private void validateCurrentState() {
        if ((status == RiskCaseStatus.IN_REVIEW || status == RiskCaseStatus.ACTION_REQUIRED)
                && assignment == null) {
            invariant("active review state requires assignment");
        }
        if ((status == RiskCaseStatus.ACTION_REQUIRED || status == RiskCaseStatus.RESOLVED
                || status == RiskCaseStatus.CLOSED) && currentDecisionRef == null) {
            invariant("current state requires a decision");
        }
    }

    private void invariant(String message) {
        throw new RiskCaseDomainException(RiskCaseDomainError.INVARIANT_VIOLATION, message);
    }

    private RiskCaseId requireId() {
        if (id == null) {
            throw new IllegalStateException("risk case is not persisted");
        }
        return id;
    }

    public RiskCaseSnapshot snapshot() {
        return new RiskCaseSnapshot(id, caseNumber, subjectRef, intakeSource,
                intakeSummary, status, priority, assignment, currentDecisionRef,
                currentCycle, createdBy, createdAt, updatedBy, updatedAt, version);
    }
}
