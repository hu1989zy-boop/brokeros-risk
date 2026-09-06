package com.brokeros.risk.tradingdata.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.brokeros.risk.tradingdata.application.TradingDataCapabilities;
import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class TradingDataMetricsTests {

    @Test
    void metricsUseOnlyBoundedMetadataTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerTradingDataMetrics metrics = new MicrometerTradingDataMetrics(registry);

        metrics.recordOperation(TradingDataIngestionOutcome.ACCEPTED);
        metrics.recordGapDetected();
        metrics.recordAuthorizationDenied(TradingDataCapabilities.INGEST);
        metrics.recordBackpressure();
        metrics.recordDuration(Duration.ofMillis(5));

        assertThat(registry.get("brokeros.risk.tradingdata.operations")
                .tag("outcome", "ACCEPTED").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.tradingdata.gaps.detected")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.tradingdata.authorization.denied")
                .tag("capability", "trading-data:ingest")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.tradingdata.backpressure")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("brokeros.risk.tradingdata.duration")
                .timer().count()).isEqualTo(1);
        assertThat(registry.getMeters())
                .allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .allSatisfy(tag -> assertThat(tag.getValue())
                                .doesNotContain("server-1", "account-1", "payload")));
    }
}
