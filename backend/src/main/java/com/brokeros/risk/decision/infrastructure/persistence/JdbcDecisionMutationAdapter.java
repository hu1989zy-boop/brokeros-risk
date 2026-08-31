package com.brokeros.risk.decision.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Optional;

import com.brokeros.risk.decision.application.AuthorizedMutationContext;
import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionConflictException;
import com.brokeros.risk.decision.application.DecisionException;
import com.brokeros.risk.decision.application.RecordDecisionSpec;
import com.brokeros.risk.decision.application.port.DecisionMutationPort;
import com.brokeros.risk.decision.application.port.DecisionRefGenerator;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.domain.DecisionOperationType;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.decision.domain.DecisionSemanticFingerprint;
import com.brokeros.risk.decision.domain.DecisionSource;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcDecisionMutationAdapter implements DecisionMutationPort {

    private static final int MAX_GENERATED_REF_ATTEMPTS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final DecisionRefGenerator decisionRefGenerator;
    private final MySqlDecisionConstraintClassifier constraintClassifier =
            new MySqlDecisionConstraintClassifier();

    public JdbcDecisionMutationAdapter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            DecisionRefGenerator decisionRefGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.decisionRefGenerator = decisionRefGenerator;
    }

    @Override
    public CompletedDecisionOperation record(
            RecordDecisionSpec spec,
            AuthorizedMutationContext context) {
        int generatedRefAttempts = 0;
        while (true) {
            try {
                CompletedDecisionOperation result = transactionTemplate.execute(
                        status -> recordTransaction(spec, context));
                return requireResult(result);
            } catch (DecisionException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                MySqlDecisionConstraintClassifier.Category category =
                        constraintClassifier.classify(exception);
                if (category == MySqlDecisionConstraintClassifier.Category.GENERATED_REF
                        && ++generatedRefAttempts < MAX_GENERATED_REF_ATTEMPTS) {
                    continue;
                }
                if (category == MySqlDecisionConstraintClassifier.Category.OPERATION) {
                    Optional<CompletedDecisionOperation> completed = safeFindOperation(
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

    private CompletedDecisionOperation recordTransaction(
            RecordDecisionSpec spec,
            AuthorizedMutationContext context) {
        if (!spec.operationId().equals(context.operationId())) {
            throw new IllegalArgumentException("operation identity does not match context");
        }
        Optional<CompletedDecisionOperation> completed = findOperation(
                spec.operationId().value());
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), context.fingerprint());
        }

        DecisionRef decisionRef = decisionRefGenerator.generate();
        jdbcTemplate.update("""
                INSERT INTO decision_record (
                    decision_ref, subject_ref, source, conclusion_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', ?, ?, ?)
                """, decisionRef.value(), spec.subjectRef().value(),
                spec.conclusionText().value().getBytes(StandardCharsets.UTF_8),
                context.actorContext().actorRef().value(), timestamp(context));
        long decisionId = lastInsertId();
        for (EvidenceRef evidenceRef : spec.evidenceRefs()) {
            jdbcTemplate.update("""
                    INSERT INTO decision_evidence_reference (
                        decision_id, evidence_ref, created_at)
                    VALUES (?, ?, ?)
                    """, decisionId, evidenceRef.value(), timestamp(context));
        }
        jdbcTemplate.update("""
                INSERT INTO decision_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    decision_id, outcome, occurred_at)
                VALUES (?, 'RECORD', ?, ?, 'CREATED', ?)
                """, spec.operationId().value(), context.fingerprint().value(),
                decisionId, timestamp(context));

        DecisionRecord record = new DecisionRecord(
                decisionRef, spec.subjectRef(), spec.evidenceRefs(),
                spec.conclusionText(), DecisionSource.MANUAL,
                context.actorContext().actorRef(), context.occurredAt());
        return new CompletedDecisionOperation(
                spec.operationId(), DecisionOperationType.RECORD,
                context.fingerprint(), decisionRef,
                DecisionOperationOutcome.CREATED, context.occurredAt(), record);
    }

    private CompletedDecisionOperation replay(
            CompletedDecisionOperation completed,
            DecisionSemanticFingerprint fingerprint) {
        if (completed.operationType() != DecisionOperationType.RECORD
                || !completed.fingerprint().equals(fingerprint)) {
            throw new DecisionConflictException();
        }
        return completed;
    }

    private Optional<CompletedDecisionOperation> safeFindOperation(String operationId) {
        try {
            return findOperation(operationId);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    private Optional<CompletedDecisionOperation> findOperation(String operationId) {
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
                """, JdbcDecisionRowMappers.completedOperation(), operationId);
    }

    private long lastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null || id <= 0) {
            throw new DecisionAuthorityUnavailableException();
        }
        return id;
    }

    private Timestamp timestamp(AuthorizedMutationContext context) {
        return Timestamp.from(context.occurredAt());
    }

    private <T> T requireResult(T result) {
        if (result == null) {
            throw new DecisionAuthorityUnavailableException();
        }
        return result;
    }

    private DecisionAuthorityUnavailableException unavailable(Throwable cause) {
        return new DecisionAuthorityUnavailableException(cause);
    }
}
