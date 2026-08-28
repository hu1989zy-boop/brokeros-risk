package com.brokeros.risk.tradingaccount.application;

import com.brokeros.risk.security.domain.Capability;

public final class TradingAccountCapabilities {

    public static final Capability READ =
            new Capability("trading-account-reference:read");
    public static final Capability REGISTER =
            new Capability("trading-account-reference:register");
    public static final Capability CHANGE_LIFECYCLE =
            new Capability("trading-account-reference:change-lifecycle");

    private TradingAccountCapabilities() {
    }
}
