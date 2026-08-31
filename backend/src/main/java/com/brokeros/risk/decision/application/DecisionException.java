package com.brokeros.risk.decision.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class DecisionException extends BusinessException {

    public DecisionException(ResultCode resultCode) {
        super(resultCode);
    }

    public DecisionException(ResultCode resultCode, Throwable cause) {
        super(resultCode, resultCode.defaultMessage(), cause);
    }
}
