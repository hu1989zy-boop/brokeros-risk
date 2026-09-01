package com.brokeros.risk.action.domain;

import java.util.Arrays;

public final class ActionSemanticFingerprint {

    private final byte[] value;

    public ActionSemanticFingerprint(byte[] value) {
        if (value == null || value.length != 32) {
            throw new IllegalArgumentException("action fingerprint must contain 32 bytes");
        }
        this.value = value.clone();
    }

    public byte[] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ActionSemanticFingerprint fingerprint
                && Arrays.equals(value, fingerprint.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
