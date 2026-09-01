package com.brokeros.risk.actionoutcome.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeCapabilities;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeMetricOperation;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.api.ResultCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ActionOutcomeMetricsTests {

    @Test
    void metricsUseOnlyBoundedSafeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ActionOutcomeMetrics metrics = new ActionOutcomeMetrics(registry);

        metrics.recordOperation(
                ActionOutcomeMetricOperation.RECORD,
                ActionOutcomeOperationOutcome.CREATED);
        metrics.recordConflict(ResultCode.ACTION_OUTCOME_IDEMPOTENCY_CONFLICT);
        metrics.recordAuthorizationDenied(ActionOutcomeCapabilities.READ);
        metrics.recordAccessRead("RECOGNIZED");
        metrics.recordDuration(
                ActionOutcomeMetricOperation.RECORD, Duration.ofMillis(5));

        assertThat(registry.get("brokeros.risk.actionoutcome.operations")
                .tags("operation", "RECORD", "outcome", "CREATED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.actionoutcome.conflicts")
                .tag("category", "ACTION_OUTCOME_IDEMPOTENCY_CONFLICT")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.actionoutcome.authorization.denied")
                .tag("capability", "action-outcome:read")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.actionoutcome.access.reads")
                .tag("outcome", "RECOGNIZED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.actionoutcome.duration")
                .tag("operation", "RECORD").timer().count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .allSatisfy(tag -> assertThat(tag.getValue())
                                .doesNotContain(
                                        "aoc-", "act-", "outcome text",
                                        "00000000-0000-4000-8000-000000000001")));
    }
}
