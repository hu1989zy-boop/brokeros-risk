package com.brokeros.risk.evidence.application;

import java.util.Objects;

import com.brokeros.risk.evidence.domain.CorrectionReason;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.ObservationText;

public record CorrectEvidenceSpec(
        EvidenceOperationId operationId,
        EvidenceRef targetEvidenceRef,
        CorrectionReason correctionReason,
        ObservationText observationText) {

    public CorrectEvidenceSpec {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(targetEvidenceRef, "targetEvidenceRef must not be null");
        Objects.requireNonNull(correctionReason, "correctionReason must not be null");
        Objects.requireNonNull(observationText, "observationText must not be null");
    }
}
