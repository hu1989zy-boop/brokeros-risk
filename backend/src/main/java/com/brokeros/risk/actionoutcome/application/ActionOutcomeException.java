package com.brokeros.risk.actionoutcome.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class ActionOutcomeException extends BusinessException {

    public ActionOutcomeException(ResultCode resultCode) {
        super(resultCode);
    }

    public ActionOutcomeException(ResultCode resultCode, Throwable cause) {
        super(resultCode, resultCode.defaultMessage(), cause);
    }
}
