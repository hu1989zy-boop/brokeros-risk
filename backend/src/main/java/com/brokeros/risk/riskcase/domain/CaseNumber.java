package com.brokeros.risk.riskcase.domain;

import java.util.Objects;

public record CaseNumber(String value) {

    public CaseNumber {
        Objects.requireNonNull(value, "caseNumber must not be null");
        if (!value.startsWith("RC-") || value.length() != 39) {
            throw new IllegalArgumentException("caseNumber must use RC-<UUIDv4>");
        }
        RiskCaseIdentifiers.canonicalUuidV4(value.substring(3), "caseNumber");
    }

    @Override
    public String toString() {
        return value;
    }
}
