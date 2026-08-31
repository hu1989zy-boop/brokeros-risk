package com.brokeros.risk.evidence.application;

import java.util.Objects;

public record RecordEvidenceCommand(
        String operationId,
        String subjectRef,
        String observationText) {

    public RecordEvidenceCommand {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(observationText, "observationText must not be null");
    }
}
