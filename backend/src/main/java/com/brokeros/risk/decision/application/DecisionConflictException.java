package com.brokeros.risk.decision.application;

import com.brokeros.risk.api.ResultCode;

public final class DecisionConflictException extends DecisionException {

    public DecisionConflictException() {
        super(ResultCode.DECISION_IDEMPOTENCY_CONFLICT);
    }
}
