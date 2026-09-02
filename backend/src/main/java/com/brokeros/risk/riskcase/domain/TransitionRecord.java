package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;

public record TransitionRecord(
        RiskCaseId caseId,
        long caseVersion,
        ResolutionCycleNumber cycle,
        RiskCaseTransitionOperation operation,
        RiskCaseStatus fromStatus,
        RiskCaseStatus toStatus,
        String reason,
        ActorRef actorRef,
        Instant occurredAt) {

    public TransitionRecord {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(cycle, "cycle must not be null");
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(toStatus, "toStatus must not be null");
        reason = RiskCaseText.require(reason, 1000, "reason");
        Objects.requireNonNull(actorRef, "actorRef must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (caseVersion < 1) {
            throw new IllegalArgumentException("caseVersion must be positive");
        }
    }
}
