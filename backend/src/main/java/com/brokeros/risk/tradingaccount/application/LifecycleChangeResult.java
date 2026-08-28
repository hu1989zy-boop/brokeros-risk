package com.brokeros.risk.tradingaccount.application;

import java.time.Instant;
import java.util.Objects;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;

public record LifecycleChangeResult(
        String targetRef,
        AuthorityLifecycle lifecycle,
        AuthorityOperationOutcome outcome,
        long resultingVersion,
        Instant occurredAt) {
    public LifecycleChangeResult {
        Objects.requireNonNull(targetRef, "targetRef must not be null");
        Objects.requireNonNull(lifecycle, "lifecycle must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (resultingVersion < 0) throw new IllegalArgumentException("version must not be negative");
    }
}
