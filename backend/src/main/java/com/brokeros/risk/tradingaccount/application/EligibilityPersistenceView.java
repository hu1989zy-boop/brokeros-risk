package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record EligibilityPersistenceView(
        TradingAccountRef tradingAccountRef,
        AuthorityLifecycle accountLifecycle,
        long accountVersion,
        AuthorityOperationId accountLastOperationId,
        AuthorityLifecycle scopeLifecycle,
        long scopeVersion,
        AuthorityOperationId scopeLastOperationId) {
    public EligibilityPersistenceView {
        Objects.requireNonNull(tradingAccountRef, "tradingAccountRef must not be null");
        Objects.requireNonNull(accountLifecycle, "accountLifecycle must not be null");
        Objects.requireNonNull(accountLastOperationId, "accountLastOperationId must not be null");
        Objects.requireNonNull(scopeLifecycle, "scopeLifecycle must not be null");
        Objects.requireNonNull(scopeLastOperationId, "scopeLastOperationId must not be null");
        if (accountVersion < 0 || scopeVersion < 0) throw new IllegalArgumentException("version must not be negative");
    }
}
