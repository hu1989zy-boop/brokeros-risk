package com.brokeros.risk.security.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class ActorAccessDeniedException extends BusinessException {

    public ActorAccessDeniedException() {
        super(ResultCode.ACTOR_ACCESS_DENIED);
    }
}
