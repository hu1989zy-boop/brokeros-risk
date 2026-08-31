package com.brokeros.risk.decision.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionException;
import com.brokeros.risk.decision.application.port.DecisionAccessLogPort;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcDecisionAccessLogAdapter implements DecisionAccessLogPort {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcDecisionAccessLogAdapter(
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
            DecisionRef ref,
            ActorRef accessor,
            Instant occurredAt) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                long decisionId = findDecisionId(ref);
                int inserted = jdbcTemplate.update("""
                        INSERT INTO decision_access_log (
                            decision_id, accessing_actor_ref, accessed_at)
                        VALUES (?, ?, ?)
                        """, decisionId, accessor.value(), Timestamp.from(occurredAt));
                if (inserted != 1) {
                    throw new DecisionAuthorityUnavailableException();
                }
            });
        } catch (DecisionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new DecisionAuthorityUnavailableException(exception);
        }
    }

    private long findDecisionId(DecisionRef ref) {
        List<Long> rows = jdbcTemplate.query(
                "SELECT id FROM decision_record WHERE decision_ref = ?",
                (resultSet, rowNumber) -> resultSet.getLong("id"), ref.value());
        if (rows.size() != 1) {
            throw new DecisionAuthorityUnavailableException();
        }
        return rows.getFirst();
    }
}
