package com.brokeros.risk.evidence.infrastructure.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.AuthorizedMutationContext;
import com.brokeros.risk.evidence.application.CompletedEvidenceOperation;
import com.brokeros.risk.evidence.application.CorrectEvidenceSpec;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceConflictException;
import com.brokeros.risk.evidence.application.EvidenceCorrectionResult;
import com.brokeros.risk.evidence.application.EvidenceException;
import com.brokeros.risk.evidence.application.EvidenceRecordingResult;
import com.brokeros.risk.evidence.application.RecordEvidenceSpec;
import com.brokeros.risk.evidence.application.port.EvidenceMutationPort;
import com.brokeros.risk.evidence.application.port.EvidenceRefGenerator;
import com.brokeros.risk.evidence.domain.EvidenceFingerprint;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.evidence.domain.EvidenceOperationType;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.EvidenceStatus;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcEvidenceMutationAdapter implements EvidenceMutationPort {

    private static final int MAX_GENERATED_REF_ATTEMPTS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final EvidenceRefGenerator evidenceRefGenerator;
    private final MySqlEvidenceConstraintClassifier constraintClassifier =
            new MySqlEvidenceConstraintClassifier();

    public JdbcEvidenceMutationAdapter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            EvidenceRefGenerator evidenceRefGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.evidenceRefGenerator = evidenceRefGenerator;
    }

    @Override
    public EvidenceRecordingResult record(
            RecordEvidenceSpec spec,
            AuthorizedMutationContext context) {
        int generatedRefAttempts = 0;
        while (true) {
            try {
                EvidenceRecordingResult result = transactionTemplate.execute(
                        status -> recordTransaction(spec, context));
                return requireResult(result);
            } catch (EvidenceException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                MySqlEvidenceConstraintClassifier.Category category =
                        constraintClassifier.classify(exception);
                if (category == MySqlEvidenceConstraintClassifier.Category.GENERATED_REF
                        && ++generatedRefAttempts < MAX_GENERATED_REF_ATTEMPTS) {
                    continue;
                }
                if (category == MySqlEvidenceConstraintClassifier.Category.OPERATION) {
                    Optional<CompletedEvidenceOperation> completed = safeFindOperation(
                            spec.operationId().value());
                    if (completed.isPresent()) {
                        return recordingReplay(
                                completed.orElseThrow(), context.fingerprint());
                    }
                }
                throw unavailable(exception);
            } catch (IllegalArgumentException exception) {
                throw unavailable(exception);
            }
        }
    }

    private EvidenceRecordingResult recordTransaction(
            RecordEvidenceSpec spec,
            AuthorizedMutationContext context) {
        Optional<CompletedEvidenceOperation> completed = findOperation(
                spec.operationId().value());
        if (completed.isPresent()) {
            return recordingReplay(completed.orElseThrow(), context.fingerprint());
        }

        EvidenceRef evidenceRef = evidenceRefGenerator.generate();
        jdbcTemplate.update("""
                INSERT INTO evidence_record (
                    evidence_ref, subject_ref, source, observation_text, status,
                    supersedes_id, superseded_by_id, recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', ?, 'ACTIVE', NULL, NULL, ?, ?)
                """, evidenceRef.value(), spec.subjectRef().value(),
                spec.observationText().value().getBytes(StandardCharsets.UTF_8),
                context.actorContext().actorRef().value(), timestamp(context));
        long evidenceId = lastInsertId();
        long operationRowId = insertOperation(
                spec.operationId().value(), EvidenceOperationType.RECORD,
                context, evidenceId, EvidenceOperationOutcome.CREATED);
        insertHistory(operationRowId, EvidenceOperationType.RECORD, context, null);
        return new EvidenceRecordingResult(
                evidenceRef, EvidenceOperationOutcome.CREATED, context.occurredAt());
    }

    @Override
    public EvidenceCorrectionResult correct(
            CorrectEvidenceSpec spec,
            AuthorizedMutationContext context) {
        int generatedRefAttempts = 0;
        while (true) {
            try {
                EvidenceCorrectionResult result = transactionTemplate.execute(
                        status -> correctTransaction(spec, context));
                return requireResult(result);
            } catch (EvidenceConflictException exception) {
                if (exception.getResultCode() == ResultCode.EVIDENCE_ALREADY_SUPERSEDED) {
                    Optional<CompletedEvidenceOperation> completed = safeFindOperation(
                            spec.operationId().value());
                    if (completed.isPresent()) {
                        return correctionReplay(
                                completed.orElseThrow(), context.fingerprint());
                    }
                }
                throw exception;
            } catch (EvidenceException exception) {
                throw exception;
            } catch (DataAccessException exception) {
                MySqlEvidenceConstraintClassifier.Category category =
                        constraintClassifier.classify(exception);
                if (category == MySqlEvidenceConstraintClassifier.Category.GENERATED_REF
                        && ++generatedRefAttempts < MAX_GENERATED_REF_ATTEMPTS) {
                    continue;
                }
                if (category == MySqlEvidenceConstraintClassifier.Category.OPERATION
                        || category == MySqlEvidenceConstraintClassifier.Category.SUPERSESSION) {
                    Optional<CompletedEvidenceOperation> completed = safeFindOperation(
                            spec.operationId().value());
                    if (completed.isPresent()) {
                        return correctionReplay(
                                completed.orElseThrow(), context.fingerprint());
                    }
                    if (category == MySqlEvidenceConstraintClassifier.Category.SUPERSESSION) {
                        throw new EvidenceConflictException(
                                ResultCode.EVIDENCE_ALREADY_SUPERSEDED);
                    }
                }
                throw unavailable(exception);
            } catch (IllegalArgumentException exception) {
                throw unavailable(exception);
            }
        }
    }

    private EvidenceCorrectionResult correctTransaction(
            CorrectEvidenceSpec spec,
            AuthorizedMutationContext context) {
        Optional<CompletedEvidenceOperation> completed = findOperation(
                spec.operationId().value());
        if (completed.isPresent()) {
            return correctionReplay(completed.orElseThrow(), context.fingerprint());
        }

        LockedEvidenceTarget target = findTargetForUpdate(spec.targetEvidenceRef().value())
                .orElseThrow(() -> new EvidenceException(ResultCode.EVIDENCE_NOT_FOUND));
        if (target.status() != EvidenceStatus.ACTIVE) {
            throw new EvidenceConflictException(ResultCode.EVIDENCE_ALREADY_SUPERSEDED);
        }

        EvidenceRef replacementRef = evidenceRefGenerator.generate();
        jdbcTemplate.update("""
                INSERT INTO evidence_record (
                    evidence_ref, subject_ref, source, observation_text, status,
                    supersedes_id, superseded_by_id, recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', ?, 'ACTIVE', ?, NULL, ?, ?)
                """, replacementRef.value(), target.subjectRef().value(),
                spec.observationText().value().getBytes(StandardCharsets.UTF_8),
                target.id(), context.actorContext().actorRef().value(), timestamp(context));
        long replacementId = lastInsertId();

        int updated = jdbcTemplate.update("""
                UPDATE evidence_record
                SET status = 'SUPERSEDED', superseded_by_id = ?
                WHERE id = ? AND status = 'ACTIVE' AND superseded_by_id IS NULL
                """, replacementId, target.id());
        if (updated != 1) {
            throw new EvidenceConflictException(ResultCode.EVIDENCE_ALREADY_SUPERSEDED);
        }

        long operationRowId = insertOperation(
                spec.operationId().value(), EvidenceOperationType.CORRECT,
                context, target.id(), EvidenceOperationOutcome.CORRECTED);
        insertHistory(
                operationRowId, EvidenceOperationType.CORRECT,
                context, spec.correctionReason().value());
        return new EvidenceCorrectionResult(
                replacementRef, EvidenceOperationOutcome.CORRECTED, context.occurredAt());
    }

    private long insertOperation(
            String operationId,
            EvidenceOperationType operationType,
            AuthorizedMutationContext context,
            long evidenceId,
            EvidenceOperationOutcome outcome) {
        jdbcTemplate.update("""
                INSERT INTO evidence_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    evidence_id, outcome, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, operationId, operationType.name(), context.fingerprint().value(),
                evidenceId, outcome.name(), timestamp(context));
        return lastInsertId();
    }

    private void insertHistory(
            long operationRowId,
            EvidenceOperationType operationType,
            AuthorizedMutationContext context,
            String reason) {
        jdbcTemplate.update("""
                INSERT INTO evidence_operation_history (
                    operation_row_id, operation_type, actor_ref, capability, reason,
                    before_status, after_status, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, operationRowId, operationType.name(),
                context.actorContext().actorRef().value(), context.capability().value(),
                reason == null ? null : reason.getBytes(StandardCharsets.UTF_8),
                operationType == EvidenceOperationType.RECORD ? null : "ACTIVE",
                operationType == EvidenceOperationType.RECORD ? "ACTIVE" : "SUPERSEDED",
                timestamp(context));
    }

    private EvidenceRecordingResult recordingReplay(
            CompletedEvidenceOperation completed,
            EvidenceFingerprint fingerprint) {
        requireMatchingReplay(completed, EvidenceOperationType.RECORD, fingerprint);
        return new EvidenceRecordingResult(
                completed.resultEvidenceRef(), completed.outcome(), completed.occurredAt());
    }

    private EvidenceCorrectionResult correctionReplay(
            CompletedEvidenceOperation completed,
            EvidenceFingerprint fingerprint) {
        requireMatchingReplay(completed, EvidenceOperationType.CORRECT, fingerprint);
        return new EvidenceCorrectionResult(
                completed.resultEvidenceRef(), completed.outcome(), completed.occurredAt());
    }

    private void requireMatchingReplay(
            CompletedEvidenceOperation completed,
            EvidenceOperationType expectedType,
            EvidenceFingerprint fingerprint) {
        if (completed.operationType() != expectedType
                || !completed.fingerprint().equals(fingerprint)) {
            throw new EvidenceConflictException(ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT);
        }
    }

    private Optional<CompletedEvidenceOperation> safeFindOperation(String operationId) {
        try {
            return findOperation(operationId);
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    private Optional<CompletedEvidenceOperation> findOperation(String operationId) {
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
                """, JdbcEvidenceRowMappers.completedOperation(), operationId));
    }

    private Optional<LockedEvidenceTarget> findTargetForUpdate(String evidenceRef) {
        return one(jdbcTemplate.query("""
                SELECT id, subject_ref, status
                FROM evidence_record
                WHERE evidence_ref = ?
                FOR UPDATE
                """, (resultSet, rowNumber) -> new LockedEvidenceTarget(
                        resultSet.getLong("id"),
                        new TradingAccountRef(resultSet.getString("subject_ref")),
                        EvidenceStatus.valueOf(resultSet.getString("status"))), evidenceRef));
    }

    private <T> Optional<T> one(List<T> rows) {
        if (rows.size() > 1) {
            throw new EvidenceAuthorityUnavailableException();
        }
        return rows.stream().findFirst();
    }

    private long lastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null || id <= 0) {
            throw new EvidenceAuthorityUnavailableException();
        }
        return id;
    }

    private Timestamp timestamp(AuthorizedMutationContext context) {
        return Timestamp.from(context.occurredAt());
    }

    private <T> T requireResult(T result) {
        if (result == null) {
            throw new EvidenceAuthorityUnavailableException();
        }
        return result;
    }

    private EvidenceAuthorityUnavailableException unavailable(Throwable cause) {
        return new EvidenceAuthorityUnavailableException(cause);
    }

    private record LockedEvidenceTarget(
            long id,
            TradingAccountRef subjectRef,
            EvidenceStatus status) {

        private LockedEvidenceTarget {
            Objects.requireNonNull(subjectRef, "subjectRef must not be null");
            Objects.requireNonNull(status, "status must not be null");
        }
    }
}
