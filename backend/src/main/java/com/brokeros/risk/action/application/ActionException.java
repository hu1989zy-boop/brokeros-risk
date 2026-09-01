package com.brokeros.risk.action.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class ActionException extends BusinessException {

    public ActionException(ResultCode resultCode) {
        super(resultCode);
    }

    public ActionException(ResultCode resultCode, Throwable cause) {
        super(resultCode, resultCode.defaultMessage(), cause);
    }
}
