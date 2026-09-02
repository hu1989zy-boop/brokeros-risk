package com.brokeros.risk.riskcase.interfaces.rest;

import java.util.List;

import com.brokeros.risk.riskcase.application.RiskCaseHistoryPage;

public record RiskCaseHistoryPageResponse(
        List<RiskCaseHistoryEntryResponse> entries,
        String nextCursor) {

    public static RiskCaseHistoryPageResponse from(RiskCaseHistoryPage page) {
        return new RiskCaseHistoryPageResponse(
                page.entries().stream().map(RiskCaseHistoryEntryResponse::from).toList(),
                page.nextCursor());
    }
}
