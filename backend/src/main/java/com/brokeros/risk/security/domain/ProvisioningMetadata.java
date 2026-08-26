package com.brokeros.risk.security.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public record ProvisioningMetadata(String source, String reference) {

    private static final Pattern VALID_SOURCE = Pattern.compile("[a-z][a-z0-9-]{0,31}");

    public ProvisioningMetadata {
        Objects.requireNonNull(source, "provisioning source must not be null");
        Objects.requireNonNull(reference, "provisioning reference must not be null");
        if (!VALID_SOURCE.matcher(source).matches()) {
            throw new IllegalArgumentException(
                    "provisioning source must be a lowercase controlled code");
        }
        if (reference.isBlank() || reference.length() > 128) {
            throw new IllegalArgumentException(
                    "provisioning reference must be nonblank and at most 128 characters");
        }
    }
}
