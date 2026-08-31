package com.brokeros.risk.evidence.domain;

public record ObservationText(String value) {

    public ObservationText {
        value = EvidenceText.require(value, 4000, "observationText");
    }

    @Override
    public String toString() {
        return value;
    }
}
