package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;

public record PriorityChangeRecord(
        RiskCaseId caseId,
        long caseVersion,
        RiskCasePriority previousPriority,
        RiskCasePriority newPriority,
        ActorRef changedBy,
        String reason,
        Instant occurredAt) {

    public PriorityChangeRecord {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(previousPriority, "previousPriority must not be null");
        Objects.requireNonNull(newPriority, "newPriority must not be null");
        Objects.requireNonNull(changedBy, "changedBy must not be null");
        reason = RiskCaseText.require(reason, 1000, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (previousPriority == newPriority) {
            throw new IllegalArgumentException("priority must change");
        }
    }
}
