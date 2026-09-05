package com.brokeros.risk.action.interfaces.rest;

import java.time.Instant;
import java.util.List;

import com.brokeros.risk.action.application.ActionReferenceSummary;
import com.brokeros.risk.action.domain.ActionStatus;

public record ActionReferenceListResponse(List<Item> items) {

    public ActionReferenceListResponse {
        items = List.copyOf(items);
    }

    static ActionReferenceListResponse from(List<ActionReferenceSummary> summaries) {
        return new ActionReferenceListResponse(summaries.stream().map(Item::from).toList());
    }

    public record Item(
            String actionRef,
            String decisionRef,
            ActionStatus status,
            Instant recordedAt) {

        static Item from(ActionReferenceSummary summary) {
            return new Item(
                    summary.actionRef().value(),
                    summary.decisionRef().value(),
                    summary.status(),
                    summary.recordedAt());
        }
    }
}
