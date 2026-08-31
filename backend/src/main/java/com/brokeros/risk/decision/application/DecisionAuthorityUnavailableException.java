package com.brokeros.risk.decision.application;

import com.brokeros.risk.api.ResultCode;

public final class DecisionAuthorityUnavailableException extends DecisionException {

    public DecisionAuthorityUnavailableException() {
        super(ResultCode.DECISION_AUTHORITY_UNAVAILABLE);
    }

    public DecisionAuthorityUnavailableException(Throwable cause) {
        super(ResultCode.DECISION_AUTHORITY_UNAVAILABLE, cause);
    }
}
