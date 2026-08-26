package com.brokeros.risk.security.application;

import java.time.Clock;
import java.util.Objects;

import com.brokeros.risk.security.application.port.SecurityProvisioningPort;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorStatus;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.CapabilityStatus;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.PrincipalMappingStatus;
import com.brokeros.risk.security.domain.ProvisioningMetadata;

public class ActorProvisioningService {

    private final SecurityProvisioningPort provisioningPort;
    private final Clock clock;

    public ActorProvisioningService(SecurityProvisioningPort provisioningPort, Clock clock) {
        this.provisioningPort = Objects.requireNonNull(
                provisioningPort,
                "provisioningPort must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ProvisioningResult provision(ProvisioningManifest manifest) {
        Objects.requireNonNull(manifest, "manifest must not be null");
        return provisioningPort.provision(manifest, clock.instant());
    }

    public long disableActor(
            ActorRef actorRef,
            long expectedVersion,
            ProvisioningMetadata metadata) {
        return provisioningPort.changeActorStatus(
                actorRef,
                expectedVersion,
                ActorStatus.DISABLED,
                metadata,
                clock.instant());
    }

    public long reactivateActor(
            ActorRef actorRef,
            long expectedVersion,
            ProvisioningMetadata metadata) {
        return provisioningPort.changeActorStatus(
                actorRef,
                expectedVersion,
                ActorStatus.ACTIVE,
                metadata,
                clock.instant());
    }

    public long disableMapping(
            ExternalPrincipalKey principalKey,
            long expectedVersion,
            ProvisioningMetadata metadata) {
        return provisioningPort.changeMappingStatus(
                principalKey,
                expectedVersion,
                PrincipalMappingStatus.DISABLED,
                metadata,
                clock.instant());
    }

    public long reactivateMapping(
            ExternalPrincipalKey principalKey,
            long expectedVersion,
            ProvisioningMetadata metadata) {
        return provisioningPort.changeMappingStatus(
                principalKey,
                expectedVersion,
                PrincipalMappingStatus.ACTIVE,
                metadata,
                clock.instant());
    }

    public long revokeCapability(
            ActorRef actorRef,
            Capability capability,
            long expectedVersion,
            ProvisioningMetadata metadata) {
        return provisioningPort.changeCapabilityStatus(
                actorRef,
                capability,
                expectedVersion,
                CapabilityStatus.REVOKED,
                metadata,
                clock.instant());
    }

    public long regrantCapability(
            ActorRef actorRef,
            Capability capability,
            long expectedVersion,
            ProvisioningMetadata metadata) {
        return provisioningPort.changeCapabilityStatus(
                actorRef,
                capability,
                expectedVersion,
                CapabilityStatus.GRANTED,
                metadata,
                clock.instant());
    }
}
