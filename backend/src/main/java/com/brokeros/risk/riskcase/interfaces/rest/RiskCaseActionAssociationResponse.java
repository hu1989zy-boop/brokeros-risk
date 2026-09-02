package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.riskcase.domain.ActionAssociationEvent;

public record RiskCaseActionAssociationResponse(
        String eventType,
        String actionRef,
        String decisionRef,
        String outcomeRef,
        long version,
        String actorRef,
        Instant occurredAt) {

    public static RiskCaseActionAssociationResponse from(ActionAssociationEvent event) {
        return new RiskCaseActionAssociationResponse(
                event.eventType().name(), event.actionRef().value(), event.decisionRef().value(),
                event.outcomeRef() == null ? null : event.outcomeRef().value(),
                event.caseVersion(), event.actorRef().value(), event.occurredAt());
    }
}
