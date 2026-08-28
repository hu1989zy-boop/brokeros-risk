package com.brokeros.risk.tradingaccount.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.tradingaccount.application.AccountProvisioningResult;
import com.brokeros.risk.tradingaccount.application.AuthorizedMutationContext;
import com.brokeros.risk.tradingaccount.application.AuthorityOperationRequest;
import com.brokeros.risk.tradingaccount.application.ChangeAccountLifecycleSpec;
import com.brokeros.risk.tradingaccount.application.ChangeScopeLifecycleSpec;
import com.brokeros.risk.tradingaccount.application.LifecycleChangeResult;
import com.brokeros.risk.tradingaccount.application.RegisterAccountSpec;
import com.brokeros.risk.tradingaccount.application.RegisterScopeSpec;
import com.brokeros.risk.tradingaccount.application.ScopeProvisioningResult;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityException;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountConflictException;
import com.brokeros.risk.tradingaccount.application.port.AccountAuthorityScopeRefGenerator;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMutationPort;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountRefGenerator;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AttestationReference;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcTradingAccountAuthorityMutationAdapter
        implements TradingAccountAuthorityMutationPort {

    private static final int MAX_GENERATED_REF_ATTEMPTS = 3;
    private static final int MAX_RACE_RECLASSIFICATIONS = 2;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final AccountAuthorityScopeRefGenerator scopeRefGenerator;
    private final TradingAccountRefGenerator accountRefGenerator;
    private final MySqlAuthorityConstraintClassifier constraintClassifier =
            new MySqlAuthorityConstraintClassifier();

    public JdbcTradingAccountAuthorityMutationAdapter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            AccountAuthorityScopeRefGenerator scopeRefGenerator,
            TradingAccountRefGenerator accountRefGenerator) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.scopeRefGenerator = scopeRefGenerator;
        this.accountRefGenerator = accountRefGenerator;
    }

    @Override
    public ScopeProvisioningResult registerScope(
            RegisterScopeSpec spec,
            AuthorizedMutationContext context) {
        AuthorityOperationRequest request = spec.request();
        try {
            Optional<StoredOperation> before = findOperation(request.operationId().value());
            if (before.isPresent()) return scopeReplay(before.orElseThrow(), request, context);
            int generatedAttempts = 0;
            int raceAttempts = 0;
            while (true) {
                try {
                    return transactionTemplate.execute(status -> registerScopeTransaction(
                            request, context));
                } catch (DataAccessException exception) {
                    MySqlAuthorityConstraintClassifier.Category category =
                            constraintClassifier.classify(exception);
                    if (category == MySqlAuthorityConstraintClassifier.Category.GENERATED_REF
                            && ++generatedAttempts < MAX_GENERATED_REF_ATTEMPTS) continue;
                    if ((category == MySqlAuthorityConstraintClassifier.Category.OPERATION
                            || category == MySqlAuthorityConstraintClassifier.Category.SCOPE_ATTESTATION)
                            && ++raceAttempts <= MAX_RACE_RECLASSIFICATIONS) continue;
                    throw unavailable(exception);
                }
            }
        } catch (TradingAccountAuthorityException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    private ScopeProvisioningResult registerScopeTransaction(
            AuthorityOperationRequest request,
            AuthorizedMutationContext context) {
        Optional<StoredOperation> replay = findOperation(request.operationId().value());
        if (replay.isPresent()) return scopeReplay(replay.orElseThrow(), request, context);
        Optional<ScopeRow> prior = findScopeByAttestation(request.attestation());
        if (prior.isPresent()) {
            ScopeRow row = prior.orElseThrow();
            long operationRowId = insertOperation(
                    request, context, "AUTHORITY_SCOPE", row.id(), null,
                    row.ref(), AuthorityOperationOutcome.UNCHANGED, row.version());
            insertHistory(operationRowId, request, context, row.lifecycle(), row.lifecycle(),
                    row.version(), row.version());
            return new ScopeProvisioningResult(
                    new AccountAuthorityScopeRef(row.ref()),
                    AuthorityOperationOutcome.UNCHANGED,
                    row.version(), context.occurredAt());
        }
        AccountAuthorityScopeRef candidate = scopeRefGenerator.generate();
        jdbcTemplate.update("""
                INSERT INTO trading_account_authority_scope (
                    authority_scope_ref, lifecycle_status, version,
                    registration_attestation_source, registration_attestation_ref,
                    registered_by_actor_ref, last_operation_id, created_at, updated_at)
                VALUES (?, 'ACTIVE', 0, ?, ?, ?, ?, ?, ?)
                """, candidate.value(), request.attestation().source(),
                request.attestation().reference(), context.actorContext().actorRef().value(),
                request.operationId().value(), timestamp(context), timestamp(context));
        long scopeRowId = lastInsertId();
        long operationRowId = insertOperation(
                request, context, "AUTHORITY_SCOPE", scopeRowId, null,
                candidate.value(), AuthorityOperationOutcome.CREATED, 0);
        insertHistory(operationRowId, request, context, null, AuthorityLifecycle.ACTIVE, null, 0L);
        return new ScopeProvisioningResult(
                candidate, AuthorityOperationOutcome.CREATED, 0, context.occurredAt());
    }

    @Override
    public AccountProvisioningResult registerAccount(
            RegisterAccountSpec spec,
            AuthorizedMutationContext context) {
        AuthorityOperationRequest request = spec.request();
        try {
            Optional<StoredOperation> before = findOperation(request.operationId().value());
            if (before.isPresent()) return accountReplay(before.orElseThrow(), request, context);
            int generatedAttempts = 0;
            int raceAttempts = 0;
            while (true) {
                try {
                    return transactionTemplate.execute(status -> registerAccountTransaction(
                            request, context));
                } catch (DataAccessException exception) {
                    MySqlAuthorityConstraintClassifier.Category category =
                            constraintClassifier.classify(exception);
                    if (category == MySqlAuthorityConstraintClassifier.Category.GENERATED_REF
                            && ++generatedAttempts < MAX_GENERATED_REF_ATTEMPTS) continue;
                    if ((category == MySqlAuthorityConstraintClassifier.Category.OPERATION
                            || category == MySqlAuthorityConstraintClassifier.Category.EXTERNAL_IDENTITY)
                            && ++raceAttempts <= MAX_RACE_RECLASSIFICATIONS) continue;
                    throw unavailable(exception);
                }
            }
        } catch (TradingAccountAuthorityException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    private AccountProvisioningResult registerAccountTransaction(
            AuthorityOperationRequest request,
            AuthorizedMutationContext context) {
        Optional<StoredOperation> replay = findOperation(request.operationId().value());
        if (replay.isPresent()) return accountReplay(replay.orElseThrow(), request, context);
        ScopeRow scope = findScopeByRef(request.scopeRef().value()).orElseThrow(() ->
                new TradingAccountAuthorityException(ResultCode.ACCOUNT_AUTHORITY_SCOPE_NOT_FOUND));
        if (scope.lifecycle() != AuthorityLifecycle.ACTIVE) {
            throw new TradingAccountAuthorityException(ResultCode.ACCOUNT_AUTHORITY_SCOPE_NOT_ELIGIBLE);
        }
        Optional<AccountRow> prior = findAccountByIdentity(scope.id(), request);
        if (prior.isPresent()) {
            AccountRow row = prior.orElseThrow();
            if (!row.attestation().equals(request.attestation())) {
                throw new TradingAccountConflictException(ResultCode.TRADING_ACCOUNT_MAPPING_CONFLICT);
            }
            long operationRowId = insertOperation(
                    request, context, "TRADING_ACCOUNT", null, row.id(), row.ref(),
                    AuthorityOperationOutcome.UNCHANGED, row.version());
            insertHistory(operationRowId, request, context, row.lifecycle(), row.lifecycle(),
                    row.version(), row.version());
            return new AccountProvisioningResult(
                    new TradingAccountRef(row.ref()), AuthorityOperationOutcome.UNCHANGED,
                    row.version(), context.occurredAt());
        }
        TradingAccountRef candidate = accountRefGenerator.generate();
        jdbcTemplate.update("""
                INSERT INTO trading_account_reference (
                    trading_account_ref, authority_scope_id,
                    source_family, source_instance, source_server, source_environment,
                    external_account_key, lifecycle_status, version,
                    registration_attestation_source, registration_attestation_ref,
                    registered_by_actor_ref, last_operation_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 0, ?, ?, ?, ?, ?, ?)
                """, candidate.value(), scope.id(),
                request.namespace().sourceFamily(), request.namespace().sourceInstance(),
                request.namespace().sourceServer(), request.namespace().sourceEnvironment(),
                request.externalAccountKey().utf8Bytes(),
                request.attestation().source(), request.attestation().reference(),
                context.actorContext().actorRef().value(), request.operationId().value(),
                timestamp(context), timestamp(context));
        long accountRowId = lastInsertId();
        long operationRowId = insertOperation(
                request, context, "TRADING_ACCOUNT", null, accountRowId,
                candidate.value(), AuthorityOperationOutcome.CREATED, 0);
        insertHistory(operationRowId, request, context, null, AuthorityLifecycle.ACTIVE, null, 0L);
        return new AccountProvisioningResult(
                candidate, AuthorityOperationOutcome.CREATED, 0, context.occurredAt());
    }

    @Override
    public LifecycleChangeResult changeScopeLifecycle(
            ChangeScopeLifecycleSpec spec,
            AuthorizedMutationContext context) {
        AuthorityOperationRequest request = spec.request();
        try {
            Optional<StoredOperation> before = findOperation(request.operationId().value());
            if (before.isPresent()) return lifecycleReplay(before.orElseThrow(), request, context);
            return transactionTemplate.execute(status -> {
                Optional<StoredOperation> replay = findOperation(request.operationId().value());
                if (replay.isPresent()) return lifecycleReplay(replay.orElseThrow(), request, context);
                ScopeRow row = findScopeByRef(request.scopeRef().value()).orElseThrow(() ->
                        new TradingAccountAuthorityException(ResultCode.ACCOUNT_AUTHORITY_SCOPE_NOT_FOUND));
                AuthorityLifecycle target = validateTransition(row.lifecycle(), row.version(), request);
                int updated = jdbcTemplate.update("""
                        UPDATE trading_account_authority_scope
                        SET lifecycle_status = ?, version = version + 1,
                            last_operation_id = ?, updated_at = ?
                        WHERE id = ? AND version = ? AND lifecycle_status = ?
                        """, target.name(), request.operationId().value(), timestamp(context),
                        row.id(), request.expectedVersion(), row.lifecycle().name());
                if (updated != 1) throw new TradingAccountConflictException(
                        ResultCode.TRADING_ACCOUNT_VERSION_CONFLICT);
                long resultVersion = row.version() + 1;
                long operationRowId = insertOperation(
                        request, context, "AUTHORITY_SCOPE", row.id(), null, row.ref(),
                        AuthorityOperationOutcome.UPDATED, resultVersion);
                insertHistory(operationRowId, request, context, row.lifecycle(), target,
                        row.version(), resultVersion);
                return new LifecycleChangeResult(row.ref(), target,
                        AuthorityOperationOutcome.UPDATED, resultVersion, context.occurredAt());
            });
        } catch (TradingAccountAuthorityException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            if (constraintClassifier.classify(exception)
                    == MySqlAuthorityConstraintClassifier.Category.OPERATION) {
                try {
                    Optional<StoredOperation> replay = findOperation(request.operationId().value());
                    if (replay.isPresent()) {
                        return lifecycleReplay(replay.orElseThrow(), request, context);
                    }
                } catch (DataAccessException replayFailure) {
                    exception.addSuppressed(replayFailure);
                }
            }
            throw unavailable(exception);
        } catch (IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    @Override
    public LifecycleChangeResult changeAccountLifecycle(
            ChangeAccountLifecycleSpec spec,
            AuthorizedMutationContext context) {
        AuthorityOperationRequest request = spec.request();
        try {
            Optional<StoredOperation> before = findOperation(request.operationId().value());
            if (before.isPresent()) return lifecycleReplay(before.orElseThrow(), request, context);
            return transactionTemplate.execute(status -> {
                Optional<StoredOperation> replay = findOperation(request.operationId().value());
                if (replay.isPresent()) return lifecycleReplay(replay.orElseThrow(), request, context);
                AccountRow row = findAccountByRef(request.accountRef().value()).orElseThrow(() ->
                        new TradingAccountAuthorityException(ResultCode.TRADING_ACCOUNT_REFERENCE_NOT_FOUND));
                AuthorityLifecycle target = validateTransition(row.lifecycle(), row.version(), request);
                int updated = jdbcTemplate.update("""
                        UPDATE trading_account_reference
                        SET lifecycle_status = ?, version = version + 1,
                            last_operation_id = ?, updated_at = ?
                        WHERE id = ? AND version = ? AND lifecycle_status = ?
                        """, target.name(), request.operationId().value(), timestamp(context),
                        row.id(), request.expectedVersion(), row.lifecycle().name());
                if (updated != 1) throw new TradingAccountConflictException(
                        ResultCode.TRADING_ACCOUNT_VERSION_CONFLICT);
                long resultVersion = row.version() + 1;
                long operationRowId = insertOperation(
                        request, context, "TRADING_ACCOUNT", null, row.id(), row.ref(),
                        AuthorityOperationOutcome.UPDATED, resultVersion);
                insertHistory(operationRowId, request, context, row.lifecycle(), target,
                        row.version(), resultVersion);
                return new LifecycleChangeResult(row.ref(), target,
                        AuthorityOperationOutcome.UPDATED, resultVersion, context.occurredAt());
            });
        } catch (TradingAccountAuthorityException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            if (constraintClassifier.classify(exception)
                    == MySqlAuthorityConstraintClassifier.Category.OPERATION) {
                try {
                    Optional<StoredOperation> replay = findOperation(request.operationId().value());
                    if (replay.isPresent()) {
                        return lifecycleReplay(replay.orElseThrow(), request, context);
                    }
                } catch (DataAccessException replayFailure) {
                    exception.addSuppressed(replayFailure);
                }
            }
            throw unavailable(exception);
        } catch (IllegalArgumentException exception) {
            throw unavailable(exception);
        }
    }

    private AuthorityLifecycle validateTransition(
            AuthorityLifecycle current,
            long version,
            AuthorityOperationRequest request) {
        if (version != request.expectedVersion()) {
            throw new TradingAccountConflictException(ResultCode.TRADING_ACCOUNT_VERSION_CONFLICT);
        }
        if (version == Long.MAX_VALUE) {
            throw new TradingAccountConflictException(ResultCode.TRADING_ACCOUNT_VERSION_CONFLICT);
        }
        try {
            return current.transitionTo(request.operationType().targetLifecycle());
        } catch (IllegalStateException exception) {
            throw new TradingAccountConflictException(ResultCode.TRADING_ACCOUNT_INVALID_TRANSITION);
        }
    }

    private ScopeProvisioningResult scopeReplay(
            StoredOperation operation,
            AuthorityOperationRequest request,
            AuthorizedMutationContext context) {
        requireMatchingReplay(operation, request, context);
        return new ScopeProvisioningResult(new AccountAuthorityScopeRef(operation.targetRef()),
                operation.outcome(), operation.resultingVersion(), operation.occurredAt());
    }

    private AccountProvisioningResult accountReplay(
            StoredOperation operation,
            AuthorityOperationRequest request,
            AuthorizedMutationContext context) {
        requireMatchingReplay(operation, request, context);
        return new AccountProvisioningResult(new TradingAccountRef(operation.targetRef()),
                operation.outcome(), operation.resultingVersion(), operation.occurredAt());
    }

    private LifecycleChangeResult lifecycleReplay(
            StoredOperation operation,
            AuthorityOperationRequest request,
            AuthorizedMutationContext context) {
        requireMatchingReplay(operation, request, context);
        return new LifecycleChangeResult(operation.targetRef(),
                request.operationType().targetLifecycle(), operation.outcome(),
                operation.resultingVersion(), operation.occurredAt());
    }

    private void requireMatchingReplay(
            StoredOperation operation,
            AuthorityOperationRequest request,
            AuthorizedMutationContext context) {
        if (!operation.operationType().equals(request.operationType().name())
                || !Arrays.equals(operation.fingerprint(), context.fingerprint().value())) {
            throw new TradingAccountConflictException(ResultCode.TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT);
        }
    }

    private long insertOperation(
            AuthorityOperationRequest request,
            AuthorizedMutationContext context,
            String targetType,
            Long scopeId,
            Long accountId,
            String targetRef,
            AuthorityOperationOutcome outcome,
            long resultingVersion) {
        jdbcTemplate.update("""
                INSERT INTO trading_account_authority_operation (
                    operation_id, schema_version, operation_type, semantic_fingerprint,
                    target_type, authority_scope_id, trading_account_id, target_ref,
                    outcome, resulting_version, occurred_at)
                VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, request.operationId().value(), request.operationType().name(),
                context.fingerprint().value(), targetType, scopeId, accountId, targetRef,
                outcome.name(), resultingVersion, timestamp(context));
        return lastInsertId();
    }

    private void insertHistory(
            long operationRowId,
            AuthorityOperationRequest request,
            AuthorizedMutationContext context,
            AuthorityLifecycle beforeLifecycle,
            AuthorityLifecycle afterLifecycle,
            Long beforeVersion,
            Long resultingVersion) {
        jdbcTemplate.update("""
                INSERT INTO trading_account_authority_history (
                    operation_row_id, actor_ref, capability,
                    authorization_evaluated_at, authorization_actor_version,
                    authorization_grant_version, attestation_source, attestation_ref,
                    change_reason, change_ref, before_lifecycle, after_lifecycle,
                    before_version, resulting_version, request_id, trace_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, operationRowId, context.actorContext().actorRef().value(),
                context.capability().value(),
                Timestamp.from(context.authorizationDecision().evaluatedAt()),
                context.authorizationDecision().actorVersion(),
                context.authorizationDecision().grantVersion(),
                request.attestation().source(), request.attestation().reference(),
                request.reason().value(), request.changeReference().value(),
                beforeLifecycle == null ? null : beforeLifecycle.name(), afterLifecycle.name(),
                beforeVersion, resultingVersion, context.actorContext().requestId(),
                context.actorContext().traceId(), timestamp(context));
    }

    private Optional<StoredOperation> findOperation(String operationId) {
        return one(jdbcTemplate.query("""
                SELECT operation_type, semantic_fingerprint, target_ref, outcome,
                       resulting_version, occurred_at
                FROM trading_account_authority_operation WHERE operation_id = ?
                """, (rs, row) -> new StoredOperation(
                        rs.getString("operation_type"), rs.getBytes("semantic_fingerprint"),
                        rs.getString("target_ref"),
                        AuthorityOperationOutcome.valueOf(rs.getString("outcome")),
                        rs.getLong("resulting_version"),
                        rs.getTimestamp("occurred_at").toInstant()), operationId));
    }

    private Optional<ScopeRow> findScopeByRef(String ref) {
        return one(jdbcTemplate.query("""
                SELECT id, authority_scope_ref, lifecycle_status, version,
                       registration_attestation_source, registration_attestation_ref
                FROM trading_account_authority_scope WHERE authority_scope_ref = ?
                """, (rs, row) -> scopeRow(rs.getLong("id"), rs.getString("authority_scope_ref"),
                        rs.getString("lifecycle_status"), rs.getLong("version"),
                        rs.getString("registration_attestation_source"),
                        rs.getString("registration_attestation_ref")), ref));
    }

    private Optional<ScopeRow> findScopeByAttestation(AttestationReference attestation) {
        return one(jdbcTemplate.query("""
                SELECT id, authority_scope_ref, lifecycle_status, version,
                       registration_attestation_source, registration_attestation_ref
                FROM trading_account_authority_scope
                WHERE registration_attestation_source = ? AND registration_attestation_ref = ?
                """, (rs, row) -> scopeRow(rs.getLong("id"), rs.getString("authority_scope_ref"),
                        rs.getString("lifecycle_status"), rs.getLong("version"),
                        rs.getString("registration_attestation_source"),
                        rs.getString("registration_attestation_ref")),
                attestation.source(), attestation.reference()));
    }

    private ScopeRow scopeRow(long id, String ref, String lifecycle, long version,
            String attestationSource, String attestationRef) {
        return new ScopeRow(id, ref, AuthorityLifecycle.valueOf(lifecycle), version,
                new AttestationReference(attestationSource, attestationRef));
    }

    private Optional<AccountRow> findAccountByIdentity(long scopeId, AuthorityOperationRequest request) {
        return one(jdbcTemplate.query("""
                SELECT id, trading_account_ref, lifecycle_status, version,
                       registration_attestation_source, registration_attestation_ref
                FROM trading_account_reference
                WHERE authority_scope_id = ? AND source_family = ? AND source_instance = ?
                  AND source_server = ? AND source_environment = ? AND external_account_key = ?
                """, (rs, row) -> accountRow(rs.getLong("id"), rs.getString("trading_account_ref"),
                        rs.getString("lifecycle_status"), rs.getLong("version"),
                        rs.getString("registration_attestation_source"),
                        rs.getString("registration_attestation_ref")), scopeId,
                request.namespace().sourceFamily(), request.namespace().sourceInstance(),
                request.namespace().sourceServer(), request.namespace().sourceEnvironment(),
                request.externalAccountKey().utf8Bytes()));
    }

    private Optional<AccountRow> findAccountByRef(String ref) {
        return one(jdbcTemplate.query("""
                SELECT id, trading_account_ref, lifecycle_status, version,
                       registration_attestation_source, registration_attestation_ref
                FROM trading_account_reference WHERE trading_account_ref = ?
                """, (rs, row) -> accountRow(rs.getLong("id"), rs.getString("trading_account_ref"),
                        rs.getString("lifecycle_status"), rs.getLong("version"),
                        rs.getString("registration_attestation_source"),
                        rs.getString("registration_attestation_ref")), ref));
    }

    private AccountRow accountRow(long id, String ref, String lifecycle, long version,
            String attestationSource, String attestationRef) {
        return new AccountRow(id, ref, AuthorityLifecycle.valueOf(lifecycle), version,
                new AttestationReference(attestationSource, attestationRef));
    }

    private <T> Optional<T> one(List<T> rows) {
        if (rows.size() > 1) throw new TradingAccountAuthorityUnavailableException();
        return rows.stream().findFirst();
    }

    private long lastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null || id <= 0) throw new TradingAccountAuthorityUnavailableException();
        return id;
    }

    private Timestamp timestamp(AuthorizedMutationContext context) {
        return Timestamp.from(context.occurredAt());
    }

    private TradingAccountAuthorityUnavailableException unavailable(Throwable cause) {
        return new TradingAccountAuthorityUnavailableException(cause);
    }

    private record StoredOperation(
            String operationType,
            byte[] fingerprint,
            String targetRef,
            AuthorityOperationOutcome outcome,
            long resultingVersion,
            java.time.Instant occurredAt) {
    }

    private record ScopeRow(
            long id,
            String ref,
            AuthorityLifecycle lifecycle,
            long version,
            AttestationReference attestation) {
    }

    private record AccountRow(
            long id,
            String ref,
            AuthorityLifecycle lifecycle,
            long version,
            AttestationReference attestation) {
    }
}
