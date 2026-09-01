package com.brokeros.risk.action.infrastructure.observability;

import java.time.Duration;

import com.brokeros.risk.action.application.ActionMetricOperation;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.domain.Capability;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ActionMetrics implements ActionMetricsPort {

    private final MeterRegistry meterRegistry;

    public ActionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordOperation(
            ActionMetricOperation operation,
            ActionOperationOutcome outcome) {
        meterRegistry.counter(
                "brokeros.risk.action.operations",
                "operation", operation.name(),
                "outcome", outcome.name()).increment();
    }

    @Override
    public void recordConflict(ResultCode category) {
        meterRegistry.counter(
                "brokeros.risk.action.conflicts",
                "category", category.code()).increment();
    }

    @Override
    public void recordAuthorizationDenied(Capability capability) {
        meterRegistry.counter(
                "brokeros.risk.action.authorization.denied",
                "capability", capability.value()).increment();
    }

    @Override
    public void recordAccessRead(String outcome) {
        meterRegistry.counter(
                "brokeros.risk.action.access.reads",
                "outcome", outcome).increment();
    }

    @Override
    public void recordDuration(
            ActionMetricOperation operation,
            Duration duration) {
        meterRegistry.timer(
                "brokeros.risk.action.duration",
                "operation", operation.name()).record(duration);
    }
}
