package com.brokeros.risk.tradingdata.infrastructure.persistence;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.dao.DataAccessException;

final class MySqlTradingDataConstraintClassifier {

    boolean isSourceSequenceDuplicate(DataAccessException exception) {
        SQLException sqlException = rootSqlException(exception);
        return sqlException != null
                && sqlException.getErrorCode() == 1062
                && "23000".equals(sqlException.getSQLState())
                && sqlException.getMessage().toLowerCase(Locale.ROOT)
                        .contains("uq_trading_data_event_source_sequence");
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
