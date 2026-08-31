package com.brokeros.risk.evidence.domain;

import java.util.Objects;
import java.util.UUID;

public record EvidenceOperationId(String value) {

    public EvidenceOperationId {
        Objects.requireNonNull(value, "operationId must not be null");
        UUID uuid;
        try {
            uuid = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("operationId must be a UUID", exception);
        }
        if (uuid.version() != 4 || !uuid.toString().equals(value)) {
            throw new IllegalArgumentException(
                    "operationId must be a canonical lowercase UUIDv4");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
