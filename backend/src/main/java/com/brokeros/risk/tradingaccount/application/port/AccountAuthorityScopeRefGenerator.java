package com.brokeros.risk.tradingaccount.application.port;

import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;

@FunctionalInterface
public interface AccountAuthorityScopeRefGenerator {
    AccountAuthorityScopeRef generate();
}
