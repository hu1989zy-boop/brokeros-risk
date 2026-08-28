package com.brokeros.risk.tradingaccount.domain;

import java.util.Objects;

public record ChangeReason(String value) {
    public ChangeReason {
        Objects.requireNonNull(value, "change reason must not be null");
        if (!AttestationReference.isSafe(value, 256, 1024)) {
            throw new IllegalArgumentException("change reason is invalid");
        }
    }
}
