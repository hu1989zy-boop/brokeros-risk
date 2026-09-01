package com.brokeros.risk.actionoutcome.application;

import com.brokeros.risk.api.ResultCode;

public final class ActionOutcomeAuthorityUnavailableException
        extends ActionOutcomeException {

    public ActionOutcomeAuthorityUnavailableException() {
        super(ResultCode.ACTION_OUTCOME_AUTHORITY_UNAVAILABLE);
    }

    public ActionOutcomeAuthorityUnavailableException(Throwable cause) {
        super(ResultCode.ACTION_OUTCOME_AUTHORITY_UNAVAILABLE, cause);
    }
}
