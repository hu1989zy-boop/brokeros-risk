package com.brokeros.risk.actionoutcome.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeAuthorityUnavailableException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeException;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeAccessLogPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.security.domain.ActorRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcActionOutcomeAccessLogAdapter
        implements ActionOutcomeAccessLogPort {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcActionOutcomeAccessLogAdapter(
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
            ActionOutcomeRef ref,
            ActorRef accessor,
            Instant occurredAt) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                long actionOutcomeId = findActionOutcomeId(ref);
                int inserted = jdbcTemplate.update("""
                        INSERT INTO action_outcome_access_log (
                            action_outcome_id, accessing_actor_ref, accessed_at)
                        VALUES (?, ?, ?)
                        """, actionOutcomeId, accessor.value(),
                        Timestamp.from(occurredAt));
                if (inserted != 1) {
                    throw new ActionOutcomeAuthorityUnavailableException();
                }
            });
        } catch (ActionOutcomeException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionOutcomeAuthorityUnavailableException(exception);
        }
    }

    private long findActionOutcomeId(ActionOutcomeRef ref) {
        java.util.List<Long> rows = jdbcTemplate.query(
                "SELECT id FROM action_outcome_record WHERE action_outcome_ref = ?",
                (resultSet, rowNumber) -> resultSet.getLong("id"), ref.value());
        if (rows.size() != 1) {
            throw new ActionOutcomeAuthorityUnavailableException();
        }
        return rows.getFirst();
    }
}
