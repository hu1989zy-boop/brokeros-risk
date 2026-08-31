package com.brokeros.risk.evidence.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.evidence.application.CompletedEvidenceOperation;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceException;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEvidenceQueryAdapter implements EvidenceQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEvidenceQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CompletedEvidenceOperation> findOperation(EvidenceOperationId id) {
        try {
            return one(jdbcTemplate.query("""
                    SELECT operation_row.operation_type,
                           operation_row.semantic_fingerprint,
                           CASE
                               WHEN operation_row.operation_type = 'RECORD'
                                   THEN target.evidence_ref
                               ELSE replacement.evidence_ref
                           END AS result_evidence_ref,
                           operation_row.outcome,
                           operation_row.occurred_at
                    FROM evidence_operation operation_row
                    JOIN evidence_record target ON target.id = operation_row.evidence_id
                    LEFT JOIN evidence_record replacement
                        ON replacement.supersedes_id = target.id
                    WHERE operation_row.operation_id = ?
                    """, JdbcEvidenceRowMappers.completedOperation(), id.value()));
        } catch (EvidenceException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new EvidenceAuthorityUnavailableException(exception);
        }
    }

    @Override
    public Optional<EvidenceRecord> findByRef(EvidenceRef ref) {
        try {
            return one(jdbcTemplate.query("""
                    SELECT record_row.evidence_ref,
                           record_row.subject_ref,
                           record_row.source,
                           record_row.observation_text,
                           record_row.status,
                           record_row.recorded_by_actor_ref,
                           record_row.recorded_at,
                           prior.evidence_ref AS supersedes_ref,
                           replacement.evidence_ref AS superseded_by_ref
                    FROM evidence_record record_row
                    LEFT JOIN evidence_record prior ON prior.id = record_row.supersedes_id
                    LEFT JOIN evidence_record replacement
                        ON replacement.id = record_row.superseded_by_id
                    WHERE record_row.evidence_ref = ?
                    """, JdbcEvidenceRowMappers.evidenceRecord(), ref.value()));
        } catch (EvidenceException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new EvidenceAuthorityUnavailableException(exception);
        }
    }

    private <T> Optional<T> one(List<T> rows) {
        if (rows.size() > 1) {
            throw new EvidenceAuthorityUnavailableException();
        }
        return rows.stream().findFirst();
    }
}
