package com.brokeros.risk.security.infrastructure.authentication;

import java.util.List;
import java.util.Objects;

import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public final class BrokerOsAuthentication extends AbstractAuthenticationToken {

    private final ActorContext actorContext;

    public BrokerOsAuthentication(ActorContext actorContext) {
        super(List.of());
        this.actorContext = Objects.requireNonNull(actorContext, "actorContext must not be null");
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return actorContext.actorRef();
    }

    @Override
    public String getName() {
        return actorContext.actorRef().value();
    }

    public ActorContext actorContext() {
        return actorContext;
    }
}
