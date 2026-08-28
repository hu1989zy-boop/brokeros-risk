package com.brokeros.risk.tradingaccount.application;

import com.brokeros.risk.api.ResultCode;

public final class TradingAccountAuthorityUnavailableException extends TradingAccountAuthorityException {
    public TradingAccountAuthorityUnavailableException() {
        super(ResultCode.TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE);
    }
    public TradingAccountAuthorityUnavailableException(Throwable cause) {
        super(ResultCode.TRADING_ACCOUNT_AUTHORITY_UNAVAILABLE, cause);
    }
}
