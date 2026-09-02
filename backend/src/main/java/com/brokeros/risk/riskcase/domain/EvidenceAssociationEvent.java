package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorRef;

public record EvidenceAssociationEvent(
        Long id,
        EvidenceAssociationEventRef eventRef,
        RiskCaseId caseId,
        long caseVersion,
        EvidenceAssociationEventType eventType,
        EvidenceRef evidenceRef,
        Long priorEventId,
        EvidenceRef replacementEvidenceRef,
        String reason,
        String source,
        ActorRef actorRef,
        Instant occurredAt) {

    public EvidenceAssociationEvent {
        Objects.requireNonNull(eventRef, "eventRef must not be null");
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(evidenceRef, "evidenceRef must not be null");
        reason = RiskCaseText.require(reason, 1000, "reason");
        source = RiskCaseText.require(source, 64, "source");
        Objects.requireNonNull(actorRef, "actorRef must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
