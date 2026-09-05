package com.brokeros.risk.actionoutcome.interfaces.rest;

import java.time.Instant;
import java.util.List;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeReferenceSummary;

public record ActionOutcomeReferenceListResponse(List<Item> items) {

    public ActionOutcomeReferenceListResponse {
        items = List.copyOf(items);
    }

    static ActionOutcomeReferenceListResponse from(
            List<ActionOutcomeReferenceSummary> summaries) {
        return new ActionOutcomeReferenceListResponse(
                summaries.stream().map(Item::from).toList());
    }

    public record Item(String actionOutcomeRef, String actionRef, Instant recordedAt) {

        static Item from(ActionOutcomeReferenceSummary summary) {
            return new Item(
                    summary.actionOutcomeRef().value(),
                    summary.actionRef().value(),
                    summary.recordedAt());
        }
    }
}
