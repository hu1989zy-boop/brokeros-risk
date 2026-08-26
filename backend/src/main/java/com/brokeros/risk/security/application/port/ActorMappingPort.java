package com.brokeros.risk.security.application.port;

import com.brokeros.risk.security.domain.MappedActor;
import com.brokeros.risk.security.domain.VerifiedPrincipal;

public interface ActorMappingPort {

    MappedActor resolveActiveActor(VerifiedPrincipal verifiedPrincipal);
}
