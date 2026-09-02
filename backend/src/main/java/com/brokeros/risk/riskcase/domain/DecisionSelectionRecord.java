package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record DecisionSelectionRecord(
        RiskCaseId caseId,
        long caseVersion,
        DecisionRef previousDecisionRef,
        DecisionRef newDecisionRef,
        ActorRef selectedBy,
        String reason,
        Instant selectedAt) {

    public DecisionSelectionRecord {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(selectedBy, "selectedBy must not be null");
        reason = RiskCaseText.require(reason, 1000, "reason");
        Objects.requireNonNull(selectedAt, "selectedAt must not be null");
        if (Objects.equals(previousDecisionRef, newDecisionRef)) {
            throw new IllegalArgumentException("decision selection must change");
        }
    }
}
