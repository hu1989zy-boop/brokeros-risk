package com.brokeros.risk.tradingdata.infrastructure.observability;

import java.time.Duration;

import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.tradingdata.application.port.TradingDataMetricsPort;
import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerTradingDataMetrics implements TradingDataMetricsPort {

    private final MeterRegistry meterRegistry;

    public MicrometerTradingDataMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordOperation(TradingDataIngestionOutcome outcome) {
        meterRegistry.counter(
                "brokeros.risk.tradingdata.operations",
                "outcome", outcome.name()).increment();
    }

    @Override
    public void recordGapDetected() {
        meterRegistry.counter("brokeros.risk.tradingdata.gaps.detected").increment();
    }

    @Override
    public void recordAuthorizationDenied(Capability capability) {
        meterRegistry.counter(
                "brokeros.risk.tradingdata.authorization.denied",
                "capability", capability.value()).increment();
    }

    @Override
    public void recordBackpressure() {
        meterRegistry.counter("brokeros.risk.tradingdata.backpressure").increment();
    }

    @Override
    public void recordDuration(Duration duration) {
        meterRegistry.timer("brokeros.risk.tradingdata.duration").record(duration);
    }
}
