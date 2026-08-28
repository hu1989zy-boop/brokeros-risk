package com.brokeros.risk.tradingaccount.domain;

public enum AuthorityLifecycle {
    ACTIVE,
    INACTIVE,
    RETIRED;

    public AuthorityLifecycle transitionTo(AuthorityLifecycle target) {
        if (this == ACTIVE && (target == INACTIVE || target == RETIRED)) {
            return target;
        }
        if (this == INACTIVE && (target == ACTIVE || target == RETIRED)) {
            return target;
        }
        throw new IllegalStateException("lifecycle transition is invalid");
    }
}
