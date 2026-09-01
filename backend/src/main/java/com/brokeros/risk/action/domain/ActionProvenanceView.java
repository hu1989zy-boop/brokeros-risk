package com.brokeros.risk.action.domain;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;

public record ActionProvenanceView(
        ActionRef actionRef,
        ActionProvenanceOutcome outcome,
        DecisionRef decisionRef,
        ActionStatus status,
        ActorRef recordedByActorRef,
        Instant recordedAt) {

    public ActionProvenanceView {
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        boolean complete = decisionRef != null && status != null
                && recordedByActorRef != null && recordedAt != null;
        if (outcome == ActionProvenanceOutcome.RECOGNIZED && !complete) {
            throw new IllegalArgumentException("recognized provenance must be complete");
        }
        if (outcome == ActionProvenanceOutcome.NOT_FOUND
                && (decisionRef != null || status != null
                || recordedByActorRef != null || recordedAt != null)) {
            throw new IllegalArgumentException(
                    "not-found provenance cannot disclose metadata");
        }
    }

    public static ActionProvenanceView recognized(ActionRecord record) {
        return new ActionProvenanceView(
                record.actionRef(), ActionProvenanceOutcome.RECOGNIZED,
                record.decisionRef(), record.status(),
                record.recordedByActorRef(), record.recordedAt());
    }

    public static ActionProvenanceView notFound(ActionRef ref) {
        return new ActionProvenanceView(
                ref, ActionProvenanceOutcome.NOT_FOUND,
                null, null, null, null);
    }
}
