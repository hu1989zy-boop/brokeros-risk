package com.brokeros.risk.evidence.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.evidence.domain.EvidenceFingerprint;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.evidence.domain.EvidenceOperationType;
import com.brokeros.risk.evidence.domain.EvidenceRef;

public record CompletedEvidenceOperation(
        EvidenceOperationType operationType,
        EvidenceFingerprint fingerprint,
        EvidenceRef resultEvidenceRef,
        EvidenceOperationOutcome outcome,
        Instant occurredAt) {

    public CompletedEvidenceOperation {
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(resultEvidenceRef, "resultEvidenceRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if ((operationType == EvidenceOperationType.RECORD
                && outcome != EvidenceOperationOutcome.CREATED)
                || (operationType == EvidenceOperationType.CORRECT
                && outcome != EvidenceOperationOutcome.CORRECTED)) {
            throw new IllegalArgumentException("operation type and outcome do not match");
        }
    }
}
