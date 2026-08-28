package com.brokeros.risk.tradingaccount.application;

import java.time.Instant;
import java.util.Objects;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import com.brokeros.risk.tradingaccount.domain.ManifestFingerprint;

public record CompletedAuthorityOperation(
        AuthorityOperationType operationType,
        ManifestFingerprint fingerprint,
        String targetRef,
        AuthorityOperationOutcome outcome,
        long resultingVersion,
        Instant occurredAt) {
    public CompletedAuthorityOperation {
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(targetRef, "targetRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (resultingVersion < 0) throw new IllegalArgumentException("version must not be negative");
    }
}
