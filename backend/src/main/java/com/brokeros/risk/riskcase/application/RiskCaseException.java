package com.brokeros.risk.riskcase.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class RiskCaseException extends BusinessException {

    public RiskCaseException(ResultCode resultCode) {
        super(resultCode);
    }

    public RiskCaseException(ResultCode resultCode, Throwable cause) {
        super(resultCode, resultCode.defaultMessage(), cause);
    }
}
