package com.brokeros.risk.tradingaccount.domain;

public record AccountAuthorityScopeRef(String value) {

    public AccountAuthorityScopeRef {
        value = CanonicalUuidV4.require(value, "aas-");
    }

    @Override
    public String toString() {
        return value;
    }
}
