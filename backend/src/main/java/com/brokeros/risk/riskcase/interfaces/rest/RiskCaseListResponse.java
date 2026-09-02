package com.brokeros.risk.riskcase.interfaces.rest;

import java.util.List;

import com.brokeros.risk.riskcase.application.RiskCasePage;
import com.brokeros.risk.riskcase.application.RiskCaseSummary;

public record RiskCaseListResponse(
        List<RiskCaseSummaryResponse> items,
        int page,
        int size,
        boolean hasNext) {

    public RiskCaseListResponse {
        items = List.copyOf(items);
    }

    public static RiskCaseListResponse from(RiskCasePage<RiskCaseSummary> result) {
        return new RiskCaseListResponse(
                result.items().stream().map(RiskCaseSummaryResponse::from).toList(),
                result.page(),
                result.size(),
                result.hasNext());
    }
}
