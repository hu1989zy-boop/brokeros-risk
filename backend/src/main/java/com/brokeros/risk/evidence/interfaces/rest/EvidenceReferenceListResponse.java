package com.brokeros.risk.evidence.interfaces.rest;

import java.time.Instant;
import java.util.List;

import com.brokeros.risk.evidence.application.EvidenceReferenceSummary;
import com.brokeros.risk.evidence.domain.EvidenceStatus;

public record EvidenceReferenceListResponse(List<Item> items) {

    public EvidenceReferenceListResponse {
        items = List.copyOf(items);
    }

    static EvidenceReferenceListResponse from(List<EvidenceReferenceSummary> summaries) {
        return new EvidenceReferenceListResponse(summaries.stream().map(Item::from).toList());
    }

    public record Item(
            String evidenceRef,
            String subjectRef,
            EvidenceStatus status,
            Instant recordedAt) {

        static Item from(EvidenceReferenceSummary summary) {
            return new Item(
                    summary.evidenceRef().value(),
                    summary.subjectRef().value(),
                    summary.status(),
                    summary.recordedAt());
        }
    }
}
