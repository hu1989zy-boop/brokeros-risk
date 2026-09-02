package com.brokeros.risk.riskcase.application;

import java.util.List;

public record RiskCaseHistoryPage(
        List<RiskCaseHistoryEntry> entries,
        String nextCursor) {

    public RiskCaseHistoryPage {
        entries = List.copyOf(entries);
    }
}
