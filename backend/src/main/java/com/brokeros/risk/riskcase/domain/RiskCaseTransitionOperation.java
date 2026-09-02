package com.brokeros.risk.riskcase.domain;

public enum RiskCaseTransitionOperation {
    CREATE,
    BEGIN_REVIEW,
    MARK_ACTION_REQUIRED,
    RETURN_TO_REVIEW,
    RESOLVE,
    CLOSE,
    CANCEL,
    RESUME_RESOLVED,
    REOPEN_CLOSED
}
