package com.brokeros.risk.tradingaccount.domain;

public enum AuthorityOperationType {
    REGISTER_AUTHORITY_SCOPE,
    REGISTER_TRADING_ACCOUNT,
    DEACTIVATE_AUTHORITY_SCOPE,
    REACTIVATE_AUTHORITY_SCOPE,
    RETIRE_AUTHORITY_SCOPE,
    DEACTIVATE_TRADING_ACCOUNT,
    REACTIVATE_TRADING_ACCOUNT,
    RETIRE_TRADING_ACCOUNT;

    public boolean isScopeOperation() {
        return name().endsWith("AUTHORITY_SCOPE");
    }

    public boolean isRegistration() {
        return this == REGISTER_AUTHORITY_SCOPE || this == REGISTER_TRADING_ACCOUNT;
    }

    public AuthorityLifecycle targetLifecycle() {
        return switch (this) {
            case DEACTIVATE_AUTHORITY_SCOPE, DEACTIVATE_TRADING_ACCOUNT -> AuthorityLifecycle.INACTIVE;
            case REACTIVATE_AUTHORITY_SCOPE, REACTIVATE_TRADING_ACCOUNT -> AuthorityLifecycle.ACTIVE;
            case RETIRE_AUTHORITY_SCOPE, RETIRE_TRADING_ACCOUNT -> AuthorityLifecycle.RETIRED;
            default -> throw new IllegalStateException("registration has no lifecycle target");
        };
    }
}
