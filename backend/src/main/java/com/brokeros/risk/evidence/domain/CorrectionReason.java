package com.brokeros.risk.evidence.domain;

public record CorrectionReason(String value) {

    public CorrectionReason {
        value = EvidenceText.require(value, 1000, "correctionReason");
    }

    @Override
    public String toString() {
        return value;
    }
}
