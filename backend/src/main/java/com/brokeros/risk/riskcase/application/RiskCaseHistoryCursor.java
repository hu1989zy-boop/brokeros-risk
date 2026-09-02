package com.brokeros.risk.riskcase.application;

public record RiskCaseHistoryCursor(long caseVersion, int eventRank, long rowId) {

    public RiskCaseHistoryCursor {
        if (caseVersion < 0 || eventRank < 0 || rowId < 0) {
            throw new IllegalArgumentException("history cursor values must be nonnegative");
        }
    }
}
