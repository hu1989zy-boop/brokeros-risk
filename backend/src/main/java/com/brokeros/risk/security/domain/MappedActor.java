package com.brokeros.risk.security.domain;

import java.util.Objects;

public record MappedActor(ActorRef actorRef, ActorType actorType, long actorVersion) {

    public MappedActor {
        Objects.requireNonNull(actorRef, "actorRef must not be null");
        Objects.requireNonNull(actorType, "actorType must not be null");
        if (actorVersion < 0) {
            throw new IllegalArgumentException("actorVersion must not be negative");
        }
    }
}
