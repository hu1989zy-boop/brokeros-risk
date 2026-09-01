package com.brokeros.risk.actionoutcome.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;

public record ActionOutcomeRecordedResponse(
        String actionOutcomeRef,
        String actionRef,
        String recordedByActorRef,
        Instant recordedAt,
        ActionOutcomeOperationOutcome outcome) {

    static ActionOutcomeRecordedResponse from(
            CompletedActionOutcomeOperation operation) {
        return new ActionOutcomeRecordedResponse(
                operation.actionOutcomeRef().value(),
                operation.actionOutcomeRecord().actionRef().value(),
                operation.actionOutcomeRecord().recordedByActorRef().value(),
                operation.actionOutcomeRecord().recordedAt(),
                operation.outcome());
    }
}
