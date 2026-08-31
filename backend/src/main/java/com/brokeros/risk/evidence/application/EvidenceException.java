package com.brokeros.risk.evidence.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;

public class EvidenceException extends BusinessException {

    public EvidenceException(ResultCode resultCode) {
        super(resultCode);
    }

    public EvidenceException(ResultCode resultCode, Throwable cause) {
        super(resultCode, resultCode.defaultMessage(), cause);
    }
}
