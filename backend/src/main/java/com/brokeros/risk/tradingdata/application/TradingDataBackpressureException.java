package com.brokeros.risk.tradingdata.application;

import com.brokeros.risk.api.ResultCode;

public final class TradingDataBackpressureException extends TradingDataException {

    public TradingDataBackpressureException(Throwable cause) {
        super(ResultCode.TRADING_DATA_BACKPRESSURE, cause);
    }
}
