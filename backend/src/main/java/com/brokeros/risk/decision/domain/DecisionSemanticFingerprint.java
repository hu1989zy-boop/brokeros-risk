package com.brokeros.risk.decision.domain;

import java.util.Arrays;

public final class DecisionSemanticFingerprint {

    private final byte[] value;

    public DecisionSemanticFingerprint(byte[] value) {
        if (value == null || value.length != 32) {
            throw new IllegalArgumentException("decision fingerprint must contain 32 bytes");
        }
        this.value = value.clone();
    }

    public byte[] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DecisionSemanticFingerprint fingerprint
                && Arrays.equals(value, fingerprint.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
