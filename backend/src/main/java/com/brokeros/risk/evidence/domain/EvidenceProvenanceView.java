package com.brokeros.risk.evidence.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record EvidenceProvenanceView(
        EvidenceRef evidenceRef,
        EvidenceProvenanceOutcome outcome,
        TradingAccountRef subjectRef,
        EvidenceSource source,
        ActorRef recordedByActorRef,
        Instant recordedAt,
        EvidenceStatus status,
        EvidenceRef supersededByRef) {

    public EvidenceProvenanceView {
        Objects.requireNonNull(evidenceRef, "evidenceRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        boolean complete = subjectRef != null && source != null && recordedByActorRef != null
                && recordedAt != null && status != null;
        if (outcome == EvidenceProvenanceOutcome.RECOGNIZED && !complete) {
            throw new IllegalArgumentException("recognized provenance must be complete");
        }
        if (outcome == EvidenceProvenanceOutcome.NOT_FOUND
                && (complete || subjectRef != null || source != null || recordedByActorRef != null
                || recordedAt != null || status != null || supersededByRef != null)) {
            throw new IllegalArgumentException("not-found provenance cannot disclose metadata");
        }
    }

    public static EvidenceProvenanceView recognized(EvidenceRecord record) {
        return new EvidenceProvenanceView(
                record.evidenceRef(), EvidenceProvenanceOutcome.RECOGNIZED,
                record.subjectRef(), record.source(), record.recordedByActorRef(),
                record.recordedAt(), record.status(), record.supersededByRef());
    }

    public static EvidenceProvenanceView notFound(EvidenceRef ref) {
        return new EvidenceProvenanceView(
                ref, EvidenceProvenanceOutcome.NOT_FOUND,
                null, null, null, null, null, null);
    }
}
