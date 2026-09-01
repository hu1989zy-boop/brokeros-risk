package com.brokeros.risk.action.application;

import java.util.Objects;

public record RecordActionCommand(
        String operationId,
        String decisionRef,
        String intentText) {

    public RecordActionCommand {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        Objects.requireNonNull(intentText, "intentText must not be null");
    }
}
