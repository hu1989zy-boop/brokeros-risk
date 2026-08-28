package com.brokeros.risk.tradingaccount.infrastructure.observability;

import java.time.Duration;

import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMetricsPort;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerTradingAccountAuthorityMetrics
        implements TradingAccountAuthorityMetricsPort {

    private final MeterRegistry meterRegistry;

    public MicrometerTradingAccountAuthorityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordOperation(AuthorityOperationType type, AuthorityOperationOutcome outcome) {
        meterRegistry.counter(
                "brokeros.risk.trading.account.authority.operations",
                "operation", type.name(),
                "outcome", outcome.name()).increment();
    }

    @Override
    public void recordDuration(AuthorityOperationType type, Duration duration) {
        meterRegistry.timer(
                "brokeros.risk.trading.account.authority.duration",
                "operation", type.name()).record(duration);
    }
}
