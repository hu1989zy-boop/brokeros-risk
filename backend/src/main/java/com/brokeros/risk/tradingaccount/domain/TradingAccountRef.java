package com.brokeros.risk.tradingaccount.domain;

public record TradingAccountRef(String value) {

    public TradingAccountRef {
        value = CanonicalUuidV4.require(value, "ta-");
    }

    @Override
    public String toString() {
        return value;
    }
}
