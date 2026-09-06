package com.brokeros.risk.tradingdata.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record TradingDataEnvelope(
        int envelopeVersion,
        PlatformTag platform,
        String sourceServerId,
        long sourceSequence,
        String tradingAccountId,
        Instant occurredAt,
        byte[] payload) {

    public static final int MAX_PAYLOAD_BYTES = 65_535;
    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public TradingDataEnvelope {
        if (envelopeVersion < 1) {
            throw new IllegalArgumentException("envelopeVersion must be at least 1");
        }
        Objects.requireNonNull(platform, "platform must not be null");
        requireIdentifier(sourceServerId, "sourceServerId");
        if (sourceSequence < 0) {
            throw new IllegalArgumentException("sourceSequence must not be negative");
        }
        requireIdentifier(tradingAccountId, "tradingAccountId");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (payload.length < 1 || payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("payload length must be between 1 and 65535 bytes");
        }
        payload = payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    private static void requireIdentifier(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must be a safe identifier of at most 128 characters");
        }
    }
}
