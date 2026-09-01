package com.brokeros.risk.actionoutcome.application;

import java.util.Objects;

public record RecordActionOutcomeCommand(
        String operationId,
        String actionRef,
        String outcomeText) {

    public RecordActionOutcomeCommand {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        Objects.requireNonNull(outcomeText, "outcomeText must not be null");
    }
}
