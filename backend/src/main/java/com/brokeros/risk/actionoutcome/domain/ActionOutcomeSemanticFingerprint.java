package com.brokeros.risk.actionoutcome.domain;

import java.util.Arrays;

public final class ActionOutcomeSemanticFingerprint {

    private final byte[] value;

    public ActionOutcomeSemanticFingerprint(byte[] value) {
        if (value == null || value.length != 32) {
            throw new IllegalArgumentException(
                    "action outcome fingerprint must contain 32 bytes");
        }
        this.value = value.clone();
    }

    public byte[] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ActionOutcomeSemanticFingerprint fingerprint
                && Arrays.equals(value, fingerprint.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
