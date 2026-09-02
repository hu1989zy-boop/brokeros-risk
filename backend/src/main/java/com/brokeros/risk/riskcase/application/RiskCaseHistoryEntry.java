package com.brokeros.risk.riskcase.application;

import java.time.Instant;

public record RiskCaseHistoryEntry(
        long caseVersion,
        int eventRank,
        long rowId,
        String eventType,
        String affectedRef,
        String actorRef,
        Instant occurredAt) {
}
