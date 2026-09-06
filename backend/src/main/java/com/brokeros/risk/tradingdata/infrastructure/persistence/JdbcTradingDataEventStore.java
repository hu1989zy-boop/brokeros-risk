package com.brokeros.risk.tradingdata.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.OptionalLong;

import com.brokeros.risk.tradingdata.application.TradingDataAuthorityUnavailableException;
import com.brokeros.risk.tradingdata.application.port.TradingDataAppendOutcome;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventStore;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTradingDataEventStore implements TradingDataEventStore {

    private final JdbcTemplate jdbcTemplate;
    private final MySqlTradingDataConstraintClassifier constraintClassifier =
            new MySqlTradingDataConstraintClassifier();

    public JdbcTradingDataEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OptionalLong lastContiguousSequence(String sourceServerId) {
        try {
            Long sequence = jdbcTemplate.queryForObject(
                    "SELECT MAX(source_sequence) FROM trading_data_event "
                            + "WHERE source_server_id = ?",
                    Long.class,
                    sourceServerId);
            return sequence == null ? OptionalLong.empty() : OptionalLong.of(sequence);
        } catch (DataAccessException exception) {
            throw new TradingDataAuthorityUnavailableException(exception);
        }
    }

    @Override
    public TradingDataAppendOutcome append(
            TradingDataEnvelope envelope,
            Instant receivedAt) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO trading_data_event (
                        envelope_version, platform, source_server_id, source_sequence,
                        trading_account_id, occurred_at, received_at, payload)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    envelope.envelopeVersion(),
                    envelope.platform().name(),
                    envelope.sourceServerId(),
                    envelope.sourceSequence(),
                    envelope.tradingAccountId(),
                    Timestamp.from(envelope.occurredAt()),
                    Timestamp.from(receivedAt),
                    envelope.payload());
            return TradingDataAppendOutcome.INSERTED;
        } catch (DataAccessException exception) {
            if (constraintClassifier.isSourceSequenceDuplicate(exception)) {
                return TradingDataAppendOutcome.DUPLICATE;
            }
            throw new TradingDataAuthorityUnavailableException(exception);
        }
    }

    @Override
    public void recordGap(
            String sourceServerId,
            long fromSequence,
            long toSequence,
            Instant detectedAt) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO trading_data_ingestion_gap (
                        source_server_id, from_sequence, to_sequence, detected_at)
                    VALUES (?, ?, ?, ?)
                    """,
                    sourceServerId,
                    fromSequence,
                    toSequence,
                    Timestamp.from(detectedAt));
        } catch (DataAccessException exception) {
            throw new TradingDataAuthorityUnavailableException(exception);
        }
    }
}
