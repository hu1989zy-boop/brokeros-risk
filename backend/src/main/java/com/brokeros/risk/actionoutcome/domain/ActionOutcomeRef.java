package com.brokeros.risk.actionoutcome.domain;

import java.util.Objects;
import java.util.UUID;

public record ActionOutcomeRef(String value) {

    public ActionOutcomeRef {
        Objects.requireNonNull(value, "actionOutcomeRef must not be null");
        if (!value.startsWith("aoc-")) {
            throw new IllegalArgumentException("actionOutcomeRef has an invalid prefix");
        }
        String uuidText = value.substring(4);
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("actionOutcomeRef must contain a UUID", exception);
        }
        if (uuid.version() != 4 || !uuid.toString().equals(uuidText)) {
            throw new IllegalArgumentException(
                    "actionOutcomeRef must contain a canonical lowercase UUIDv4");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
