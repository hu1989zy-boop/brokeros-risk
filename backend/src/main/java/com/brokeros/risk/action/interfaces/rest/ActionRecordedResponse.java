package com.brokeros.risk.action.interfaces.rest;

import java.time.Instant;

import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionStatus;
import com.brokeros.risk.action.domain.CompletedActionOperation;

public record ActionRecordedResponse(
        String actionRef,
        String decisionRef,
        ActionStatus status,
        String recordedByActorRef,
        Instant recordedAt,
        ActionOperationOutcome outcome) {

    static ActionRecordedResponse from(CompletedActionOperation operation) {
        return new ActionRecordedResponse(
                operation.actionRef().value(),
                operation.actionRecord().decisionRef().value(),
                operation.actionRecord().status(),
                operation.actionRecord().recordedByActorRef().value(),
                operation.actionRecord().recordedAt(),
                operation.outcome());
    }
}
