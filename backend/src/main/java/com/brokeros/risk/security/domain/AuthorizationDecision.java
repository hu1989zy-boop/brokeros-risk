package com.brokeros.risk.security.domain;

import java.time.Instant;
import java.util.Objects;

public record AuthorizationDecision(
        AuthorizationOutcome outcome,
        ActorRef actorRef,
        Capability capability,
        AuthorizationReason reason,
        Instant evaluatedAt,
        Long actorVersion,
        Long grantVersion) {

    public AuthorizationDecision {
        Objects.requireNonNull(outcome, "outcome must not be null");
        Objects.requireNonNull(actorRef, "actorRef must not be null");
        Objects.requireNonNull(capability, "capability must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        if ((actorVersion != null && actorVersion < 0)
                || (grantVersion != null && grantVersion < 0)) {
            throw new IllegalArgumentException("observed versions must not be negative");
        }
        if (outcome == AuthorizationOutcome.ALLOW
                && (reason != AuthorizationReason.EXPLICIT_GRANT
                || actorVersion == null
                || grantVersion == null)) {
            throw new IllegalArgumentException("allow requires an explicit versioned grant");
        }
        if (outcome == AuthorizationOutcome.DENY
                && reason == AuthorizationReason.EXPLICIT_GRANT) {
            throw new IllegalArgumentException("deny cannot use explicit grant reason");
        }
    }

    public static AuthorizationDecision allow(
            ActorRef actorRef,
            Capability capability,
            Instant evaluatedAt,
            long actorVersion,
            long grantVersion) {
        return new AuthorizationDecision(
                AuthorizationOutcome.ALLOW,
                actorRef,
                capability,
                AuthorizationReason.EXPLICIT_GRANT,
                evaluatedAt,
                actorVersion,
                grantVersion);
    }

    public static AuthorizationDecision deny(
            ActorRef actorRef,
            Capability capability,
            AuthorizationReason reason,
            Instant evaluatedAt,
            Long actorVersion,
            Long grantVersion) {
        return new AuthorizationDecision(
                AuthorizationOutcome.DENY,
                actorRef,
                capability,
                reason,
                evaluatedAt,
                actorVersion,
                grantVersion);
    }

    public boolean isAllowed() {
        return outcome == AuthorizationOutcome.ALLOW;
    }
}
