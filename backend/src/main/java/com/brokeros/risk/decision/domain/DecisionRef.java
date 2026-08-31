package com.brokeros.risk.decision.domain;

import java.util.Objects;
import java.util.UUID;

public record DecisionRef(String value) {

    public DecisionRef {
        Objects.requireNonNull(value, "decisionRef must not be null");
        if (!value.startsWith("dec-")) {
            throw new IllegalArgumentException("decisionRef has an invalid prefix");
        }
        String uuidText = value.substring(4);
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("decisionRef must contain a UUID", exception);
        }
        if (uuid.version() != 4 || !uuid.toString().equals(uuidText)) {
            throw new IllegalArgumentException(
                    "decisionRef must contain a canonical lowercase UUIDv4");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
