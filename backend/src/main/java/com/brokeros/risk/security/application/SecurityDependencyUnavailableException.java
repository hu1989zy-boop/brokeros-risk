package com.brokeros.risk.security.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class SecurityDependencyUnavailableException extends BusinessException {

    public SecurityDependencyUnavailableException(Throwable cause) {
        super(
                ResultCode.SECURITY_DEPENDENCY_UNAVAILABLE,
                ResultCode.SECURITY_DEPENDENCY_UNAVAILABLE.defaultMessage(),
                cause);
    }
}
