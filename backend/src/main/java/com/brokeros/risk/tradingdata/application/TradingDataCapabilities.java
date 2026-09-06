package com.brokeros.risk.tradingdata.application;

import com.brokeros.risk.security.domain.Capability;

public final class TradingDataCapabilities {

    public static final Capability INGEST = new Capability("trading-data:ingest");

    private TradingDataCapabilities() {
    }
}
