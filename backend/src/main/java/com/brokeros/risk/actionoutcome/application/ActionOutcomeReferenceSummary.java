package com.brokeros.risk.actionoutcome.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;

public record ActionOutcomeReferenceSummary(
        ActionOutcomeRef actionOutcomeRef,
        ActionRef actionRef,
        Instant recordedAt) {

    public ActionOutcomeReferenceSummary {
        Objects.requireNonNull(actionOutcomeRef);
        Objects.requireNonNull(actionRef);
        Objects.requireNonNull(recordedAt);
    }
}
