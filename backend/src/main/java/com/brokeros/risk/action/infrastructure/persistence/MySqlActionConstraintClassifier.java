package com.brokeros.risk.action.infrastructure.persistence;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.dao.DataAccessException;

final class MySqlActionConstraintClassifier {

    enum Category {
        OPERATION,
        GENERATED_REF,
        UNKNOWN
    }

    Category classify(DataAccessException exception) {
        SQLException sqlException = rootSqlException(exception);
        if (sqlException == null
                || sqlException.getErrorCode() != 1062
                || !"23000".equals(sqlException.getSQLState())) {
            return Category.UNKNOWN;
        }
        String message = sqlException.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("uk_action_operation_id")) {
            return Category.OPERATION;
        }
        if (message.contains("uk_action_record_ref")) {
            return Category.GENERATED_REF;
        }
        return Category.UNKNOWN;
    }

    private SQLException rootSqlException(Throwable throwable) {
        SQLException found = null;
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                found = sqlException;
            }
            current = current.getCause();
        }
        return found;
    }
}
