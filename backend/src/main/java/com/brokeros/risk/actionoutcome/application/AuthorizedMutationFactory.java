package com.brokeros.risk.actionoutcome.application;

import java.time.Clock;
import java.util.Objects;

import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSemanticFingerprint;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;

public final class AuthorizedMutationFactory {

    private final Clock clock;

    public AuthorizedMutationFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public AuthorizedMutationContext create(
            ActionOutcomeOperationId operationId,
            ActionOutcomeSemanticFingerprint fingerprint,
            ActorContext actorContext,
            AuthorizationDecision authorizationDecision,
            Capability capability) {
        return new AuthorizedMutationContext(
                operationId, fingerprint, actorContext,
                authorizationDecision, capability, clock.instant());
    }
}
