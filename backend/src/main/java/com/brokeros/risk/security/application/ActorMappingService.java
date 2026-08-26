package com.brokeros.risk.security.application;

import java.util.UUID;

import com.brokeros.risk.security.application.port.ActorMappingPort;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.MappedActor;
import com.brokeros.risk.security.domain.VerifiedPrincipal;

public class ActorMappingService {

    private final ActorMappingPort actorMappingPort;

    public ActorMappingService(ActorMappingPort actorMappingPort) {
        this.actorMappingPort = actorMappingPort;
    }

    public ActorContext createContext(
            VerifiedPrincipal verifiedPrincipal,
            String requestId,
            String traceId) {
        MappedActor actor = actorMappingPort.resolveActiveActor(verifiedPrincipal);
        if (actor.actorType() != verifiedPrincipal.principalType()) {
            throw new ActorAccessDeniedException();
        }
        return new ActorContext(
                actor.actorRef(),
                actor.actorType(),
                verifiedPrincipal.externalPrincipalKey(),
                verifiedPrincipal.authenticationMethod(),
                verifiedPrincipal.authenticatedAt(),
                verifiedPrincipal.credentialExpiresAt(),
                UUID.randomUUID(),
                requestId,
                traceId);
    }
}
