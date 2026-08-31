package com.brokeros.risk.evidence.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.EvidenceCapabilities;
import com.brokeros.risk.evidence.application.EvidenceMetricOperation;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class EvidenceMetricsTests {

    @Test
    void metricsUseOnlyBoundedSafeTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerEvidenceMetrics metrics = new MicrometerEvidenceMetrics(registry);

        metrics.recordOperation(
                EvidenceMetricOperation.RECORD, EvidenceOperationOutcome.CREATED);
        metrics.recordConflict(ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT);
        metrics.recordAuthorizationDenied(EvidenceCapabilities.READ);
        metrics.recordAccessRead("RECOGNIZED");
        metrics.recordDuration(EvidenceMetricOperation.RECORD, Duration.ofMillis(5));

        assertThat(registry.get("brokeros.risk.evidence.operations")
                .tags("operation", "RECORD", "outcome", "CREATED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.evidence.conflicts")
                .tag("category", "EVIDENCE_IDEMPOTENCY_CONFLICT")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.evidence.authorization.denied")
                .tag("capability", "evidence:read")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.evidence.access.reads")
                .tag("outcome", "RECOGNIZED")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.evidence.duration")
                .tag("operation", "RECORD").timer().count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .allSatisfy(tag -> assertThat(tag.getValue())
                                .doesNotContain("ev-", "ta-", "observation", ACTOR_LIKE_VALUE)));
    }

    private static final String ACTOR_LIKE_VALUE =
            "00000000-0000-4000-8000-000000000001";
}
