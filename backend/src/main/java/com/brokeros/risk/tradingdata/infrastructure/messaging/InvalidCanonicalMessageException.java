package com.brokeros.risk.tradingdata.infrastructure.messaging;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.tradingdata.application.TradingDataException;

/** Deliberately carries no parser cause, input text, key, or payload. */
public final class InvalidCanonicalMessageException extends TradingDataException {
    public InvalidCanonicalMessageException() {
        super(ResultCode.TRADING_DATA_REQUEST_INVALID);
    }
}
