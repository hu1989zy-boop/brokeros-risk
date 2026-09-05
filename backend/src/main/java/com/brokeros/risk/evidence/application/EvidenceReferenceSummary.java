package com.brokeros.risk.evidence.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.EvidenceStatus;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record EvidenceReferenceSummary(
        EvidenceRef evidenceRef,
        TradingAccountRef subjectRef,
        EvidenceStatus status,
        Instant recordedAt) {

    public EvidenceReferenceSummary {
        Objects.requireNonNull(evidenceRef);
        Objects.requireNonNull(subjectRef);
        Objects.requireNonNull(status);
        Objects.requireNonNull(recordedAt);
    }
}
