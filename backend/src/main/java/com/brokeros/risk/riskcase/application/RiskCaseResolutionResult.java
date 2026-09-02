package com.brokeros.risk.riskcase.application;

import com.brokeros.risk.riskcase.domain.ResolutionRecord;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;

public record RiskCaseResolutionResult(
        RiskCaseSnapshot riskCase,
        ResolutionRecord resolution) {
}
