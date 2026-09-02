package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.riskcase.application.RiskCaseResolutionResult;

public record RiskCaseResolutionResponse(
        RiskCaseDetailResponse riskCase,
        int cycleNo,
        String outcome,
        String decisionRef,
        String resolutionSummary,
        String resolvedByRef,
        Instant resolvedAt) {

    public static RiskCaseResolutionResponse from(RiskCaseResolutionResult result) {
        return new RiskCaseResolutionResponse(
                RiskCaseDetailResponse.from(result.riskCase()),
                result.resolution().cycle().value(), result.resolution().outcome().name(),
                result.resolution().decisionRef().value(), result.resolution().summary(),
                result.resolution().resolvedBy().value(), result.resolution().resolvedAt());
    }
}
