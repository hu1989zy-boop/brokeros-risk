package com.brokeros.risk.action.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionException;
import com.brokeros.risk.action.application.port.ActionAccessLogPort;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.security.domain.ActorRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcActionAccessLogAdapter implements ActionAccessLogPort {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcActionAccessLogAdapter(
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
            ActionRef ref,
            ActorRef accessor,
            Instant occurredAt) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                long actionId = findActionId(ref);
                int inserted = jdbcTemplate.update("""
                        INSERT INTO action_access_log (
                            action_id, accessing_actor_ref, accessed_at)
                        VALUES (?, ?, ?)
                        """, actionId, accessor.value(), Timestamp.from(occurredAt));
                if (inserted != 1) {
                    throw new ActionAuthorityUnavailableException();
                }
            });
        } catch (ActionException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new ActionAuthorityUnavailableException(exception);
        }
    }

    private long findActionId(ActionRef ref) {
        List<Long> rows = jdbcTemplate.query(
                "SELECT id FROM action_record WHERE action_ref = ?",
                (resultSet, rowNumber) -> resultSet.getLong("id"), ref.value());
        if (rows.size() != 1) {
            throw new ActionAuthorityUnavailableException();
        }
        return rows.getFirst();
    }
}
