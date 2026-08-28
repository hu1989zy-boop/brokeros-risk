package com.brokeros.risk.tradingaccount.application;

import com.brokeros.risk.api.ResultCode;

public final class TradingAccountConflictException extends TradingAccountAuthorityException {
    public TradingAccountConflictException(ResultCode resultCode) {
        super(resultCode);
        if (resultCode != ResultCode.TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT
                && resultCode != ResultCode.TRADING_ACCOUNT_MAPPING_CONFLICT
                && resultCode != ResultCode.TRADING_ACCOUNT_VERSION_CONFLICT
                && resultCode != ResultCode.TRADING_ACCOUNT_INVALID_TRANSITION) {
            throw new IllegalArgumentException("result code is not a Q-010 conflict");
        }
    }
}
