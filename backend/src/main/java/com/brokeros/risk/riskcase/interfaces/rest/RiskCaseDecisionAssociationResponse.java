package com.brokeros.risk.riskcase.interfaces.rest;

import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;

public record RiskCaseDecisionAssociationResponse(
        String decisionRef,
        long version) {

    public static RiskCaseDecisionAssociationResponse from(RiskCaseSnapshot snapshot) {
        return new RiskCaseDecisionAssociationResponse(
                snapshot.currentDecisionRef().value(), snapshot.version());
    }
}
