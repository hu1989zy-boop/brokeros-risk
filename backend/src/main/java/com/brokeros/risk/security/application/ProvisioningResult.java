package com.brokeros.risk.security.application;

public record ProvisioningResult(int createdActors, int unchangedActors) {

    public ProvisioningResult {
        if (createdActors < 0 || unchangedActors < 0) {
            throw new IllegalArgumentException("provisioning counts must not be negative");
        }
    }
}
