package com.brokeros.risk.action.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record ActionRecord(
        ActionRef actionRef,
        DecisionRef decisionRef,
        IntentText intentText,
        ActionStatus status,
        ActionSource source,
        ActorRef recordedByActorRef,
        Instant recordedAt) {

    public ActionRecord {
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        Objects.requireNonNull(intentText, "intentText must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(recordedByActorRef, "recordedByActorRef must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (status != ActionStatus.PROPOSED || source != ActionSource.MANUAL) {
            throw new IllegalArgumentException("action must be manual and proposed");
        }
    }
}
