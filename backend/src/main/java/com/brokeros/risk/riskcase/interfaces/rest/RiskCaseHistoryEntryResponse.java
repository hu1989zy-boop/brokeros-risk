package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.riskcase.application.RiskCaseHistoryEntry;

public record RiskCaseHistoryEntryResponse(
        long version,
        String eventType,
        String affectedRef,
        String actorRef,
        Instant occurredAt) {

    public static RiskCaseHistoryEntryResponse from(RiskCaseHistoryEntry entry) {
        return new RiskCaseHistoryEntryResponse(
                entry.caseVersion(), entry.eventType(), entry.affectedRef(),
                entry.actorRef(), entry.occurredAt());
    }
}
