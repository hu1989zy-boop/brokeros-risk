package com.brokeros.risk.riskcase.domain;

import java.util.Objects;
import java.util.UUID;

final class RiskCaseIdentifiers {

    private RiskCaseIdentifiers() {
    }

    static String canonicalUuidV4(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a canonical UUIDv4", exception);
        }
        if (parsed.version() != 4 || !parsed.toString().equals(value)) {
            throw new IllegalArgumentException(name + " must be a canonical lowercase UUIDv4");
        }
        return value;
    }
}
