package com.brokeros.risk.riskcase.domain;

import java.util.Objects;

public final class RiskCaseDomainException extends RuntimeException {

    private final RiskCaseDomainError error;

    public RiskCaseDomainException(RiskCaseDomainError error, String message) {
        super(message);
        this.error = Objects.requireNonNull(error, "error must not be null");
    }

    public RiskCaseDomainError error() {
        return error;
    }
}
