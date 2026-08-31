package com.brokeros.risk.decision.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.DecisionCapabilities;
import com.brokeros.risk.decision.application.DecisionMetricOperation;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class DecisionMetricsTests {

    @Test
    void metricsUseOnlyBoundedSafeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DecisionMetrics metrics = new DecisionMetrics(registry);

        metrics.recordOperation(
                DecisionMetricOperation.RECORD, DecisionOperationOutcome.CREATED);
        metrics.recordConflict(ResultCode.DECISION_IDEMPOTENCY_CONFLICT);
        metrics.recordAuthorizationDenied(DecisionCapabilities.READ);
        metrics.recordAccessRead("RECOGNIZED");
        metrics.recordDuration(DecisionMetricOperation.RECORD, Duration.ofMillis(5));

        assertThat(registry.get("brokeros.risk.decision.operations")
                .tags("operation", "RECORD", "outcome", "CREATED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.decision.conflicts")
                .tag("category", "DECISION_IDEMPOTENCY_CONFLICT")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.decision.authorization.denied")
                .tag("capability", "decision:read")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.decision.access.reads")
                .tag("outcome", "RECOGNIZED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.decision.duration")
                .tag("operation", "RECORD").timer().count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .allSatisfy(tag -> assertThat(tag.getValue())
                                .doesNotContain(
                                        "dec-", "ev-", "ta-", "conclusion",
                                        "00000000-0000-4000-8000-000000000001")));
    }
}
