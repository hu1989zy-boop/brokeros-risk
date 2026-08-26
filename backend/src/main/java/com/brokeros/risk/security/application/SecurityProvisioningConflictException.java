package com.brokeros.risk.security.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class SecurityProvisioningConflictException extends BusinessException {

    public SecurityProvisioningConflictException() {
        super(ResultCode.SECURITY_PROVISIONING_CONFLICT);
    }

    public SecurityProvisioningConflictException(Throwable cause) {
        super(
                ResultCode.SECURITY_PROVISIONING_CONFLICT,
                ResultCode.SECURITY_PROVISIONING_CONFLICT.defaultMessage(),
                cause);
    }
}
