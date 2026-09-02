package com.brokeros.risk.riskcase.domain;

public record InvestigationNoteRef(String value) {

    public InvestigationNoteRef {
        value = RiskCaseIdentifiers.canonicalUuidV4(value, "investigationNoteRef");
    }
}
