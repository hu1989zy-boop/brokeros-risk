package com.brokeros.risk.riskcase.application;

import java.util.Set;

import com.brokeros.risk.api.ResultCode;

public final class RiskCaseResultCodes {

    public static final Set<ResultCode> APPROVED = Set.of(
            ResultCode.RISK_CASE_NOT_FOUND,
            ResultCode.RISK_CASE_INVALID_TRANSITION,
            ResultCode.RISK_CASE_INVARIANT_VIOLATION,
            ResultCode.RISK_CASE_VERSION_CONFLICT,
            ResultCode.RISK_CASE_IDEMPOTENCY_CONFLICT,
            ResultCode.RISK_CASE_PRIMARY_DECISION_CONFLICT,
            ResultCode.RISK_CASE_REFERENCE_NOT_FOUND,
            ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE,
            ResultCode.RISK_CASE_SUBJECT_NOT_ELIGIBLE);

    private RiskCaseResultCodes() {
    }
}
