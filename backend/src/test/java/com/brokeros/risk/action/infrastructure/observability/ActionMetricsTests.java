package com.brokeros.risk.action.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.brokeros.risk.action.application.ActionCapabilities;
import com.brokeros.risk.action.application.ActionMetricOperation;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.api.ResultCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ActionMetricsTests {

    @Test
    void metricsUseOnlyBoundedSafeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ActionMetrics metrics = new ActionMetrics(registry);

        metrics.recordOperation(
                ActionMetricOperation.RECORD, ActionOperationOutcome.CREATED);
        metrics.recordConflict(ResultCode.ACTION_IDEMPOTENCY_CONFLICT);
        metrics.recordAuthorizationDenied(ActionCapabilities.READ);
        metrics.recordAccessRead("RECOGNIZED");
        metrics.recordDuration(ActionMetricOperation.RECORD, Duration.ofMillis(5));

        assertThat(registry.get("brokeros.risk.action.operations")
                .tags("operation", "RECORD", "outcome", "CREATED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.action.conflicts")
                .tag("category", "ACTION_IDEMPOTENCY_CONFLICT")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.action.authorization.denied")
                .tag("capability", "action:read")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.action.access.reads")
                .tag("outcome", "RECOGNIZED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.action.duration")
                .tag("operation", "RECORD").timer().count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .allSatisfy(tag -> assertThat(tag.getValue())
                                .doesNotContain(
                                        "act-", "dec-", "intent",
                                        "00000000-0000-4000-8000-000000000001")));
    }
}
