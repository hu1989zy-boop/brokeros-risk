package com.brokeros.risk.tradingdata.application.port;

import java.time.Duration;

import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;

public interface TradingDataMetricsPort {

    void recordOperation(TradingDataIngestionOutcome outcome);

    void recordGapDetected();

    void recordAuthorizationDenied(Capability capability);

    void recordBackpressure();

    void recordDuration(Duration duration);
}
