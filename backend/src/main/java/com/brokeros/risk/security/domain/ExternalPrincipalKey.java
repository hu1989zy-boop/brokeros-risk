package com.brokeros.risk.security.domain;

import java.util.Objects;

public record ExternalPrincipalKey(String issuer, String subject, ActorType principalType) {

    private static final int MAX_COMPONENT_LENGTH = 255;

    public ExternalPrincipalKey {
        requireExactComponent(issuer, "issuer");
        requireExactComponent(subject, "subject");
        Objects.requireNonNull(principalType, "principalType must not be null");
    }

    private static void requireExactComponent(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        if (value.length() > MAX_COMPONENT_LENGTH) {
            throw new IllegalArgumentException(name + " must not exceed 255 characters");
        }
    }
}
