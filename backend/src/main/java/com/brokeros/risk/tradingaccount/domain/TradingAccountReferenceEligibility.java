package com.brokeros.risk.tradingaccount.domain;

import java.util.Objects;

public record TradingAccountReferenceEligibility(
        TradingAccountRef tradingAccountRef,
        EligibilityDecision decision,
        AuthoritySnapshotRef authoritySnapshotRef,
        AuthorityProvenanceRef authorityProvenanceRef) {

    public TradingAccountReferenceEligibility {
        Objects.requireNonNull(tradingAccountRef, "tradingAccountRef must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        if (decision == EligibilityDecision.NOT_RECOGNIZED
                && (authoritySnapshotRef != null || authorityProvenanceRef != null)) {
            throw new IllegalArgumentException("unrecognized reference cannot carry evidence");
        }
        if (decision != EligibilityDecision.NOT_RECOGNIZED
                && (authoritySnapshotRef == null || authorityProvenanceRef == null)) {
            throw new IllegalArgumentException("recognized reference requires evidence");
        }
    }
}
