package com.brokeros.risk.actionoutcome.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record ActionOutcomeRecord(
        ActionOutcomeRef actionOutcomeRef,
        ActionRef actionRef,
        OutcomeText outcomeText,
        ActionOutcomeSource source,
        ActorRef recordedByActorRef,
        Instant recordedAt) {

    public ActionOutcomeRecord {
        Objects.requireNonNull(actionOutcomeRef, "actionOutcomeRef must not be null");
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        Objects.requireNonNull(outcomeText, "outcomeText must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(recordedByActorRef, "recordedByActorRef must not be null");
        Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        if (source != ActionOutcomeSource.MANUAL) {
            throw new IllegalArgumentException("action outcome must be manual");
        }
    }
}
