package com.brokeros.risk.tradingdata.application;

import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;

public record TradingDataIngestionResult(
        TradingDataIngestionOutcome outcome,
        boolean gapDetected) {
}
