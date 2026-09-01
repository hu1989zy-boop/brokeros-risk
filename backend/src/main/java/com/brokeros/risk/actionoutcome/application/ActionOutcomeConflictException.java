package com.brokeros.risk.actionoutcome.application;

import com.brokeros.risk.api.ResultCode;

public final class ActionOutcomeConflictException extends ActionOutcomeException {

    public ActionOutcomeConflictException() {
        super(ResultCode.ACTION_OUTCOME_IDEMPOTENCY_CONFLICT);
    }
}
