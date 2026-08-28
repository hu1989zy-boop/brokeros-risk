package com.brokeros.risk.tradingaccount.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.application.AccountProvisioningResult;
import com.brokeros.risk.tradingaccount.application.AuthorizedMutationContext;
import com.brokeros.risk.tradingaccount.application.AuthorityOperationRequest;
import com.brokeros.risk.tradingaccount.application.ChangeAccountLifecycleSpec;
import com.brokeros.risk.tradingaccount.application.LifecycleChangeResult;
import com.brokeros.risk.tradingaccount.application.ManifestFingerprintFactory;
import com.brokeros.risk.tradingaccount.application.RegisterAccountSpec;
import com.brokeros.risk.tradingaccount.application.RegisterScopeSpec;
import com.brokeros.risk.tradingaccount.application.ScopeProvisioningResult;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityException;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountCapabilities;
import com.brokeros.risk.tradingaccount.domain.AccountAuthorityScopeRef;
import com.brokeros.risk.tradingaccount.domain.AttestationReference;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationId;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationOutcome;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import com.brokeros.risk.tradingaccount.domain.ChangeReason;
import com.brokeros.risk.tradingaccount.domain.ChangeReference;
import com.brokeros.risk.tradingaccount.domain.ExternalAccountKey;
import com.brokeros.risk.tradingaccount.domain.ManifestFingerprint;
import com.brokeros.risk.tradingaccount.domain.SourceNamespace;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q010_MYSQL_TEST_URL", matches = ".+")
class Q010MySqlIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-08-27T10:00:00Z");
    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final ManifestFingerprintFactory FINGERPRINTS = new ManifestFingerprintFactory();

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateDisposableDatabase() {
        dataSource = dataSource();
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void migrationCreatesExactShapeConstraintsAndRestartsCleanly() {
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name LIKE 'trading_account%'
                ORDER BY table_name
                """, String.class)).containsExactly(
                        "trading_account_authority_history",
                        "trading_account_authority_operation",
                        "trading_account_authority_scope",
                        "trading_account_reference");
        assertThat(jdbc.queryForObject("""
                SELECT collation_name FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'trading_account_reference'
                  AND column_name = 'trading_account_ref'
                """, String.class)).isEqualTo("ascii_bin");
        assertThat(jdbc.queryForObject("""
                SELECT data_type FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'trading_account_reference'
                  AND column_name = 'external_account_key'
                """, String.class)).isEqualTo("varbinary");
        assertThat(jdbc.queryForList("""
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name IN ('trading_account_authority_scope', 'trading_account_reference',
                                     'trading_account_authority_operation', 'trading_account_authority_history')
                """, String.class)).contains(
                        "uk_ta_authority_scope_attestation",
                        "uk_trading_account_reference_external_identity",
                        "uk_ta_authority_operation_id",
                        "uk_ta_authority_history_operation");

        Flyway.configure().dataSource(dataSource).load().validate();
        assertThat(Flyway.configure().dataSource(dataSource).load().migrate().migrationsExecuted)
                .isZero();
    }

    @Test
    void registrationReplayCompatibleDuplicateAndMappingConflictAreDurable() {
        JdbcTradingAccountAuthorityMutationAdapter adapter = adapter(
                List.of(scopeRef(1)), List.of(accountRef(1)));
        AuthorityOperationRequest scopeRequest = scopeRegistration(operationId(1), "scope-attestation");
        ScopeProvisioningResult scope = adapter.registerScope(
                new RegisterScopeSpec(scopeRequest), context(scopeRequest));
        assertThat(scope.outcome()).isEqualTo(AuthorityOperationOutcome.CREATED);

        ScopeProvisioningResult replay = adapter.registerScope(
                new RegisterScopeSpec(scopeRequest), context(scopeRequest));
        assertThat(replay).isEqualTo(scope);
        AuthorityOperationRequest compatibleScope = scopeRegistration(operationId(2), "scope-attestation");
        assertThat(adapter.registerScope(new RegisterScopeSpec(compatibleScope), context(compatibleScope)).outcome())
                .isEqualTo(AuthorityOperationOutcome.UNCHANGED);

        AuthorityOperationRequest accountRequest = accountRegistration(
                operationId(3), scope.scopeRef(), "Exact-001", "account-attestation");
        AccountProvisioningResult account = adapter.registerAccount(
                new RegisterAccountSpec(accountRequest), context(accountRequest));
        assertThat(account.outcome()).isEqualTo(AuthorityOperationOutcome.CREATED);
        AuthorityOperationRequest compatibleAccount = accountRegistration(
                operationId(4), scope.scopeRef(), "Exact-001", "account-attestation");
        assertThat(adapter.registerAccount(
                new RegisterAccountSpec(compatibleAccount), context(compatibleAccount)).outcome())
                .isEqualTo(AuthorityOperationOutcome.UNCHANGED);
        AuthorityOperationRequest conflict = accountRegistration(
                operationId(5), scope.scopeRef(), "Exact-001", "different-attestation");
        assertThatThrownBy(() -> adapter.registerAccount(
                new RegisterAccountSpec(conflict), context(conflict)))
                .isInstanceOf(TradingAccountAuthorityException.class)
                .satisfies(error -> assertThat(((TradingAccountAuthorityException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_ACCOUNT_MAPPING_CONFLICT));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_reference", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT external_account_key FROM trading_account_reference WHERE trading_account_ref = ?",
                byte[].class, account.tradingAccountRef().value()))
                .containsExactly(new ExternalAccountKey("Exact-001").utf8Bytes());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_authority_operation", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_authority_history", Integer.class)).isEqualTo(4);
    }

    @Test
    void changedReplayLifecycleCasAndRetiredTerminalStateFailClosed() {
        JdbcTradingAccountAuthorityMutationAdapter adapter = adapter(
                List.of(scopeRef(1)), List.of(accountRef(1)));
        AuthorityOperationRequest scopeRequest = scopeRegistration(operationId(1), "scope-attestation");
        ScopeProvisioningResult scope = adapter.registerScope(
                new RegisterScopeSpec(scopeRequest), context(scopeRequest));
        AuthorityOperationRequest accountRequest = accountRegistration(
                operationId(2), scope.scopeRef(), "Exact-001", "account-attestation");
        AccountProvisioningResult account = adapter.registerAccount(
                new RegisterAccountSpec(accountRequest), context(accountRequest));

        AuthorityOperationRequest changedReplay = accountRegistration(
                operationId(2), scope.scopeRef(), "Changed-001", "account-attestation");
        assertThatThrownBy(() -> adapter.registerAccount(
                new RegisterAccountSpec(changedReplay), context(changedReplay)))
                .satisfies(error -> assertThat(((TradingAccountAuthorityException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_ACCOUNT_IDEMPOTENCY_CONFLICT));

        AuthorityOperationRequest retire = lifecycle(
                operationId(3), AuthorityOperationType.RETIRE_TRADING_ACCOUNT,
                account.tradingAccountRef(), 0);
        assertThat(adapter.changeAccountLifecycle(
                new ChangeAccountLifecycleSpec(retire), context(retire)).resultingVersion()).isEqualTo(1);
        AuthorityOperationRequest stale = lifecycle(
                operationId(4), AuthorityOperationType.REACTIVATE_TRADING_ACCOUNT,
                account.tradingAccountRef(), 0);
        assertThatThrownBy(() -> adapter.changeAccountLifecycle(
                new ChangeAccountLifecycleSpec(stale), context(stale)))
                .satisfies(error -> assertThat(((TradingAccountAuthorityException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_ACCOUNT_VERSION_CONFLICT));
        AuthorityOperationRequest terminal = lifecycle(
                operationId(5), AuthorityOperationType.REACTIVATE_TRADING_ACCOUNT,
                account.tradingAccountRef(), 1);
        assertThatThrownBy(() -> adapter.changeAccountLifecycle(
                new ChangeAccountLifecycleSpec(terminal), context(terminal)))
                .satisfies(error -> assertThat(((TradingAccountAuthorityException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_ACCOUNT_INVALID_TRANSITION));
    }

    @Test
    void concurrentIdenticalRegistrationConvergesToOneMappingAndCompleteHistory() throws Exception {
        JdbcTradingAccountAuthorityMutationAdapter setup = adapter(
                List.of(scopeRef(1)), List.of(accountRef(1)));
        AuthorityOperationRequest scopeRequest = scopeRegistration(operationId(1), "scope-attestation");
        ScopeProvisioningResult scope = setup.registerScope(
                new RegisterScopeSpec(scopeRequest), context(scopeRequest));

        AuthorityOperationRequest first = accountRegistration(
                operationId(2), scope.scopeRef(), "Concurrent-001", "account-attestation");
        AuthorityOperationRequest second = accountRegistration(
                operationId(3), scope.scopeRef(), "Concurrent-001", "account-attestation");
        JdbcTradingAccountAuthorityMutationAdapter firstAdapter = adapter(
                List.of(scopeRef(2)), List.of(accountRef(2)));
        JdbcTradingAccountAuthorityMutationAdapter secondAdapter = adapter(
                List.of(scopeRef(3)), List.of(accountRef(3)));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var one = executor.submit(() -> {
                start.await();
                return firstAdapter.registerAccount(new RegisterAccountSpec(first), context(first));
            });
            var two = executor.submit(() -> {
                start.await();
                return secondAdapter.registerAccount(new RegisterAccountSpec(second), context(second));
            });
            start.countDown();
            assertThat(List.of(one.get().outcome(), two.get().outcome()))
                    .containsExactlyInAnyOrder(
                            AuthorityOperationOutcome.CREATED,
                            AuthorityOperationOutcome.UNCHANGED);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_reference", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_authority_operation", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_authority_history", Integer.class)).isEqualTo(3);
    }

    @Test
    void concurrentDuplicateOperationDeliveryReturnsOneDurableReplayResult() throws Exception {
        JdbcTradingAccountAuthorityMutationAdapter setup = adapter(
                List.of(scopeRef(1)), List.of(accountRef(1)));
        AuthorityOperationRequest scopeRequest = scopeRegistration(operationId(1), "scope-attestation");
        ScopeProvisioningResult scope = setup.registerScope(
                new RegisterScopeSpec(scopeRequest), context(scopeRequest));
        AuthorityOperationRequest request = accountRegistration(
                operationId(2), scope.scopeRef(), "Duplicate-001", "account-attestation");
        JdbcTradingAccountAuthorityMutationAdapter firstAdapter = adapter(
                List.of(), List.of(accountRef(2)));
        JdbcTradingAccountAuthorityMutationAdapter secondAdapter = adapter(
                List.of(), List.of(accountRef(3)));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var one = executor.submit(() -> {
                start.await();
                return firstAdapter.registerAccount(new RegisterAccountSpec(request), context(request));
            });
            var two = executor.submit(() -> {
                start.await();
                return secondAdapter.registerAccount(new RegisterAccountSpec(request), context(request));
            });
            start.countDown();
            AccountProvisioningResult first = one.get();
            AccountProvisioningResult second = two.get();
            assertThat(first).isEqualTo(second);
            assertThat(first.outcome()).isEqualTo(AuthorityOperationOutcome.CREATED);
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_account_reference", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_account_authority_operation", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_account_authority_history", Integer.class)).isEqualTo(2);
    }

    @Test
    void historyFailureRollsBackStateAndOperation() {
        jdbc.execute("""
                CREATE TRIGGER q010_force_history_failure
                BEFORE INSERT ON trading_account_authority_history
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced history failure'
                """);
        JdbcTradingAccountAuthorityMutationAdapter adapter = adapter(
                List.of(scopeRef(1)), List.of(accountRef(1)));
        AuthorityOperationRequest request = scopeRegistration(operationId(1), "scope-attestation");
        try {
            assertThatThrownBy(() -> adapter.registerScope(
                    new RegisterScopeSpec(request), context(request)))
                    .isInstanceOf(TradingAccountAuthorityUnavailableException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_authority_scope", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_authority_operation", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_account_authority_history", Integer.class)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER q010_force_history_failure");
        }
    }

    @Test
    void generatedReferenceCollisionRetriesOnlyThreeTimesAndNeverSplitsIdentity() {
        AccountAuthorityScopeRef collision = scopeRef(1);
        JdbcTradingAccountAuthorityMutationAdapter setup = adapter(
                List.of(collision), List.of());
        AuthorityOperationRequest first = scopeRegistration(operationId(1), "scope-attestation-1");
        setup.registerScope(new RegisterScopeSpec(first), context(first));

        JdbcTradingAccountAuthorityMutationAdapter colliding = adapter(
                List.of(collision, collision, collision), List.of());
        AuthorityOperationRequest second = scopeRegistration(operationId(2), "scope-attestation-2");
        assertThatThrownBy(() -> colliding.registerScope(
                new RegisterScopeSpec(second), context(second)))
                .isInstanceOf(TradingAccountAuthorityUnavailableException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_account_authority_scope", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_account_authority_operation", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentLifecycleCasPreventsLostUpdateAndRetainsHistoricalResolution() throws Exception {
        JdbcTradingAccountAuthorityMutationAdapter setup = adapter(
                List.of(scopeRef(1)), List.of(accountRef(1)));
        AuthorityOperationRequest scopeRequest = scopeRegistration(operationId(1), "scope-attestation");
        ScopeProvisioningResult scope = setup.registerScope(
                new RegisterScopeSpec(scopeRequest), context(scopeRequest));
        AuthorityOperationRequest accountRequest = accountRegistration(
                operationId(2), scope.scopeRef(), "Lifecycle-001", "account-attestation");
        AccountProvisioningResult account = setup.registerAccount(
                new RegisterAccountSpec(accountRequest), context(accountRequest));
        AuthorityOperationRequest deactivate = lifecycle(
                operationId(3), AuthorityOperationType.DEACTIVATE_TRADING_ACCOUNT,
                account.tradingAccountRef(), 0);
        AuthorityOperationRequest retire = lifecycle(
                operationId(4), AuthorityOperationType.RETIRE_TRADING_ACCOUNT,
                account.tradingAccountRef(), 0);
        JdbcTradingAccountAuthorityMutationAdapter firstAdapter = adapter(List.of(), List.of());
        JdbcTradingAccountAuthorityMutationAdapter secondAdapter = adapter(List.of(), List.of());
        CountDownLatch start = new CountDownLatch(1);
        List<Object> results;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var one = executor.submit(() -> runLifecycle(firstAdapter, deactivate, start));
            var two = executor.submit(() -> runLifecycle(secondAdapter, retire, start));
            start.countDown();
            results = List.of(one.get(), two.get());
        }
        assertThat(results.stream().filter(LifecycleChangeResult.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(TradingAccountAuthorityException.class::isInstance)).hasSize(1);
        TradingAccountAuthorityException conflict = (TradingAccountAuthorityException) results.stream()
                .filter(TradingAccountAuthorityException.class::isInstance).findFirst().orElseThrow();
        assertThat(conflict.getResultCode()).isEqualTo(ResultCode.TRADING_ACCOUNT_VERSION_CONFLICT);
        assertThat(jdbc.queryForObject(
                "SELECT version FROM trading_account_reference WHERE trading_account_ref = ?",
                Long.class, account.tradingAccountRef().value())).isEqualTo(1);
        assertThat(new JdbcTradingAccountAuthorityQueryAdapter(jdbc)
                .findEligibility(account.tradingAccountRef())).isPresent();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_account_authority_history", Integer.class)).isEqualTo(3);
    }

    @Test
    void operationOutcomeFailureRollsBackCurrentState() {
        jdbc.execute("""
                CREATE TRIGGER q010_force_operation_failure
                BEFORE INSERT ON trading_account_authority_operation
                FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'forced operation failure'
                """);
        JdbcTradingAccountAuthorityMutationAdapter adapter = adapter(
                List.of(scopeRef(1)), List.of());
        AuthorityOperationRequest request = scopeRegistration(operationId(1), "scope-attestation");
        try {
            assertThatThrownBy(() -> adapter.registerScope(
                    new RegisterScopeSpec(request), context(request)))
                    .isInstanceOf(TradingAccountAuthorityUnavailableException.class);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM trading_account_authority_scope", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM trading_account_authority_operation", Integer.class)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER q010_force_operation_failure");
        }
    }

    @Test
    void mysqlCheckEnforcementUsesExactVendorError() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO trading_account_authority_scope (
                    authority_scope_ref, lifecycle_status, version,
                    registration_attestation_source, registration_attestation_ref,
                    registered_by_actor_ref, last_operation_id, created_at, updated_at)
                VALUES (?, 'INVALID', 0, 'broker-record', 'approval', ?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, scopeRef(1).value(), ACTOR, operationId(1)))
                .isInstanceOf(DataAccessException.class)
                .satisfies(error -> {
                    SQLException sql = rootSql((Throwable) error);
                    assertThat((Object) sql).isNotNull();
                    assertThat(sql.getErrorCode()).isEqualTo(3819);
                    assertThat(sql.getSQLState()).isEqualTo("HY000");
                });
    }

    private JdbcTradingAccountAuthorityMutationAdapter adapter(
            List<AccountAuthorityScopeRef> scopeRefs,
            List<TradingAccountRef> accountRefs) {
        var scopeSequence = new ArrayList<>(scopeRefs);
        var accountSequence = new ArrayList<>(accountRefs);
        return new JdbcTradingAccountAuthorityMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource),
                () -> scopeSequence.removeFirst(),
                () -> accountSequence.removeFirst());
    }

    private Object runLifecycle(
            JdbcTradingAccountAuthorityMutationAdapter adapter,
            AuthorityOperationRequest request,
            CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            return adapter.changeAccountLifecycle(
                    new ChangeAccountLifecycleSpec(request), context(request));
        } catch (TradingAccountAuthorityException exception) {
            return exception;
        }
    }

    private AuthorizedMutationContext context(AuthorityOperationRequest request) {
        Capability capability = request.operationType().isRegistration()
                ? TradingAccountCapabilities.REGISTER : TradingAccountCapabilities.CHANGE_LIFECYCLE;
        ActorContext actor = new ActorContext(
                new ActorRef(ACTOR), ActorType.SERVICE,
                new ExternalPrincipalKey("urn:brokeros:risk:internal-service",
                        "trading-account-reference-provisioner", ActorType.SERVICE),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"), null, null);
        AuthorizationDecision decision = AuthorizationDecision.allow(
                actor.actorRef(), capability, NOW, 1, 1);
        ManifestFingerprint fingerprint = FINGERPRINTS.create(request);
        return new AuthorizedMutationContext(fingerprint, actor, decision, capability, NOW);
    }

    private AuthorityOperationRequest scopeRegistration(String operationId, String attestationRef) {
        return new AuthorityOperationRequest(
                1, new AuthorityOperationId(operationId),
                AuthorityOperationType.REGISTER_AUTHORITY_SCOPE,
                null, null, null, null, null,
                new AttestationReference("broker-record", attestationRef),
                new ChangeReason("Controlled registration"), new ChangeReference("change-1"));
    }

    private AuthorityOperationRequest accountRegistration(
            String operationId,
            AccountAuthorityScopeRef scopeRef,
            String externalKey,
            String attestationRef) {
        return new AuthorityOperationRequest(
                1, new AuthorityOperationId(operationId),
                AuthorityOperationType.REGISTER_TRADING_ACCOUNT,
                scopeRef, null,
                new SourceNamespace("platform", "instance", "server-1", "production"),
                new ExternalAccountKey(externalKey), null,
                new AttestationReference("broker-record", attestationRef),
                new ChangeReason("Controlled registration"), new ChangeReference("change-1"));
    }

    private AuthorityOperationRequest lifecycle(
            String operationId,
            AuthorityOperationType type,
            TradingAccountRef accountRef,
            long expectedVersion) {
        return new AuthorityOperationRequest(
                1, new AuthorityOperationId(operationId), type,
                null, accountRef, null, null, expectedVersion,
                new AttestationReference("broker-record", "lifecycle-approval"),
                new ChangeReason("Controlled lifecycle change"), new ChangeReference("change-2"));
    }

    private String operationId(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private AccountAuthorityScopeRef scopeRef(int value) {
        return new AccountAuthorityScopeRef("aas-" + operationId(100 + value));
    }

    private TradingAccountRef accountRef(int value) {
        return new TradingAccountRef("ta-" + operationId(200 + value));
    }

    private SQLException rootSql(Throwable error) {
        SQLException found = null;
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException) found = sqlException;
        }
        return found;
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q010_MYSQL_TEST_URL"));
        source.setUsername(required("Q010_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q010_MYSQL_TEST_PASSWORD"));
        return source;
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value;
    }
}
