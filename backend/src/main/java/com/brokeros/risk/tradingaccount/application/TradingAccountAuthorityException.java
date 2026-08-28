package com.brokeros.risk.tradingaccount.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class TradingAccountAuthorityException extends BusinessException {

    public TradingAccountAuthorityException(ResultCode resultCode) {
        super(resultCode);
    }

    public TradingAccountAuthorityException(ResultCode resultCode, Throwable cause) {
        super(resultCode, resultCode.defaultMessage(), cause);
    }
}
