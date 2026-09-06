package com.brokeros.risk.tradingdata.application;

import java.time.Instant;

public record IngestTradingDataCommand(
        int envelopeVersion,
        String platform,
        String sourceServerId,
        long sourceSequence,
        String tradingAccountId,
        Instant occurredAt,
        byte[] payload) {

    public IngestTradingDataCommand {
        payload = payload == null ? null : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload == null ? null : payload.clone();
    }
}
