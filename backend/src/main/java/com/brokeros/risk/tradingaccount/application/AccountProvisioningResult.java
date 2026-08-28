package com.brokeros.risk.tradingaccount.application;

import java.time.Instant;
import java.util.Objects;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record AccountProvisioningResult(
        TradingAccountRef tradingAccountRef,
        AuthorityOperationOutcome outcome,
        long resultingVersion,
        Instant occurredAt) {
    public AccountProvisioningResult {
        Objects.requireNonNull(tradingAccountRef, "tradingAccountRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (resultingVersion < 0) throw new IllegalArgumentException("version must not be negative");
    }
}
