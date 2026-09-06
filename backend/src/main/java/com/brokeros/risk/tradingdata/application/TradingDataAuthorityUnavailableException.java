package com.brokeros.risk.tradingdata.application;

import com.brokeros.risk.api.ResultCode;

public final class TradingDataAuthorityUnavailableException extends TradingDataException {

    public TradingDataAuthorityUnavailableException(Throwable cause) {
        super(ResultCode.TRADING_DATA_AUTHORITY_UNAVAILABLE, cause);
    }
}
