package com.brokeros.risk.security.domain;

import java.util.Objects;
import java.util.UUID;

public record ActorRef(String value) {

    public ActorRef {
        Objects.requireNonNull(value, "actorRef must not be null");
        UUID parsed;
        try {
            parsed = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("actorRef must be a canonical UUIDv4", exception);
        }
        if (parsed.version() != 4 || !parsed.toString().equals(value)) {
            throw new IllegalArgumentException("actorRef must be a canonical lowercase UUIDv4");
        }
    }

    public static ActorRef generate() {
        return new ActorRef(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
