package com.brokeros.risk.riskcase.application;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.riskcase.domain.RiskCaseDomainError;
import com.brokeros.risk.riskcase.domain.RiskCaseDomainException;

final class RiskCaseErrors {

    private RiskCaseErrors() {
    }

    static RiskCaseException translate(RiskCaseDomainException exception) {
        ResultCode resultCode = switch (exception.error()) {
            case INVALID_TRANSITION -> ResultCode.RISK_CASE_INVALID_TRANSITION;
            case INVARIANT_VIOLATION -> ResultCode.RISK_CASE_INVARIANT_VIOLATION;
            case VERSION_CONFLICT -> ResultCode.RISK_CASE_VERSION_CONFLICT;
        };
        return new RiskCaseException(resultCode, exception);
    }

    static RiskCaseException invalid(Throwable exception) {
        return new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION, exception);
    }
}
