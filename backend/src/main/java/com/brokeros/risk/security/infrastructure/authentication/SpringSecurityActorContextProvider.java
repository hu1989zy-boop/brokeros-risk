package com.brokeros.risk.security.infrastructure.authentication;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;
import com.brokeros.risk.security.application.port.ActorContextProvider;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSecurityActorContextProvider implements ActorContextProvider {

    @Override
    public ActorContext currentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof BrokerOsAuthentication brokerOsAuthentication
                && authentication.isAuthenticated()) {
            return brokerOsAuthentication.actorContext();
        }
        throw new BusinessException(ResultCode.AUTHENTICATION_REQUIRED);
    }
}
