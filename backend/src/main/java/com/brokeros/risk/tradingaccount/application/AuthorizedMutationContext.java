package com.brokeros.risk.tradingaccount.application;

import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.tradingaccount.domain.ManifestFingerprint;

public record AuthorizedMutationContext(
        ManifestFingerprint fingerprint,
        ActorContext actorContext,
        AuthorizationDecision authorizationDecision,
        Capability capability,
        Instant occurredAt) {

    public AuthorizedMutationContext {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(authorizationDecision, "authorizationDecision must not be null");
        Objects.requireNonNull(capability, "capability must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (!authorizationDecision.isAllowed()
                || !actorContext.actorRef().equals(authorizationDecision.actorRef())
                || !capability.equals(authorizationDecision.capability())) {
            throw new IllegalArgumentException("mutation context requires the exact allow decision");
        }
    }
}
