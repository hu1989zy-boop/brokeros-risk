package com.brokeros.risk.riskcase.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.RiskCasePriority;
import com.brokeros.risk.riskcase.domain.RiskCaseStatus;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record RiskCaseSummary(
        CaseNumber caseNumber,
        TradingAccountRef subjectRef,
        RiskCaseStatus status,
        RiskCasePriority priority,
        ActorRef assigneeRef,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public RiskCaseSummary {
        Objects.requireNonNull(caseNumber, "caseNumber must not be null");
        Objects.requireNonNull(subjectRef, "subjectRef must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }
}
