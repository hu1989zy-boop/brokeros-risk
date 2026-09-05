package com.brokeros.risk.decision.interfaces.rest;

import java.time.Instant;
import java.util.List;

import com.brokeros.risk.decision.application.DecisionReferenceSummary;

public record DecisionReferenceListResponse(List<Item> items) {

    public DecisionReferenceListResponse {
        items = List.copyOf(items);
    }

    static DecisionReferenceListResponse from(List<DecisionReferenceSummary> summaries) {
        return new DecisionReferenceListResponse(summaries.stream().map(Item::from).toList());
    }

    public record Item(String decisionRef, String subjectRef, Instant recordedAt) {

        static Item from(DecisionReferenceSummary summary) {
            return new Item(
                    summary.decisionRef().value(),
                    summary.subjectRef().value(),
                    summary.recordedAt());
        }
    }
}
