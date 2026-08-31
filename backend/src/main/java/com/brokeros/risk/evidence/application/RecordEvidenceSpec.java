package com.brokeros.risk.evidence.application;

import java.util.Objects;

import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.ObservationText;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record RecordEvidenceSpec(
        EvidenceOperationId operationId,
        TradingAccountRef subjectRef,
        ObservationText observationText) {

    public RecordEvidenceSpec {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(observationText, "observationText must not be null");
    }
}
