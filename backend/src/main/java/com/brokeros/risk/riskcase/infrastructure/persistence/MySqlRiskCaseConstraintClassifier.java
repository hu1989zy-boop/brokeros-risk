package com.brokeros.risk.riskcase.infrastructure.persistence;

import java.sql.SQLException;

import com.brokeros.risk.riskcase.application.port.RiskCaseConflictKind;

final class MySqlRiskCaseConstraintClassifier {

    RiskCaseConflictKind classify(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException
                    && sqlException.getErrorCode() == 1062
                    && "23000".equals(sqlException.getSQLState())) {
                String message = sqlException.getMessage();
                if (message != null && message.contains("uq_risk_case_case_number")) {
                    return RiskCaseConflictKind.CASE_NUMBER;
                }
                if (message != null && message.contains("uq_risk_case_creation_key")) {
                    return RiskCaseConflictKind.CREATION_KEY;
                }
                if (message != null && message.contains("uq_risk_case_decision_ref")) {
                    return RiskCaseConflictKind.PRIMARY_DECISION;
                }
                return RiskCaseConflictKind.OTHER;
            }
            current = current.getCause();
        }
        return RiskCaseConflictKind.OTHER;
    }
}
