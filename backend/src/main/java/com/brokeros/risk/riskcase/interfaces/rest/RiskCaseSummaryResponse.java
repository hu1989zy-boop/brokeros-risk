package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.riskcase.application.RiskCaseSummary;

public record RiskCaseSummaryResponse(
        String caseNumber,
        String subjectRef,
        String status,
        String priority,
        String assigneeRef,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public static RiskCaseSummaryResponse from(RiskCaseSummary summary) {
        return new RiskCaseSummaryResponse(
                summary.caseNumber().value(),
                summary.subjectRef().value(),
                summary.status().name(),
                summary.priority().name(),
                summary.assigneeRef() == null ? null : summary.assigneeRef().value(),
                summary.createdAt(),
                summary.updatedAt(),
                summary.version());
    }
}
