package com.brokeros.risk.security.application;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.ProvisioningMetadata;

public record ProvisioningManifest(
        ProvisioningMetadata metadata,
        List<ActorProvisioningSpec> actors) {

    public ProvisioningManifest {
        Objects.requireNonNull(metadata, "metadata must not be null");
        actors = List.copyOf(Objects.requireNonNull(actors, "actors must not be null"));
        if (actors.isEmpty()) {
            throw new IllegalArgumentException("manifest must contain at least one actor");
        }

        Set<ExternalPrincipalKey> seenKeys = new HashSet<>();
        for (ActorProvisioningSpec actor : actors) {
            for (ExternalPrincipalKey key : actor.principalKeys()) {
                if (!seenKeys.add(key)) {
                    throw new IllegalArgumentException(
                            "a principal mapping may belong to only one manifest actor");
                }
            }
        }
    }
}
