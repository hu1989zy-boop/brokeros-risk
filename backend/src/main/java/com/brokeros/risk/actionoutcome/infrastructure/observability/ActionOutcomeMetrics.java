package com.brokeros.risk.actionoutcome.infrastructure.observability;

import java.time.Duration;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeMetricOperation;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.domain.Capability;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ActionOutcomeMetrics implements ActionOutcomeMetricsPort {

    private final MeterRegistry meterRegistry;

    public ActionOutcomeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordOperation(
            ActionOutcomeMetricOperation operation,
            ActionOutcomeOperationOutcome outcome) {
        meterRegistry.counter(
                "brokeros.risk.actionoutcome.operations",
                "operation", operation.name(),
                "outcome", outcome.name()).increment();
    }

    @Override
    public void recordConflict(ResultCode category) {
        meterRegistry.counter(
                "brokeros.risk.actionoutcome.conflicts",
                "category", category.code()).increment();
    }

    @Override
    public void recordAuthorizationDenied(Capability capability) {
        meterRegistry.counter(
                "brokeros.risk.actionoutcome.authorization.denied",
                "capability", capability.value()).increment();
    }

    @Override
    public void recordAccessRead(String outcome) {
        meterRegistry.counter(
                "brokeros.risk.actionoutcome.access.reads",
                "outcome", outcome).increment();
    }

    @Override
    public void recordDuration(
            ActionOutcomeMetricOperation operation,
            Duration duration) {
        meterRegistry.timer(
                "brokeros.risk.actionoutcome.duration",
                "operation", operation.name()).record(duration);
    }
}
