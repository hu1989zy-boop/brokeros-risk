package com.brokeros.risk.tradingaccount.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record SourceNamespace(
        String sourceFamily,
        String sourceInstance,
        String sourceServer,
        String sourceEnvironment) {

    private static final Pattern FAMILY = Pattern.compile("[a-z][a-z0-9-]{0,62}");
    private static final Pattern SERVER = Pattern.compile("[a-z0-9][a-z0-9._-]{0,127}");
    private static final Pattern ENVIRONMENT = Pattern.compile("[a-z][a-z0-9-]{0,31}");

    public SourceNamespace {
        Objects.requireNonNull(sourceFamily, "sourceFamily must not be null");
        Objects.requireNonNull(sourceInstance, "sourceInstance must not be null");
        Objects.requireNonNull(sourceServer, "sourceServer must not be null");
        Objects.requireNonNull(sourceEnvironment, "sourceEnvironment must not be null");
        if (!FAMILY.matcher(sourceFamily).matches()
                || !FAMILY.matcher(sourceInstance).matches()
                || !SERVER.matcher(sourceServer).matches()
                || !ENVIRONMENT.matcher(sourceEnvironment).matches()) {
            throw new IllegalArgumentException("source namespace is invalid");
        }
    }
}
