package com.brokeros.risk.decision.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.domain.DecisionOperationType;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.decision.domain.DecisionSemanticFingerprint;

public record CompletedDecisionOperation(
        DecisionOperationId operationId,
        DecisionOperationType operationType,
        DecisionSemanticFingerprint fingerprint,
        DecisionRef decisionRef,
        DecisionOperationOutcome outcome,
        Instant occurredAt,
        DecisionRecord decisionRecord) {

    public CompletedDecisionOperation {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(decisionRecord, "decisionRecord must not be null");
        if (operationType != DecisionOperationType.RECORD
                || outcome != DecisionOperationOutcome.CREATED
                || !decisionRef.equals(decisionRecord.decisionRef())
                || !occurredAt.equals(decisionRecord.recordedAt())) {
            throw new IllegalArgumentException("completed decision operation is inconsistent");
        }
    }
}
