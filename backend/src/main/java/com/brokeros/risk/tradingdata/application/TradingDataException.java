package com.brokeros.risk.tradingdata.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class TradingDataException extends BusinessException {

    public TradingDataException(ResultCode resultCode) {
        super(resultCode);
    }

    public TradingDataException(ResultCode resultCode, Throwable cause) {
        super(resultCode, resultCode.defaultMessage(), cause);
    }
}
