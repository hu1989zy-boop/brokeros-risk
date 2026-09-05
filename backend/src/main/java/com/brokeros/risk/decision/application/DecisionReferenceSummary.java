package com.brokeros.risk.decision.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record DecisionReferenceSummary(
        DecisionRef decisionRef,
        TradingAccountRef subjectRef,
        Instant recordedAt) {

    public DecisionReferenceSummary {
        Objects.requireNonNull(decisionRef);
        Objects.requireNonNull(subjectRef);
        Objects.requireNonNull(recordedAt);
    }
}
