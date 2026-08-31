package com.brokeros.risk.decision.infrastructure.observability;

import java.time.Duration;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.DecisionMetricOperation;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.security.domain.Capability;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class DecisionMetrics implements DecisionMetricsPort {

    private final MeterRegistry meterRegistry;

    public DecisionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordOperation(
            DecisionMetricOperation operation,
            DecisionOperationOutcome outcome) {
        meterRegistry.counter(
                "brokeros.risk.decision.operations",
                "operation", operation.name(),
                "outcome", outcome.name()).increment();
    }

    @Override
    public void recordConflict(ResultCode category) {
        meterRegistry.counter(
                "brokeros.risk.decision.conflicts",
                "category", category.code()).increment();
    }

    @Override
    public void recordAuthorizationDenied(Capability capability) {
        meterRegistry.counter(
                "brokeros.risk.decision.authorization.denied",
                "capability", capability.value()).increment();
    }

    @Override
    public void recordAccessRead(String outcome) {
        meterRegistry.counter(
                "brokeros.risk.decision.access.reads",
                "outcome", outcome).increment();
    }

    @Override
    public void recordDuration(
            DecisionMetricOperation operation,
            Duration duration) {
        meterRegistry.timer(
                "brokeros.risk.decision.duration",
                "operation", operation.name()).record(duration);
    }
}
