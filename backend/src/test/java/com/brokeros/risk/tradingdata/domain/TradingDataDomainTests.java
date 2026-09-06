package com.brokeros.risk.tradingdata.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TradingDataDomainTests {

    @Test
    void envelopePreservesArbitraryOpaqueBytesAndDefendsItsBoundary() {
        byte[] arbitrary = {(byte) 0xff, 0x00, (byte) 0xc3, 0x28};
        TradingDataEnvelope envelope = envelope(arbitrary);

        arbitrary[0] = 0x01;
        assertThat(envelope.payload())
                .containsExactly((byte) 0xff, 0x00, (byte) 0xc3, 0x28);
        byte[] returned = envelope.payload();
        returned[1] = 0x01;
        assertThat(envelope.payload())
                .containsExactly((byte) 0xff, 0x00, (byte) 0xc3, 0x28);
    }

    @Test
    void envelopeRejectsInvalidMetadataAndPayloadSizeWithoutInspectingPayload() {
        assertThatThrownBy(() -> new TradingDataEnvelope(
                0, PlatformTag.MT4, "server-1", 0, "account-1",
                Instant.EPOCH, new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TradingDataEnvelope(
                1, PlatformTag.MT4, "server 1", 0, "account-1",
                Instant.EPOCH, new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TradingDataEnvelope(
                1, PlatformTag.MT4, "server-1", -1, "account-1",
                Instant.EPOCH, new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envelope(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> envelope(
                new byte[TradingDataEnvelope.MAX_PAYLOAD_BYTES + 1]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private TradingDataEnvelope envelope(byte[] payload) {
        return new TradingDataEnvelope(
                1, PlatformTag.MT5, "server-1", 7, "account-1",
                Instant.parse("2026-09-06T00:00:00Z"), payload);
    }
}
