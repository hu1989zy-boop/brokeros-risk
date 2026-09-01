package com.brokeros.risk.actionoutcome.domain;

import java.time.Instant;
import java.util.Objects;

public record CompletedActionOutcomeOperation(
        ActionOutcomeOperationId operationId,
        ActionOutcomeOperationType operationType,
        ActionOutcomeSemanticFingerprint fingerprint,
        ActionOutcomeRef actionOutcomeRef,
        ActionOutcomeOperationOutcome outcome,
        Instant occurredAt,
        ActionOutcomeRecord actionOutcomeRecord) {

    public CompletedActionOutcomeOperation {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(actionOutcomeRef, "actionOutcomeRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(actionOutcomeRecord, "actionOutcomeRecord must not be null");
        if (operationType != ActionOutcomeOperationType.RECORD
                || outcome != ActionOutcomeOperationOutcome.CREATED
                || !actionOutcomeRef.equals(actionOutcomeRecord.actionOutcomeRef())
                || !occurredAt.equals(actionOutcomeRecord.recordedAt())) {
            throw new IllegalArgumentException(
                    "completed action outcome operation is inconsistent");
        }
    }
}
