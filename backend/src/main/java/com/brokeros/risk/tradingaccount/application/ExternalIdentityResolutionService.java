package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;
import java.util.Optional;

import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityQueryPort;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountIdentity;

public final class ExternalIdentityResolutionService {
    private final AuthorizationGuard authorizationGuard;
    private final TradingAccountAuthorityQueryPort queryPort;

    public ExternalIdentityResolutionService(
            AuthorizationGuard authorizationGuard,
            TradingAccountAuthorityQueryPort queryPort) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
    }

    public Optional<TradingAccountState> resolve(
            ActorContext actorContext,
            ExternalAccountIdentity identity) {
        authorizationGuard.requireAllowed(actorContext, TradingAccountCapabilities.READ);
        return queryPort.findByExternalIdentity(identity);
    }
}
