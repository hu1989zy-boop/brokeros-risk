package com.brokeros.risk.evidence.infrastructure.observability;

import java.time.Duration;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.EvidenceMetricOperation;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.security.domain.Capability;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerEvidenceMetrics implements EvidenceMetricsPort {

    private final MeterRegistry meterRegistry;

    public MicrometerEvidenceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordOperation(
            EvidenceMetricOperation operation,
            EvidenceOperationOutcome outcome) {
        meterRegistry.counter(
                "brokeros.risk.evidence.operations",
                "operation", operation.name(),
                "outcome", outcome.name()).increment();
    }

    @Override
    public void recordConflict(ResultCode category) {
        meterRegistry.counter(
                "brokeros.risk.evidence.conflicts",
                "category", category.code()).increment();
    }

    @Override
    public void recordAuthorizationDenied(Capability capability) {
        meterRegistry.counter(
                "brokeros.risk.evidence.authorization.denied",
                "capability", capability.value()).increment();
    }

    @Override
    public void recordAccessRead(String outcome) {
        meterRegistry.counter(
                "brokeros.risk.evidence.access.reads",
                "outcome", outcome).increment();
    }

    @Override
    public void recordDuration(
            EvidenceMetricOperation operation,
            Duration duration) {
        meterRegistry.timer(
                "brokeros.risk.evidence.duration",
                "operation", operation.name()).record(duration);
    }
}
