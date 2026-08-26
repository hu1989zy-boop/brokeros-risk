package com.brokeros.risk.security.application.port;

import com.brokeros.risk.security.domain.ActorContext;

public interface ActorContextProvider {

    ActorContext currentContext();
}
