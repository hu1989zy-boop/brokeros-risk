package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;

public record AssignmentChangeRecord(
        RiskCaseId caseId,
        long caseVersion,
        ActorRef previousAssignee,
        ActorRef newAssignee,
        ActorRef assignedBy,
        String reason,
        Instant occurredAt) {

    public AssignmentChangeRecord {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(assignedBy, "assignedBy must not be null");
        reason = RiskCaseText.require(reason, 1000, "reason");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (previousAssignee == null && newAssignee == null) {
            throw new IllegalArgumentException("assignment change must retain a side");
        }
    }
}
