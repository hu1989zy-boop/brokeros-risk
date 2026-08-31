package com.brokeros.risk.evidence.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceException;
import com.brokeros.risk.evidence.application.port.EvidenceAccessLogPort;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcEvidenceAccessLogAdapter implements EvidenceAccessLogPort {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcEvidenceAccessLogAdapter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setReadOnly(false);
        this.transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void recordFullDetailAccess(
            EvidenceRef ref,
            ActorRef accessor,
            Instant occurredAt) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                long evidenceId = findEvidenceId(ref);
                int inserted = jdbcTemplate.update("""
                        INSERT INTO evidence_access_log (
                            evidence_id, accessing_actor_ref, accessed_at)
                        VALUES (?, ?, ?)
                        """, evidenceId, accessor.value(), Timestamp.from(occurredAt));
                if (inserted != 1) {
                    throw new EvidenceAuthorityUnavailableException();
                }
            });
        } catch (EvidenceException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new EvidenceAuthorityUnavailableException(exception);
        }
    }

    private long findEvidenceId(EvidenceRef ref) {
        List<Long> rows = jdbcTemplate.query(
                "SELECT id FROM evidence_record WHERE evidence_ref = ?",
                (resultSet, rowNumber) -> resultSet.getLong("id"), ref.value());
        if (rows.size() != 1) {
            throw new EvidenceAuthorityUnavailableException();
        }
        return rows.getFirst();
    }
}
