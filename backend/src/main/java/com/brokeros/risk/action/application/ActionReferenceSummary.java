package com.brokeros.risk.action.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.ActionStatus;
import com.brokeros.risk.decision.domain.DecisionRef;

public record ActionReferenceSummary(
        ActionRef actionRef,
        DecisionRef decisionRef,
        ActionStatus status,
        Instant recordedAt) {

    public ActionReferenceSummary {
        Objects.requireNonNull(actionRef);
        Objects.requireNonNull(decisionRef);
        Objects.requireNonNull(status);
        Objects.requireNonNull(recordedAt);
    }
}
