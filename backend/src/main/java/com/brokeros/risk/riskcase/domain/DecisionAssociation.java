package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record DecisionAssociation(
        Long id,
        RiskCaseId caseId,
        long caseVersion,
        DecisionRef decisionRef,
        ActorRef associatedBy,
        String reason,
        Instant associatedAt) {

    public DecisionAssociation {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        Objects.requireNonNull(associatedBy, "associatedBy must not be null");
        reason = RiskCaseText.require(reason, 1000, "reason");
        Objects.requireNonNull(associatedAt, "associatedAt must not be null");
    }
}
