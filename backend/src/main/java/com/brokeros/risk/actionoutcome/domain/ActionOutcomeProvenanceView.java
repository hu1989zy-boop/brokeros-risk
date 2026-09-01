package com.brokeros.risk.actionoutcome.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record ActionOutcomeProvenanceView(
        ActionOutcomeRef actionOutcomeRef,
        ActionOutcomeProvenanceOutcome outcome,
        ActionRef actionRef,
        ActorRef recordedByActorRef,
        Instant recordedAt) {

    public ActionOutcomeProvenanceView {
        Objects.requireNonNull(actionOutcomeRef, "actionOutcomeRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        boolean complete = actionRef != null
                && recordedByActorRef != null && recordedAt != null;
        if (outcome == ActionOutcomeProvenanceOutcome.RECOGNIZED && !complete) {
            throw new IllegalArgumentException("recognized provenance must be complete");
        }
        if (outcome == ActionOutcomeProvenanceOutcome.NOT_FOUND
                && (actionRef != null || recordedByActorRef != null || recordedAt != null)) {
            throw new IllegalArgumentException(
                    "not-found provenance cannot disclose metadata");
        }
    }

    public static ActionOutcomeProvenanceView recognized(ActionOutcomeRecord record) {
        return new ActionOutcomeProvenanceView(
                record.actionOutcomeRef(), ActionOutcomeProvenanceOutcome.RECOGNIZED,
                record.actionRef(), record.recordedByActorRef(), record.recordedAt());
    }

    public static ActionOutcomeProvenanceView notFound(ActionOutcomeRef ref) {
        return new ActionOutcomeProvenanceView(
                ref, ActionOutcomeProvenanceOutcome.NOT_FOUND,
                null, null, null);
    }
}
