package com.brokeros.risk.tradingaccount.application;

import java.time.Clock;
import java.util.Objects;

import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;

public final class AuthorizedMutationFactory {

    private final AuthorizationGuard authorizationGuard;
    private final ManifestFingerprintFactory fingerprintFactory;
    private final Clock clock;

    public AuthorizedMutationFactory(
            AuthorizationGuard authorizationGuard,
            ManifestFingerprintFactory fingerprintFactory,
            Clock clock) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.fingerprintFactory = Objects.requireNonNull(fingerprintFactory);
        this.clock = Objects.requireNonNull(clock);
    }

    public AuthorizedMutationContext authorize(
            ActorContext actorContext,
            Capability capability,
            AuthorityOperationRequest request) {
        AuthorizationDecision decision = authorizationGuard.requireAllowed(actorContext, capability);
        return new AuthorizedMutationContext(
                fingerprintFactory.create(request),
                actorContext,
                decision,
                capability,
                clock.instant());
    }
}
