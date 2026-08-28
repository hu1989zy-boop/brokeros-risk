package com.brokeros.risk.tradingaccount.domain;

public record AuthorityOperationId(String value) {

    public AuthorityOperationId {
        value = CanonicalUuidV4.requireOperationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
