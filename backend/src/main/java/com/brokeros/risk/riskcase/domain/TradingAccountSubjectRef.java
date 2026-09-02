package com.brokeros.risk.riskcase.domain;

import java.util.Objects;

import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record TradingAccountSubjectRef(TradingAccountRef tradingAccountRef) {

    public TradingAccountSubjectRef {
        Objects.requireNonNull(tradingAccountRef, "tradingAccountRef must not be null");
    }

    public RiskSubjectType subjectType() {
        return RiskSubjectType.TRADING_ACCOUNT;
    }

    public String value() {
        return tradingAccountRef.value();
    }
}
