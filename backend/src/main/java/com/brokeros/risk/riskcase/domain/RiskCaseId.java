package com.brokeros.risk.riskcase.domain;

public record RiskCaseId(long value) {

    public RiskCaseId {
        if (value <= 0) {
            throw new IllegalArgumentException("riskCaseId must be positive");
        }
    }
}
