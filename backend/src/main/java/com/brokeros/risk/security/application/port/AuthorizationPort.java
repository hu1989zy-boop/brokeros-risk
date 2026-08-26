package com.brokeros.risk.security.application.port;

import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;

public interface AuthorizationPort {

    AuthorizationDecision decide(ActorContext actorContext, Capability capability);
}
