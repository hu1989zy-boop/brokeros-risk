package com.brokeros.risk.evidence.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record EvidenceRecord(
        EvidenceRef evidenceRef,
        TradingAccountRef subjectRef,
        EvidenceSource source,
        ObservationText observationText,
        EvidenceStatus status,
        ActorRef recordedByActorRef,
        Instant recordedAt,
        EvidenceRef supersedesRef,
        EvidenceRef supersededByRef) {

    public EvidenceRecord {
        Objects.requireNonNull(evidenceRef, "evidenceRef must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(observationText, "observationText must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(recordedByActorRef, "recordedByActorRef must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (status == EvidenceStatus.ACTIVE && supersededByRef != null) {
            throw new IllegalArgumentException("active evidence cannot have a replacement");
        }
        if (status == EvidenceStatus.SUPERSEDED && supersededByRef == null) {
            throw new IllegalArgumentException("superseded evidence requires a replacement");
        }
        if (evidenceRef.equals(supersedesRef) || evidenceRef.equals(supersededByRef)) {
            throw new IllegalArgumentException("evidence cannot supersede itself");
        }
    }

    public EvidenceRecord supersededBy(EvidenceRef replacementRef) {
        Objects.requireNonNull(replacementRef, "replacementRef must not be null");
        if (status != EvidenceStatus.ACTIVE || supersededByRef != null) {
            throw new IllegalStateException("only active evidence may be superseded once");
        }
        return new EvidenceRecord(
                evidenceRef, subjectRef, source, observationText,
                EvidenceStatus.SUPERSEDED, recordedByActorRef, recordedAt,
                supersedesRef, replacementRef);
    }
}
