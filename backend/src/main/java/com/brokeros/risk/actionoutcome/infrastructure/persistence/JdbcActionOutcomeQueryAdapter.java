package com.brokeros.risk.actionoutcome.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeAuthorityUnavailableException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeReferenceSummary;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeQueryPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcActionOutcomeQueryAdapter implements ActionOutcomeQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcActionOutcomeQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CompletedActionOutcomeOperation> findOperation(
            ActionOutcomeOperationId id) {
        try {
            return jdbcTemplate.query("""
                    SELECT operation_row.operation_id,
                           operation_row.operation_type,
                           operation_row.semantic_fingerprint,
                           operation_row.outcome,
                           operation_row.occurred_at,
                           record_row.action_outcome_ref,
                           record_row.action_ref,
                           record_row.source,
                           record_row.outcome_text,
                           record_row.recorded_by_actor_ref,
                           record_row.recorded_at
                    FROM action_outcome_operation operation_row
                    JOIN action_outcome_record record_row
                        ON record_row.id = operation_row.action_outcome_id
                    WHERE operation_row.operation_id = ?
                    """, JdbcActionOutcomeRowMappers.completedOperation(), id.value());
        } catch (ActionOutcomeException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionOutcomeAuthorityUnavailableException(exception);
        }
    }

    @Override
    public Optional<ActionOutcomeRecord> findByRef(ActionOutcomeRef ref) {
        try {
            return jdbcTemplate.query("""
                    SELECT action_outcome_ref,
                           action_ref,
                           source,
                           outcome_text,
                           recorded_by_actor_ref,
                           recorded_at
                    FROM action_outcome_record
                    WHERE action_outcome_ref = ?
                    """, JdbcActionOutcomeRowMappers.actionOutcomeRecord(), ref.value());
        } catch (ActionOutcomeException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionOutcomeAuthorityUnavailableException(exception);
        }
    }

    @Override
    public List<ActionOutcomeReferenceSummary> findSummariesByAction(
            ActionRef actionRef,
            int limit) {
        try {
            return jdbcTemplate.query("""
                    SELECT action_outcome_ref,
                           action_ref,
                           recorded_at
                    FROM action_outcome_record
                    WHERE action_ref = ?
                    ORDER BY recorded_at DESC, id DESC
                    LIMIT ?
                    """, (resultSet, rowNumber) -> new ActionOutcomeReferenceSummary(
                            new ActionOutcomeRef(resultSet.getString("action_outcome_ref")),
                            new ActionRef(resultSet.getString("action_ref")),
                            resultSet.getTimestamp("recorded_at").toInstant()),
                    actionRef.value(), limit);
        } catch (ActionOutcomeException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionOutcomeAuthorityUnavailableException(exception);
        }
    }
}
