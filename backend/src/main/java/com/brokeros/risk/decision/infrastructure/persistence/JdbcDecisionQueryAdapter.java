package com.brokeros.risk.decision.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionException;
import com.brokeros.risk.decision.application.DecisionReferenceSummary;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDecisionQueryAdapter implements DecisionQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDecisionQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CompletedDecisionOperation> findOperation(DecisionOperationId id) {
        try {
            return jdbcTemplate.query("""
                    SELECT operation_row.operation_id,
                           operation_row.operation_type,
                           operation_row.semantic_fingerprint,
                           operation_row.outcome,
                           operation_row.occurred_at,
                           record_row.id AS record_id,
                           record_row.decision_ref,
                           record_row.subject_ref,
                           record_row.source,
                           record_row.conclusion_text,
                           record_row.recorded_by_actor_ref,
                           record_row.recorded_at,
                           reference_row.evidence_ref
                    FROM decision_operation operation_row
                    JOIN decision_record record_row
                        ON record_row.id = operation_row.decision_id
                    JOIN decision_evidence_reference reference_row
                        ON reference_row.decision_id = record_row.id
                    WHERE operation_row.operation_id = ?
                    ORDER BY reference_row.evidence_ref
                    """, JdbcDecisionRowMappers.completedOperation(), id.value());
        } catch (DecisionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new DecisionAuthorityUnavailableException(exception);
        }
    }

    @Override
    public Optional<DecisionRecord> findByRef(DecisionRef ref) {
        try {
            return jdbcTemplate.query("""
                    SELECT record_row.id AS record_id,
                           record_row.decision_ref,
                           record_row.subject_ref,
                           record_row.source,
                           record_row.conclusion_text,
                           record_row.recorded_by_actor_ref,
                           record_row.recorded_at,
                           reference_row.evidence_ref
                    FROM decision_record record_row
                    JOIN decision_evidence_reference reference_row
                        ON reference_row.decision_id = record_row.id
                    WHERE record_row.decision_ref = ?
                    ORDER BY reference_row.evidence_ref
                    """, JdbcDecisionRowMappers.decisionRecord(), ref.value());
        } catch (DecisionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new DecisionAuthorityUnavailableException(exception);
        }
    }

    @Override
    public List<DecisionReferenceSummary> findSummariesBySubject(
            TradingAccountRef subjectRef,
            int limit) {
        try {
            return jdbcTemplate.query("""
                    SELECT decision_ref,
                           subject_ref,
                           recorded_at
                    FROM decision_record
                    WHERE subject_ref = ?
                    ORDER BY recorded_at DESC, id DESC
                    LIMIT ?
                    """, (resultSet, rowNumber) -> new DecisionReferenceSummary(
                            new DecisionRef(resultSet.getString("decision_ref")),
                            new TradingAccountRef(resultSet.getString("subject_ref")),
                            resultSet.getTimestamp("recorded_at").toInstant()),
                    subjectRef.value(), limit);
        } catch (DecisionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new DecisionAuthorityUnavailableException(exception);
        }
    }
}
