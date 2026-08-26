package com.brokeros.risk.security.domain;

public enum AuthorizationReason {
    EXPLICIT_GRANT,
    ACTOR_INACTIVE,
    CAPABILITY_NOT_GRANTED,
    CAPABILITY_REVOKED
}
