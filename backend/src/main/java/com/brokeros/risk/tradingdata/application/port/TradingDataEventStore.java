package com.brokeros.risk.tradingdata.application.port;

import java.time.Instant;
import java.util.OptionalLong;

import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;

public interface TradingDataEventStore {

    OptionalLong lastContiguousSequence(String sourceServerId);

    TradingDataAppendOutcome append(TradingDataEnvelope envelope, Instant receivedAt);

    void recordGap(
            String sourceServerId,
            long fromSequence,
            long toSequence,
            Instant detectedAt);
}
