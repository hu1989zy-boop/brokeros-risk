package com.brokeros.risk.security.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record Capability(String value) implements Comparable<Capability> {

    private static final Pattern VALID_CAPABILITY =
            Pattern.compile("^[a-z][a-z0-9-]{0,62}:[a-z][a-z0-9-]{0,62}$");

    public Capability {
        Objects.requireNonNull(value, "capability must not be null");
        if (value.length() > 127 || !VALID_CAPABILITY.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "capability must use exact lowercase <module>:<action> syntax");
        }
    }

    @Override
    public int compareTo(Capability other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
