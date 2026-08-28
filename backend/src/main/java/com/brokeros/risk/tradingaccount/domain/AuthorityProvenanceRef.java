package com.brokeros.risk.tradingaccount.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record AuthorityProvenanceRef(String value) {
    private static final Pattern VALID = Pattern.compile("tapv1-[0-9a-f]{64}");
    public AuthorityProvenanceRef {
        Objects.requireNonNull(value, "provenance reference must not be null");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("provenance reference is invalid");
        }
    }
}
