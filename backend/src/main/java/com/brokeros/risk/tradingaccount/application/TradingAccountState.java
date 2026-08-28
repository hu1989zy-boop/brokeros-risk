package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;
import com.brokeros.risk.tradingaccount.domain.AttestationReference;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record TradingAccountState(
        TradingAccountRef tradingAccountRef,
        AuthorityLifecycle lifecycle,
        long version,
        AttestationReference registrationAttestation,
        AuthorityOperationId lastOperationId) {
    public TradingAccountState {
        Objects.requireNonNull(tradingAccountRef, "tradingAccountRef must not be null");
        Objects.requireNonNull(lifecycle, "lifecycle must not be null");
        Objects.requireNonNull(registrationAttestation, "registrationAttestation must not be null");
        Objects.requireNonNull(lastOperationId, "lastOperationId must not be null");
        if (version < 0) throw new IllegalArgumentException("version must not be negative");
    }
}
