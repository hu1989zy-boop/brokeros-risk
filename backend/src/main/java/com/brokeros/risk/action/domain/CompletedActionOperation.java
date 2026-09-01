package com.brokeros.risk.action.domain;

import java.time.Instant;
import java.util.Objects;

public record CompletedActionOperation(
        ActionOperationId operationId,
        ActionOperationType operationType,
        ActionSemanticFingerprint fingerprint,
        ActionRef actionRef,
        ActionOperationOutcome outcome,
        Instant occurredAt,
        ActionRecord actionRecord) {

    public CompletedActionOperation {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(actionRecord, "actionRecord must not be null");
        if (operationType != ActionOperationType.RECORD
                || outcome != ActionOperationOutcome.CREATED
                || !actionRef.equals(actionRecord.actionRef())
                || !occurredAt.equals(actionRecord.recordedAt())) {
            throw new IllegalArgumentException("completed action operation is inconsistent");
        }
    }
}
