package com.brokeros.risk.evidence.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.evidence.domain.EvidenceRef;

public record EvidenceCorrectionResult(
        EvidenceRef evidenceRef,
        EvidenceOperationOutcome outcome,
        Instant occurredAt) {

    public EvidenceCorrectionResult {
        Objects.requireNonNull(evidenceRef, "evidenceRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (outcome != EvidenceOperationOutcome.CORRECTED) {
            throw new IllegalArgumentException("correction result must be CORRECTED");
        }
    }
}
