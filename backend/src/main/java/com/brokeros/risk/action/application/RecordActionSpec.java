package com.brokeros.risk.action.application;

import java.util.Objects;

import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.IntentText;
import com.brokeros.risk.decision.domain.DecisionRef;

public record RecordActionSpec(
        ActionOperationId operationId,
        DecisionRef decisionRef,
        IntentText intentText) {

    public RecordActionSpec {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        Objects.requireNonNull(intentText, "intentText must not be null");
    }
}
