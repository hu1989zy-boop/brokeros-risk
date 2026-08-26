package com.brokeros.risk.security.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class AuthorizationDeniedException extends BusinessException {

    public AuthorizationDeniedException() {
        super(ResultCode.AUTHORIZATION_DENIED);
    }
}
