package com.brokeros.risk.tradingdata.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * ADR-024 V1 routing projection, never a persistence entity or a replacement payload.
 * Prices, amounts and source-specific optional content remain in the opaque JSON.
 * In particular, an amount without a currency is not promoted into a money model.
 */
public record CanonicalTradingDataEvent(
        int canonicalVersion,
        PlatformTag platform,
        String sourceServerId,
        EventKind eventKind,
        LifecycleHint lifecycleHint,
        String accountRef,
        String orderRef,
        String dealRef,
        String positionRef,
        Instant occurredAt,
        Long sourceTime,
        Integer serverUtcOffsetSeconds,
        Side side,
        Integer sourceSideCode,
        Reason reason,
        Integer sourceReasonCode) {

    public CanonicalTradingDataEvent {
        if (canonicalVersion != 1) {
            throw new IllegalArgumentException("Unsupported canonical version");
        }
        Objects.requireNonNull(platform);
        requireText(sourceServerId);
        Objects.requireNonNull(eventKind);
        Objects.requireNonNull(lifecycleHint);
        requireText(accountRef);
        Objects.requireNonNull(occurredAt);
        optionalRef(orderRef);
        optionalRef(dealRef);
        optionalRef(positionRef);
        if ((sourceTime == null) != (serverUtcOffsetSeconds == null)) {
            throw new IllegalArgumentException("Source time requires its UTC offset");
        }
        if (eventKind == EventKind.TRADE_ACTIVITY) {
            Objects.requireNonNull(side);
            Objects.requireNonNull(reason);
        }
        if ((side == Side.OTHER && sourceSideCode == null)
                || (reason == Reason.OTHER && sourceReasonCode == null)) {
            throw new IllegalArgumentException("OTHER requires its raw source code");
        }
    }

    private static void optionalRef(String value) {
        if (value != null) {
            requireText(value);
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required canonical identity is absent");
        }
    }

    @Override
    public String toString() {
        return "CanonicalTradingDataEvent[canonicalVersion=" + canonicalVersion
                + ", eventKind=" + eventKind + "]";
    }

    public enum EventKind { TRADE_ACTIVITY, ACCOUNT_STATE, QUOTE }

    public enum LifecycleHint { OPEN, MODIFY, CLOSE, EXECUTE, STATE }

    public enum Side {
        BUY, SELL, BUY_LIMIT, SELL_LIMIT, BUY_STOP, SELL_STOP,
        BALANCE, CREDIT, CHARGE, COMMISSION, BONUS, DIVIDEND, TAX,
        CORRECTION, INTEREST, AGENT, SO_COMPENSATION, OTHER
    }

    public enum Reason {
        CLIENT, EXPERT, DEALER, SL, TP, STOP_OUT, ROLLOVER, GATEWAY,
        SIGNAL, MOBILE, WEB, API, SETTLEMENT, TRANSFER, MIGRATION, EXTERNAL, OTHER
    }
}
