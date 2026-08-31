package com.brokeros.risk.evidence.domain;

import java.util.Arrays;

public final class EvidenceFingerprint {

    private final byte[] value;

    public EvidenceFingerprint(byte[] value) {
        if (value == null || value.length != 32) {
            throw new IllegalArgumentException("evidence fingerprint must contain 32 bytes");
        }
        this.value = value.clone();
    }

    public byte[] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EvidenceFingerprint fingerprint
                && Arrays.equals(value, fingerprint.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }
}
