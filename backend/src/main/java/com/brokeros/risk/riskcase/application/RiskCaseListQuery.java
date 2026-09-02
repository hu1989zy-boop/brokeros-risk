package com.brokeros.risk.riskcase.application;

import com.brokeros.risk.riskcase.domain.RiskCasePriority;
import com.brokeros.risk.riskcase.domain.RiskCaseStatus;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public record RiskCaseListQuery(
        RiskCaseStatus status,
        RiskCasePriority priority,
        TradingAccountRef subjectRef,
        ActorRef assigneeRef) {
}
