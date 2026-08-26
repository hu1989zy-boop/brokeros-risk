package com.brokeros.risk.security.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public record ActorContext(
        ActorRef actorRef,
        ActorType actorType,
        ExternalPrincipalKey externalPrincipalKey,
        AuthenticationMethod authenticationMethod,
        Instant authenticatedAt,
        Instant credentialExpiresAt,
        UUID executionId,
        String requestId,
        String traceId) {

    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,128}");
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[0-9a-f]{32}");

    public ActorContext {
        Objects.requireNonNull(actorRef, "actorRef must not be null");
        Objects.requireNonNull(actorType, "actorType must not be null");
        Objects.requireNonNull(externalPrincipalKey, "externalPrincipalKey must not be null");
        Objects.requireNonNull(authenticationMethod, "authenticationMethod must not be null");
        Objects.requireNonNull(authenticatedAt, "authenticatedAt must not be null");
        Objects.requireNonNull(executionId, "executionId must not be null");

        if (actorType != externalPrincipalKey.principalType()) {
            throw new IllegalArgumentException("actor type must match principal type");
        }
        if (executionId.version() != 4) {
            throw new IllegalArgumentException("executionId must be a UUIDv4");
        }
        if (authenticationMethod == AuthenticationMethod.SIGNED_JWT
                && credentialExpiresAt == null) {
            throw new IllegalArgumentException(
                    "credentialExpiresAt is required for signed JWT context");
        }
        if (authenticationMethod == AuthenticationMethod.TRUSTED_IN_PROCESS
                && credentialExpiresAt != null) {
            throw new IllegalArgumentException(
                    "trusted in-process context must not carry credential expiry");
        }
        if (requestId != null && !VALID_REQUEST_ID.matcher(requestId).matches()) {
            throw new IllegalArgumentException("requestId is not a safe correlation value");
        }
        if (traceId != null && !VALID_TRACE_ID.matcher(traceId).matches()) {
            throw new IllegalArgumentException("traceId is not a W3C trace identifier");
        }
    }
}
