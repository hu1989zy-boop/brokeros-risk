package com.brokeros.risk.action.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionException;
import com.brokeros.risk.action.application.ActionReferenceSummary;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.ActionStatus;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import com.brokeros.risk.decision.domain.DecisionRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcActionQueryAdapter implements ActionQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcActionQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CompletedActionOperation> findOperation(ActionOperationId id) {
        try {
            return jdbcTemplate.query("""
                    SELECT operation_row.operation_id,
                           operation_row.operation_type,
                           operation_row.semantic_fingerprint,
                           operation_row.outcome,
                           operation_row.occurred_at,
                           record_row.action_ref,
                           record_row.decision_ref,
                           record_row.source,
                           record_row.status,
                           record_row.intent_text,
                           record_row.recorded_by_actor_ref,
                           record_row.recorded_at
                    FROM action_operation operation_row
                    JOIN action_record record_row
                        ON record_row.id = operation_row.action_id
                    WHERE operation_row.operation_id = ?
                    """, JdbcActionRowMappers.completedOperation(), id.value());
        } catch (ActionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionAuthorityUnavailableException(exception);
        }
    }

    @Override
    public Optional<ActionRecord> findByRef(ActionRef ref) {
        try {
            return jdbcTemplate.query("""
                    SELECT action_ref,
                           decision_ref,
                           source,
                           status,
                           intent_text,
                           recorded_by_actor_ref,
                           recorded_at
                    FROM action_record
                    WHERE action_ref = ?
                    """, JdbcActionRowMappers.actionRecord(), ref.value());
        } catch (ActionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionAuthorityUnavailableException(exception);
        }
    }

    @Override
    public List<ActionReferenceSummary> findSummariesByDecision(
            DecisionRef decisionRef,
            int limit) {
        try {
            return jdbcTemplate.query("""
                    SELECT action_ref,
                           decision_ref,
                           status,
                           recorded_at
                    FROM action_record
                    WHERE decision_ref = ?
                    ORDER BY recorded_at DESC, id DESC
                    LIMIT ?
                    """, (resultSet, rowNumber) -> new ActionReferenceSummary(
                            new ActionRef(resultSet.getString("action_ref")),
                            new DecisionRef(resultSet.getString("decision_ref")),
                            ActionStatus.valueOf(resultSet.getString("status")),
                            resultSet.getTimestamp("recorded_at").toInstant()),
                    decisionRef.value(), limit);
        } catch (ActionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionAuthorityUnavailableException(exception);
        }
    }
}
