package com.brokeros.risk.security.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;

public record ActorProvisioningSpec(
        ActorType actorType,
        List<ExternalPrincipalKey> principalKeys,
        Set<Capability> capabilities) {

    public ActorProvisioningSpec {
        Objects.requireNonNull(actorType, "actorType must not be null");
        principalKeys = List.copyOf(
                Objects.requireNonNull(principalKeys, "principalKeys must not be null"));
        capabilities = Set.copyOf(
                Objects.requireNonNull(capabilities, "capabilities must not be null"));

        if (principalKeys.isEmpty()) {
            throw new IllegalArgumentException("at least one principal mapping is required");
        }
        if (new LinkedHashSet<>(principalKeys).size() != principalKeys.size()) {
            throw new IllegalArgumentException("principal mappings must be unique");
        }
        if (principalKeys.stream().anyMatch(key -> key.principalType() != actorType)) {
            throw new IllegalArgumentException("principal mapping type must match actor type");
        }
    }
}
