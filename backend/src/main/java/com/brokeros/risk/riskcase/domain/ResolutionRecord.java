package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record ResolutionRecord(
        Long id,
        RiskCaseId caseId,
        ResolutionCycleNumber cycle,
        long caseVersion,
        ResolutionOutcome outcome,
        DecisionRef decisionRef,
        String summary,
        ActorRef resolvedBy,
        Instant resolvedAt) {

    public ResolutionRecord {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(cycle, "cycle must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        summary = RiskCaseText.require(summary, 2000, "resolution summary");
        Objects.requireNonNull(resolvedBy, "resolvedBy must not be null");
        Objects.requireNonNull(resolvedAt, "resolvedAt must not be null");
    }
}
