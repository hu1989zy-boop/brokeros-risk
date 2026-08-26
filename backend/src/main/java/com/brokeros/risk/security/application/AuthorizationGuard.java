package com.brokeros.risk.security.application;

import java.util.Objects;

import com.brokeros.risk.security.application.port.AuthorizationPort;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;

public class AuthorizationGuard {

    private final AuthorizationPort authorizationPort;

    public AuthorizationGuard(AuthorizationPort authorizationPort) {
        this.authorizationPort = Objects.requireNonNull(
                authorizationPort,
                "authorizationPort must not be null");
    }

    public AuthorizationDecision requireAllowed(
            ActorContext actorContext,
            Capability capability) {
        AuthorizationDecision decision = authorizationPort.decide(actorContext, capability);
        if (!decision.isAllowed()) {
            throw new AuthorizationDeniedException();
        }
        return decision;
    }
}
