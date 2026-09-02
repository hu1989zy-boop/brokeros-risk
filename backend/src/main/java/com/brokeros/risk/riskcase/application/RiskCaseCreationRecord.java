package com.brokeros.risk.riskcase.application;

import java.util.Arrays;

import com.brokeros.risk.riskcase.domain.RiskCase;

public record RiskCaseCreationRecord(
        RiskCase riskCase,
        byte[] requestHash) {

    public RiskCaseCreationRecord {
        requestHash = requestHash.clone();
    }

    @Override
    public byte[] requestHash() {
        return requestHash.clone();
    }

    public boolean matches(byte[] candidate) {
        return Arrays.equals(requestHash, candidate);
    }
}
