package com.brokeros.risk.riskcase.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorRef;

public record Assignment(
        ActorRef assignee,
        ActorRef assignedBy,
        Instant assignedAt) {

    public Assignment {
        Objects.requireNonNull(assignee, "assignee must not be null");
        Objects.requireNonNull(assignedBy, "assignedBy must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
    }
}
