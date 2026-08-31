package com.brokeros.risk.evidence.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceSource;
import com.brokeros.risk.evidence.domain.EvidenceStatus;

public record EvidenceDetailResponse(
        String evidenceRef,
        String subjectRef,
        EvidenceSource source,
        EvidenceStatus status,
        String observationText,
        String recordedByActorRef,
        Instant recordedAt,
        String supersedesRef,
        String supersededByRef) {

    static EvidenceDetailResponse from(EvidenceRecord record) {
        return new EvidenceDetailResponse(
                record.evidenceRef().value(),
                record.subjectRef().value(),
                record.source(),
                record.status(),
                record.observationText().value(),
                record.recordedByActorRef().value(),
                record.recordedAt(),
                optionalRef(record.supersedesRef()),
                optionalRef(record.supersededByRef()));
    }

    private static String optionalRef(
            com.brokeros.risk.evidence.domain.EvidenceRef evidenceRef) {
        return evidenceRef == null ? null : evidenceRef.value();
    }
}
