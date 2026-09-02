package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record ActionAssociationEvent(
        Long id,
        RiskCaseId caseId,
        long caseVersion,
        ActionAssociationEventType eventType,
        ActionRef actionRef,
        DecisionRef decisionRef,
        ActionOutcomeRef outcomeRef,
        Long priorEventId,
        String reason,
        ActorRef actorRef,
        Instant occurredAt) {

    public ActionAssociationEvent {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        reason = RiskCaseText.require(reason, 1000, "reason");
        Objects.requireNonNull(actorRef, "actorRef must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
