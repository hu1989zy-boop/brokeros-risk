package com.brokeros.risk.riskcase.infrastructure.observability;

import java.time.Duration;

import com.brokeros.risk.riskcase.application.RiskCaseMetricOperation;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.security.domain.Capability;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RiskCaseMetrics implements RiskCaseMetricsPort {

    private final MeterRegistry meterRegistry;

    public RiskCaseMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordSuccess(RiskCaseMetricOperation operation) {
        meterRegistry.counter(
                "brokeros.risk.riskcase.operations",
                "operation", operation.name(),
                "outcome", "SUCCESS").increment();
    }

    @Override
    public void recordConflict(String category) {
        meterRegistry.counter(
                "brokeros.risk.riskcase.conflicts",
                "category", category).increment();
    }

    @Override
    public void recordAuthorizationDenied(Capability capability) {
        meterRegistry.counter(
                "brokeros.risk.riskcase.authorization.denied",
                "capability", capability.value()).increment();
    }

    @Override
    public void recordDuration(
            RiskCaseMetricOperation operation, Duration duration) {
        meterRegistry.timer(
                "brokeros.risk.riskcase.duration",
                "operation", operation.name()).record(duration);
    }
}
