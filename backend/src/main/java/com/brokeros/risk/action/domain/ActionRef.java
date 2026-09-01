package com.brokeros.risk.action.domain;

import java.util.Objects;
import java.util.UUID;

public record ActionRef(String value) {

    public ActionRef {
        Objects.requireNonNull(value, "actionRef must not be null");
        if (!value.startsWith("act-")) {
            throw new IllegalArgumentException("actionRef has an invalid prefix");
        }
        String uuidText = value.substring(4);
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("actionRef must contain a UUID", exception);
        }
        if (uuid.version() != 4 || !uuid.toString().equals(uuidText)) {
            throw new IllegalArgumentException(
                    "actionRef must contain a canonical lowercase UUIDv4");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
