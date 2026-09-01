package com.brokeros.risk.action.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Optional;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionConflictException;
import com.brokeros.risk.action.application.ActionException;
import com.brokeros.risk.action.application.AuthorizedMutationContext;
import com.brokeros.risk.action.application.RecordActionSpec;
import com.brokeros.risk.action.application.port.ActionMutationPort;
import com.brokeros.risk.action.application.port.ActionRefGenerator;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionOperationType;
import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.ActionSemanticFingerprint;
import com.brokeros.risk.action.domain.ActionSource;
import com.brokeros.risk.action.domain.ActionStatus;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcActionMutationAdapter implements ActionMutationPort {

    private static final int MAX_GENERATED_REF_ATTEMPTS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ActionRefGenerator actionRefGenerator;
    private final MySqlActionConstraintClassifier constraintClassifier =
            new MySqlActionConstraintClassifier();

    public JdbcActionMutationAdapter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            ActionRefGenerator actionRefGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.actionRefGenerator = actionRefGenerator;
    }

    @Override
    public CompletedActionOperation record(
            RecordActionSpec spec,
            AuthorizedMutationContext context) {
        int generatedRefAttempts = 0;
        while (true) {
            try {
                CompletedActionOperation result = transactionTemplate.execute(
                        status -> recordTransaction(spec, context));
                return requireResult(result);
            } catch (ActionException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                MySqlActionConstraintClassifier.Category category =
                        constraintClassifier.classify(exception);
                if (category == MySqlActionConstraintClassifier.Category.GENERATED_REF
                        && ++generatedRefAttempts < MAX_GENERATED_REF_ATTEMPTS) {
                    continue;
                }
                if (category == MySqlActionConstraintClassifier.Category.OPERATION) {
                    Optional<CompletedActionOperation> completed = safeFindOperation(
                            spec.operationId().value());
                    if (completed.isPresent()) {
                        return replay(completed.orElseThrow(), context.fingerprint());
                    }
                }
                throw unavailable(exception);
            } catch (IllegalArgumentException exception) {
                throw unavailable(exception);
            }
        }
    }

    private CompletedActionOperation recordTransaction(
            RecordActionSpec spec,
            AuthorizedMutationContext context) {
        if (!spec.operationId().equals(context.operationId())) {
            throw new IllegalArgumentException("operation identity does not match context");
        }
        Optional<CompletedActionOperation> completed = findOperation(
                spec.operationId().value());
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), context.fingerprint());
        }

        ActionRef actionRef = actionRefGenerator.generate();
        jdbcTemplate.update("""
                INSERT INTO action_record (
                    action_ref, decision_ref, source, status, intent_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', 'PROPOSED', ?, ?, ?)
                """, actionRef.value(), spec.decisionRef().value(),
                spec.intentText().value().getBytes(StandardCharsets.UTF_8),
                context.actorContext().actorRef().value(), timestamp(context));
        long actionId = lastInsertId();
        jdbcTemplate.update("""
                INSERT INTO action_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    action_id, outcome, occurred_at)
                VALUES (?, 'RECORD', ?, ?, 'CREATED', ?)
                """, spec.operationId().value(), context.fingerprint().value(),
                actionId, timestamp(context));

        ActionRecord record = new ActionRecord(
                actionRef, spec.decisionRef(), spec.intentText(),
                ActionStatus.PROPOSED, ActionSource.MANUAL,
                context.actorContext().actorRef(), context.occurredAt());
        return new CompletedActionOperation(
                spec.operationId(), ActionOperationType.RECORD,
                context.fingerprint(), actionRef,
                ActionOperationOutcome.CREATED, context.occurredAt(), record);
    }

    private CompletedActionOperation replay(
            CompletedActionOperation completed,
            ActionSemanticFingerprint fingerprint) {
        if (completed.operationType() != ActionOperationType.RECORD
                || !completed.fingerprint().equals(fingerprint)) {
            throw new ActionConflictException();
        }
        return completed;
    }

    private Optional<CompletedActionOperation> safeFindOperation(String operationId) {
        try {
            return findOperation(operationId);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    private Optional<CompletedActionOperation> findOperation(String operationId) {
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
                """, JdbcActionRowMappers.completedOperation(), operationId);
    }

    private long lastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null || id <= 0) {
            throw new ActionAuthorityUnavailableException();
        }
        return id;
    }

    private Timestamp timestamp(AuthorizedMutationContext context) {
        return Timestamp.from(context.occurredAt());
    }

    private <T> T requireResult(T result) {
        if (result == null) {
            throw new ActionAuthorityUnavailableException();
        }
        return result;
    }

    private ActionAuthorityUnavailableException unavailable(Throwable cause) {
        return new ActionAuthorityUnavailableException(cause);
    }
}
