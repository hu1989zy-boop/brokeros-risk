package com.brokeros.risk.tradingaccount.infrastructure.persistence;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.dao.DataAccessException;

final class MySqlAuthorityConstraintClassifier {

    enum Category {
        OPERATION,
        EXTERNAL_IDENTITY,
        SCOPE_ATTESTATION,
        GENERATED_REF,
        UNKNOWN
    }

    Category classify(DataAccessException exception) {
        SQLException sqlException = rootSqlException(exception);
        if (sqlException == null || sqlException.getErrorCode() != 1062
                || !"23000".equals(sqlException.getSQLState())) {
            return Category.UNKNOWN;
        }
        String message = sqlException.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("uk_ta_authority_operation_id")) return Category.OPERATION;
        if (message.contains("uk_trading_account_reference_external_identity")) return Category.EXTERNAL_IDENTITY;
        if (message.contains("uk_ta_authority_scope_attestation")) return Category.SCOPE_ATTESTATION;
        if (message.contains("uk_ta_authority_scope_ref")
                || message.contains("uk_trading_account_reference_ref")) return Category.GENERATED_REF;
        return Category.UNKNOWN;
    }

    private SQLException rootSqlException(Throwable throwable) {
        SQLException found = null;
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) found = sqlException;
            current = current.getCause();
        }
        return found;
    }
}
