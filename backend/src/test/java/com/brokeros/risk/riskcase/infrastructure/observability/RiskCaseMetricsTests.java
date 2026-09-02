package com.brokeros.risk.riskcase.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import com.brokeros.risk.riskcase.application.RiskCaseCapabilities;
import com.brokeros.risk.riskcase.application.RiskCaseMetricOperation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RiskCaseMetricsTests {

    @Test
    void metricsUseOnlyBoundedOperationOutcomeConflictAndCapabilityTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RiskCaseMetrics metrics = new RiskCaseMetrics(registry);

        metrics.recordSuccess(RiskCaseMetricOperation.CREATE);
        metrics.recordConflict("VERSION");
        metrics.recordAuthorizationDenied(RiskCaseCapabilities.READ);
        metrics.recordDuration(RiskCaseMetricOperation.READ, Duration.ofMillis(5));

        assertThat(registry.getMeters()).hasSize(4);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .allMatch(tag -> Set.of(
                        "operation", "outcome", "category", "capability")
                        .contains(tag.getKey()));
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .noneMatch(tag -> tag.getValue().startsWith("RC-")
                        || tag.getValue().contains("note")
                        || tag.getValue().contains("manual intake"));
    }
}
