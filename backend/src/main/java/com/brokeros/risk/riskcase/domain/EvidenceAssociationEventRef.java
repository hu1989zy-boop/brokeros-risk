package com.brokeros.risk.riskcase.domain;

public record EvidenceAssociationEventRef(String value) {

    public EvidenceAssociationEventRef {
        value = RiskCaseIdentifiers.canonicalUuidV4(value, "evidenceAssociationEventRef");
    }
}
