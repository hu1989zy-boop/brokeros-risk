package com.brokeros.risk.evidence.application;

import java.util.Objects;

public record CorrectEvidenceCommand(
        String operationId,
        String targetEvidenceRef,
        String correctionReason,
        String observationText) {

    public CorrectEvidenceCommand {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(targetEvidenceRef, "targetEvidenceRef must not be null");
        Objects.requireNonNull(correctionReason, "correctionReason must not be null");
        Objects.requireNonNull(observationText, "observationText must not be null");
    }
}
