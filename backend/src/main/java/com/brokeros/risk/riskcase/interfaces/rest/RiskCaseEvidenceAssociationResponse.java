package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.riskcase.domain.EvidenceAssociationEvent;

public record RiskCaseEvidenceAssociationResponse(
        String associationEventRef,
        String eventType,
        String evidenceRef,
        String replacementEvidenceRef,
        long version,
        String actorRef,
        Instant occurredAt) {

    public static RiskCaseEvidenceAssociationResponse from(EvidenceAssociationEvent event) {
        return new RiskCaseEvidenceAssociationResponse(
                event.eventRef().value(), event.eventType().name(), event.evidenceRef().value(),
                event.replacementEvidenceRef() == null
                        ? null
                        : event.replacementEvidenceRef().value(),
                event.caseVersion(), event.actorRef().value(), event.occurredAt());
    }
}
