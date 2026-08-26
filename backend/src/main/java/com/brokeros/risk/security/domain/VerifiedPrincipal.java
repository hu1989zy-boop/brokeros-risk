package com.brokeros.risk.security.domain;

import java.time.Instant;
import java.util.Objects;

public record VerifiedPrincipal(
        ExternalPrincipalKey externalPrincipalKey,
        AuthenticationMethod authenticationMethod,
        Instant authenticatedAt,
        Instant credentialExpiresAt) {

    public VerifiedPrincipal {
        Objects.requireNonNull(externalPrincipalKey, "externalPrincipalKey must not be null");
        Objects.requireNonNull(authenticationMethod, "authenticationMethod must not be null");
        Objects.requireNonNull(authenticatedAt, "authenticatedAt must not be null");

        if (authenticationMethod == AuthenticationMethod.SIGNED_JWT) {
            if (externalPrincipalKey.principalType() != ActorType.HUMAN) {
                throw new IllegalArgumentException("signed JWT principal must be HUMAN");
            }
            Objects.requireNonNull(
                    credentialExpiresAt,
                    "credentialExpiresAt is required for signed JWT authentication");
        } else {
            if (externalPrincipalKey.principalType() != ActorType.SERVICE) {
                throw new IllegalArgumentException("trusted in-process principal must be SERVICE");
            }
            if (credentialExpiresAt != null) {
                throw new IllegalArgumentException(
                        "trusted in-process principal must not carry credential expiry");
            }
        }
    }

    public ActorType principalType() {
        return externalPrincipalKey.principalType();
    }
}
