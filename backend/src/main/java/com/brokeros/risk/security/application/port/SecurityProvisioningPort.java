package com.brokeros.risk.security.application.port;

import java.time.Instant;

import com.brokeros.risk.security.application.ProvisioningManifest;
import com.brokeros.risk.security.application.ProvisioningResult;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorStatus;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.CapabilityStatus;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.domain.PrincipalMappingStatus;
import com.brokeros.risk.security.domain.ProvisioningMetadata;

public interface SecurityProvisioningPort {

    ProvisioningResult provision(ProvisioningManifest manifest, Instant occurredAt);

    long changeActorStatus(
            ActorRef actorRef,
            long expectedVersion,
            ActorStatus status,
            ProvisioningMetadata metadata,
            Instant occurredAt);

    long changeMappingStatus(
            ExternalPrincipalKey principalKey,
            long expectedVersion,
            PrincipalMappingStatus status,
            ProvisioningMetadata metadata,
            Instant occurredAt);

    long changeCapabilityStatus(
            ActorRef actorRef,
            Capability capability,
            long expectedVersion,
            CapabilityStatus status,
            ProvisioningMetadata metadata,
            Instant occurredAt);
}
