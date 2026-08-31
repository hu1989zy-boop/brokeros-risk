package com.brokeros.risk.evidence.infrastructure.persistence;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.dao.DataAccessException;

final class MySqlEvidenceConstraintClassifier {

    enum Category {
        OPERATION,
        GENERATED_REF,
        SUPERSESSION,
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
        if (message.contains("uk_evidence_operation_id")) {
            return Category.OPERATION;
        }
        if (message.contains("uk_evidence_record_ref")) {
            return Category.GENERATED_REF;
        }
        if (message.contains("uk_evidence_record_supersedes")) {
            return Category.SUPERSESSION;
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
