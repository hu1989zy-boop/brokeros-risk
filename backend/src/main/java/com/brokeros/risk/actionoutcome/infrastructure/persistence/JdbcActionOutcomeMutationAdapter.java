package com.brokeros.risk.actionoutcome.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Optional;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeAuthorityUnavailableException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeConflictException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeException;
import com.brokeros.risk.actionoutcome.application.AuthorizedMutationContext;
import com.brokeros.risk.actionoutcome.application.RecordActionOutcomeSpec;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMutationPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeRefGenerator;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationType;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSemanticFingerprint;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSource;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcActionOutcomeMutationAdapter implements ActionOutcomeMutationPort {

    private static final int MAX_GENERATED_REF_ATTEMPTS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ActionOutcomeRefGenerator actionOutcomeRefGenerator;
    private final MySqlActionOutcomeConstraintClassifier constraintClassifier =
            new MySqlActionOutcomeConstraintClassifier();

    public JdbcActionOutcomeMutationAdapter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            ActionOutcomeRefGenerator actionOutcomeRefGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.actionOutcomeRefGenerator = actionOutcomeRefGenerator;
    }

    @Override
    public CompletedActionOutcomeOperation record(
            RecordActionOutcomeSpec spec,
            AuthorizedMutationContext context) {
        int generatedRefAttempts = 0;
        while (true) {
            try {
                CompletedActionOutcomeOperation result = transactionTemplate.execute(
                        status -> recordTransaction(spec, context));
                return requireResult(result);
            } catch (ActionOutcomeException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                MySqlActionOutcomeConstraintClassifier.Category category =
                        constraintClassifier.classify(exception);
                if (category
                        == MySqlActionOutcomeConstraintClassifier.Category.GENERATED_REF
                        && ++generatedRefAttempts < MAX_GENERATED_REF_ATTEMPTS) {
                    continue;
                }
                if (category
                        == MySqlActionOutcomeConstraintClassifier.Category.OPERATION) {
                    Optional<CompletedActionOutcomeOperation> completed =
                            safeFindOperation(spec.operationId().value());
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

    private CompletedActionOutcomeOperation recordTransaction(
            RecordActionOutcomeSpec spec,
            AuthorizedMutationContext context) {
        if (!spec.operationId().equals(context.operationId())) {
            throw new IllegalArgumentException("operation identity does not match context");
        }
        Optional<CompletedActionOutcomeOperation> completed =
                findOperation(spec.operationId().value());
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), context.fingerprint());
        }

        ActionOutcomeRef actionOutcomeRef = actionOutcomeRefGenerator.generate();
        jdbcTemplate.update("""
                INSERT INTO action_outcome_record (
                    action_outcome_ref, action_ref, source, outcome_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', ?, ?, ?)
                """, actionOutcomeRef.value(), spec.actionRef().value(),
                spec.outcomeText().value().getBytes(StandardCharsets.UTF_8),
                context.actorContext().actorRef().value(), timestamp(context));
        long actionOutcomeId = lastInsertId();
        jdbcTemplate.update("""
                INSERT INTO action_outcome_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    action_outcome_id, outcome, occurred_at)
                VALUES (?, 'RECORD', ?, ?, 'CREATED', ?)
                """, spec.operationId().value(), context.fingerprint().value(),
                actionOutcomeId, timestamp(context));

        ActionOutcomeRecord record = new ActionOutcomeRecord(
                actionOutcomeRef, spec.actionRef(), spec.outcomeText(),
                ActionOutcomeSource.MANUAL,
                context.actorContext().actorRef(), context.occurredAt());
        return new CompletedActionOutcomeOperation(
                spec.operationId(), ActionOutcomeOperationType.RECORD,
                context.fingerprint(), actionOutcomeRef,
                ActionOutcomeOperationOutcome.CREATED, context.occurredAt(), record);
    }

    private CompletedActionOutcomeOperation replay(
            CompletedActionOutcomeOperation completed,
            ActionOutcomeSemanticFingerprint fingerprint) {
        if (completed.operationType() != ActionOutcomeOperationType.RECORD
                || !completed.fingerprint().equals(fingerprint)) {
            throw new ActionOutcomeConflictException();
        }
        return completed;
    }

    private Optional<CompletedActionOutcomeOperation> safeFindOperation(
            String operationId) {
        try {
            return findOperation(operationId);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    private Optional<CompletedActionOutcomeOperation> findOperation(
            String operationId) {
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
                """, JdbcActionOutcomeRowMappers.completedOperation(), operationId);
    }

    private long lastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null || id <= 0) {
            throw new ActionOutcomeAuthorityUnavailableException();
        }
        return id;
    }

    private Timestamp timestamp(AuthorizedMutationContext context) {
        return Timestamp.from(context.occurredAt());
    }

    private <T> T requireResult(T result) {
        if (result == null) {
            throw new ActionOutcomeAuthorityUnavailableException();
        }
        return result;
    }

    private ActionOutcomeAuthorityUnavailableException unavailable(Throwable cause) {
        return new ActionOutcomeAuthorityUnavailableException(cause);
    }
}
