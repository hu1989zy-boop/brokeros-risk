package com.brokeros.risk.tradingaccount.domain;

import java.util.Arrays;

public final class ManifestFingerprint {

    private final byte[] value;

    public ManifestFingerprint(byte[] value) {
        if (value == null || value.length != 32) {
            throw new IllegalArgumentException("manifest fingerprint must contain 32 bytes");
        }
        this.value = value.clone();
    }

    public byte[] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ManifestFingerprint fingerprint
                && Arrays.equals(value, fingerprint.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "ManifestFingerprint[REDACTED]";
    }
}
