package com.brokeros.risk.tradingdata.interfaces.rest;

import com.brokeros.risk.tradingdata.application.TradingDataIngestionResult;
import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;

public record TradingDataIngestionResponse(
        TradingDataIngestionOutcome outcome,
        boolean gapDetected) {

    static TradingDataIngestionResponse from(TradingDataIngestionResult result) {
        return new TradingDataIngestionResponse(
                result.outcome(), result.gapDetected());
    }
}
