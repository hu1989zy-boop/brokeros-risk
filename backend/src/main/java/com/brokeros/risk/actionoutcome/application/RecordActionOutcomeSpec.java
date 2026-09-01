package com.brokeros.risk.actionoutcome.application;

import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.OutcomeText;

public record RecordActionOutcomeSpec(
        ActionOutcomeOperationId operationId,
        ActionRef actionRef,
        OutcomeText outcomeText) {

    public RecordActionOutcomeSpec {
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        Objects.requireNonNull(outcomeText, "outcomeText must not be null");
    }
}
