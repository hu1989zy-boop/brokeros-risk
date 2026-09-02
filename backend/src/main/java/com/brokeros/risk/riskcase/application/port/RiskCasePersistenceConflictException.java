package com.brokeros.risk.riskcase.application.port;

public final class RiskCasePersistenceConflictException extends RuntimeException {

    private final RiskCaseConflictKind kind;

    public RiskCasePersistenceConflictException(
            RiskCaseConflictKind kind, Throwable cause) {
        super(cause);
        this.kind = kind;
    }

    public RiskCaseConflictKind kind() {
        return kind;
    }
}
