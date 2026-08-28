package com.brokeros.risk.tradingaccount.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.tradingaccount.application.AuthorityScopeState;
import com.brokeros.risk.tradingaccount.application.CompletedAuthorityOperation;
import com.brokeros.risk.tradingaccount.application.EligibilityPersistenceView;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountState;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityQueryPort;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AttestationReference;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountIdentity;
import com.brokeros.risk.tradingaccount.domain.ManifestFingerprint;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTradingAccountAuthorityQueryAdapter implements TradingAccountAuthorityQueryPort {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTradingAccountAuthorityQueryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<CompletedAuthorityOperation> findOperation(AuthorityOperationId id) {
        return bounded(() -> jdbcTemplate.query("""
                SELECT operation_type, semantic_fingerprint, target_ref, outcome,
                       resulting_version, occurred_at
                FROM trading_account_authority_operation
                WHERE operation_id = ?
                """, (rs, row) -> new CompletedAuthorityOperation(
                        AuthorityOperationType.valueOf(rs.getString("operation_type")),
                        new ManifestFingerprint(rs.getBytes("semantic_fingerprint")),
                        rs.getString("target_ref"),
                        AuthorityOperationOutcome.valueOf(rs.getString("outcome")),
                        rs.getLong("resulting_version"),
                        rs.getTimestamp("occurred_at").toInstant()), id.value()));
    }

    @Override
    public Optional<AuthorityScopeState> findScope(AccountAuthorityScopeRef ref) {
        return bounded(() -> jdbcTemplate.query("""
                SELECT authority_scope_ref, lifecycle_status, version,
                       registration_attestation_source, registration_attestation_ref,
                       last_operation_id
                FROM trading_account_authority_scope
                WHERE authority_scope_ref = ?
                """, (rs, row) -> new AuthorityScopeState(
                        new AccountAuthorityScopeRef(rs.getString("authority_scope_ref")),
                        AuthorityLifecycle.valueOf(rs.getString("lifecycle_status")),
                        rs.getLong("version"),
                        new AttestationReference(
                                rs.getString("registration_attestation_source"),
                                rs.getString("registration_attestation_ref")),
                        new AuthorityOperationId(rs.getString("last_operation_id"))), ref.value()));
    }

    @Override
    public Optional<TradingAccountState> findByExternalIdentity(ExternalAccountIdentity identity) {
        return bounded(() -> jdbcTemplate.query("""
                SELECT a.trading_account_ref, a.lifecycle_status, a.version,
                       a.registration_attestation_source, a.registration_attestation_ref,
                       a.last_operation_id
                FROM trading_account_reference a
                JOIN trading_account_authority_scope s ON s.id = a.authority_scope_id
                WHERE s.authority_scope_ref = ? AND a.source_family = ?
                  AND a.source_instance = ? AND a.source_server = ?
                  AND a.source_environment = ? AND a.external_account_key = ?
                """, (rs, row) -> new TradingAccountState(
                        new TradingAccountRef(rs.getString("trading_account_ref")),
                        AuthorityLifecycle.valueOf(rs.getString("lifecycle_status")),
                        rs.getLong("version"),
                        new AttestationReference(
                                rs.getString("registration_attestation_source"),
                                rs.getString("registration_attestation_ref")),
                        new AuthorityOperationId(rs.getString("last_operation_id"))),
                identity.scopeRef().value(),
                identity.namespace().sourceFamily(),
                identity.namespace().sourceInstance(),
                identity.namespace().sourceServer(),
                identity.namespace().sourceEnvironment(),
                identity.externalAccountKey().utf8Bytes()));
    }

    @Override
    public Optional<EligibilityPersistenceView> findEligibility(TradingAccountRef ref) {
        return bounded(() -> jdbcTemplate.query("""
                SELECT a.trading_account_ref,
                       a.lifecycle_status AS account_lifecycle,
                       a.version AS account_version,
                       a.last_operation_id AS account_last_operation_id,
                       s.lifecycle_status AS scope_lifecycle,
                       s.version AS scope_version,
                       s.last_operation_id AS scope_last_operation_id
                FROM trading_account_reference a
                JOIN trading_account_authority_scope s ON s.id = a.authority_scope_id
                WHERE a.trading_account_ref = ?
                """, (rs, row) -> new EligibilityPersistenceView(
                        new TradingAccountRef(rs.getString("trading_account_ref")),
                        AuthorityLifecycle.valueOf(rs.getString("account_lifecycle")),
                        rs.getLong("account_version"),
                        new AuthorityOperationId(rs.getString("account_last_operation_id")),
                        AuthorityLifecycle.valueOf(rs.getString("scope_lifecycle")),
                        rs.getLong("scope_version"),
                        new AuthorityOperationId(rs.getString("scope_last_operation_id"))), ref.value()));
    }

    private <T> Optional<T> bounded(Query<T> query) {
        try {
            List<T> rows = query.execute();
            if (rows.size() > 1) {
                throw new TradingAccountAuthorityUnavailableException();
            }
            return rows.stream().findFirst();
        } catch (TradingAccountAuthorityUnavailableException exception) {
            throw exception;
        } catch (DataAccessException | IllegalArgumentException exception) {
            throw new TradingAccountAuthorityUnavailableException(exception);
        }
    }

    @FunctionalInterface
    private interface Query<T> {
        List<T> execute();
    }
}
