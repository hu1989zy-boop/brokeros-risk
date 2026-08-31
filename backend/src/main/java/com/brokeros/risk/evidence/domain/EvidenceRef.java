package com.brokeros.risk.evidence.domain;

import java.util.Objects;
import java.util.UUID;

public record EvidenceRef(String value) {

    public EvidenceRef {
        Objects.requireNonNull(value, "evidenceRef must not be null");
        if (!value.startsWith("ev-")) {
            throw new IllegalArgumentException("evidenceRef has an invalid prefix");
        }
        String uuidText = value.substring(3);
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("evidenceRef must contain a UUID", exception);
        }
        if (uuid.version() != 4 || !uuid.toString().equals(uuidText)) {
            throw new IllegalArgumentException(
                    "evidenceRef must contain a canonical lowercase UUIDv4");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
