package com.brokeros.risk.action.application;

import com.brokeros.risk.api.ResultCode;

public final class ActionConflictException extends ActionException {

    public ActionConflictException() {
        super(ResultCode.ACTION_IDEMPOTENCY_CONFLICT);
    }
}
