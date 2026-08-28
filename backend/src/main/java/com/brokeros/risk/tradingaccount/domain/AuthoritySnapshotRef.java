package com.brokeros.risk.tradingaccount.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record AuthoritySnapshotRef(String value) {
    private static final Pattern VALID = Pattern.compile("tasv1-[0-9a-f]{64}");
    public AuthoritySnapshotRef {
        Objects.requireNonNull(value, "snapshot reference must not be null");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("snapshot reference is invalid");
        }
    }
}
