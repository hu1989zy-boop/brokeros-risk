package com.brokeros.risk.tradingaccount.application.port;

import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

@FunctionalInterface
public interface TradingAccountRefGenerator {
    TradingAccountRef generate();
}
