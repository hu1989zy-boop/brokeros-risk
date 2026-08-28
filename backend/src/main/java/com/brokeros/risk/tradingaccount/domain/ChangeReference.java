package com.brokeros.risk.tradingaccount.domain;

import java.util.Objects;

public record ChangeReference(String value) {
    public ChangeReference {
        Objects.requireNonNull(value, "change reference must not be null");
        if (!AttestationReference.isSafe(value, 128, 512)) {
            throw new IllegalArgumentException("change reference is invalid");
        }
    }
}
