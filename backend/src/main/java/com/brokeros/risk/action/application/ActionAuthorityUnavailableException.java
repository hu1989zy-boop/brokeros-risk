package com.brokeros.risk.action.application;

import com.brokeros.risk.api.ResultCode;

public final class ActionAuthorityUnavailableException extends ActionException {

    public ActionAuthorityUnavailableException() {
        super(ResultCode.ACTION_AUTHORITY_UNAVAILABLE);
    }

    public ActionAuthorityUnavailableException(Throwable cause) {
        super(ResultCode.ACTION_AUTHORITY_UNAVAILABLE, cause);
    }
}
