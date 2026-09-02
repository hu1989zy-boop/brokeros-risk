package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record RiskCaseSnapshot(
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

    public RiskCaseSnapshot {
        Objects.requireNonNull(caseNumber, "caseNumber must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(intakeSource, "intakeSource must not be null");
        intakeSummary = RiskCaseText.require(intakeSummary, 1000, "intakeSummary");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(currentCycle, "currentCycle must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedBy, "updatedBy must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }
}
