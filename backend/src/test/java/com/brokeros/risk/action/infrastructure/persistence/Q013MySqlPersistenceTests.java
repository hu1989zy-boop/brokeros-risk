package com.brokeros.risk.action.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionCapabilities;
import com.brokeros.risk.action.application.ActionConflictException;
import com.brokeros.risk.action.application.ActionException;
import com.brokeros.risk.action.application.ActionFingerprintFactory;
import com.brokeros.risk.action.application.AuthorizedMutationContext;
import com.brokeros.risk.action.application.RecordActionSpec;
import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import com.brokeros.risk.action.domain.IntentText;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q013_MYSQL_TEST_URL", matches = ".+")
class Q013MySqlPersistenceTests {

    private static final Instant NOW = Instant.parse("2026-09-01T01:00:00Z");
    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String DECISION =
            "dec-00000000-0000-4000-8000-000000000002";
    private static final ActionFingerprintFactory FINGERPRINTS =
            new ActionFingerprintFactory();

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
    void recordReplayQueryAndAccessLogAreDurableAndConsistent() {
        JdbcActionMutationAdapter adapter = adapter(actionRef(1));
        RecordActionSpec spec = recordSpec(operationId(1), "intent");
        CompletedActionOperation recorded = adapter.record(spec, context(spec));

        assertThat(recorded.actionRef()).isEqualTo(actionRef(1));
        assertThat(recorded.outcome()).isEqualTo(ActionOperationOutcome.CREATED);
        assertThat(recorded.actionRecord().decisionRef()).isEqualTo(new DecisionRef(DECISION));
        assertThat(recorded.actionRecord().status().name()).isEqualTo("PROPOSED");
        assertThat(adapter.record(spec, context(spec))).isEqualTo(recorded);

        RecordActionSpec changed = recordSpec(operationId(1), "changed");
        assertThatThrownBy(() -> adapter.record(spec, context(changed)))
                .isInstanceOf(ActionConflictException.class)
                .satisfies(error -> assertThat(((ActionException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_IDEMPOTENCY_CONFLICT));

        JdbcActionQueryAdapter query = new JdbcActionQueryAdapter(jdbc);
        assertThat(query.findByRef(recorded.actionRef()).orElseThrow())
                .isEqualTo(recorded.actionRecord());
        assertThat(query.findOperation(spec.operationId()).orElseThrow())
                .isEqualTo(recorded);

        new JdbcActionAccessLogAdapter(
                jdbc, new DataSourceTransactionManager(dataSource))
                .recordFullDetailAccess(recorded.actionRef(), new ActorRef(ACTOR), NOW);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_access_log", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_operation", Integer.class)).isEqualTo(1);
    }

    @Test
    void generatedRefCollisionRetriesExactlyThreeTimesAndNeverOverwrites() {
        RecordActionSpec first = recordSpec(operationId(1), "first");
        adapter(actionRef(1)).record(first, context(first));
        AtomicInteger generated = new AtomicInteger();
        JdbcActionMutationAdapter colliding = new JdbcActionMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource), () -> {
                    generated.incrementAndGet();
                    return actionRef(1);
                });
        RecordActionSpec second = recordSpec(operationId(2), "second");

        assertThatThrownBy(() -> colliding.record(second, context(second)))
                .isInstanceOf(ActionAuthorityUnavailableException.class);
        assertThat(generated).hasValue(3);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_operation", Integer.class)).isEqualTo(1);
    }

    @Test
    void operationFailureRollsBackActionAndLedgerAtomically() {
        jdbc.execute("""
                CREATE TRIGGER q013_force_operation_failure
                BEFORE INSERT ON action_operation
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced action operation failure'
                """);
        RecordActionSpec spec = recordSpec(operationId(1), "intent");
        try {
            assertThatThrownBy(() -> adapter(actionRef(1)).record(spec, context(spec)))
                    .isInstanceOf(ActionAuthorityUnavailableException.class);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM action_record", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM action_operation", Integer.class)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER q013_force_operation_failure");
        }
    }

    @Test
    void concurrentSameOperationReturnsOneCommitAndOneReplay() throws Exception {
        RecordActionSpec spec = recordSpec(operationId(1), "intent");
        AuthorizedMutationContext context = context(spec);
        JdbcActionMutationAdapter firstAdapter = adapter(actionRef(1));
        JdbcActionMutationAdapter secondAdapter = adapter(actionRef(2));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                start.await();
                return firstAdapter.record(spec, context);
            });
            var second = executor.submit(() -> {
                start.await();
                return secondAdapter.record(spec, context);
            });
            start.countDown();
            assertThat(first.get()).isEqualTo(second.get());
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_operation", Integer.class)).isEqualTo(1);
    }

    @Test
    void failedAccessLogIsIsolatedFromConcurrentUnrelatedRecording() throws Exception {
        RecordActionSpec firstSpec = recordSpec(operationId(1), "first");
        CompletedActionOperation first = adapter(actionRef(1))
                .record(firstSpec, context(firstSpec));
        jdbc.execute("""
                CREATE TRIGGER q013_force_access_log_failure
                BEFORE INSERT ON action_access_log
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced action access log failure'
                """);
        JdbcActionAccessLogAdapter accessLog = new JdbcActionAccessLogAdapter(
                jdbc, new DataSourceTransactionManager(dataSource));
        RecordActionSpec secondSpec = recordSpec(operationId(2), "unrelated");
        JdbcActionMutationAdapter recording = adapter(actionRef(2));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var audit = executor.submit(() -> {
                start.await();
                try {
                    accessLog.recordFullDetailAccess(
                            first.actionRef(), new ActorRef(ACTOR), NOW);
                    return null;
                } catch (ActionAuthorityUnavailableException exception) {
                    return exception;
                }
            });
            var unrelated = executor.submit(() -> {
                start.await();
                return recording.record(secondSpec, context(secondSpec));
            });
            start.countDown();
            assertThat(audit.get()).isInstanceOf(ActionAuthorityUnavailableException.class);
            assertThat(unrelated.get().outcome()).isEqualTo(ActionOperationOutcome.CREATED);
        } finally {
            jdbc.execute("DROP TRIGGER q013_force_access_log_failure");
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_access_log", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_record", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_operation", Integer.class)).isEqualTo(2);
    }

    @Test
    void malformedStoredUtf8FailsClosedInsteadOfReturningReplacementContent() {
        RecordActionSpec spec = recordSpec(operationId(1), "intent");
        CompletedActionOperation recorded = adapter(actionRef(1))
                .record(spec, context(spec));
        jdbc.update(
                "UPDATE action_record SET intent_text = ? WHERE action_ref = ?",
                new byte[] {(byte) 0xc3, 0x28}, recorded.actionRef().value());

        assertThatThrownBy(() -> new JdbcActionQueryAdapter(jdbc)
                .findByRef(recorded.actionRef()))
                .isInstanceOf(ActionAuthorityUnavailableException.class);
    }

    private JdbcActionMutationAdapter adapter(ActionRef... refs) {
        var sequence = new ArrayList<>(List.of(refs));
        return new JdbcActionMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource), sequence::removeFirst);
    }

    private RecordActionSpec recordSpec(String operationId, String intent) {
        return new RecordActionSpec(
                new ActionOperationId(operationId),
                new DecisionRef(DECISION),
                new IntentText(intent));
    }

    private AuthorizedMutationContext context(RecordActionSpec spec) {
        ActorContext actor = new ActorContext(
                new ActorRef(ACTOR), ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"), null, null);
        AuthorizationDecision decision = AuthorizationDecision.allow(
                actor.actorRef(), ActionCapabilities.RECORD, NOW, 1, 1);
        return new AuthorizedMutationContext(
                spec.operationId(),
                FINGERPRINTS.forRecord(
                        spec.decisionRef().value(), spec.intentText().value()),
                actor, decision, ActionCapabilities.RECORD, NOW);
    }

    private ActionRef actionRef(int value) {
        return new ActionRef(
                "act-00000000-0000-4000-8000-" + String.format("%012d", value));
    }

    private String operationId(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q013_MYSQL_TEST_URL"));
        source.setUsername(required("Q013_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q013_MYSQL_TEST_PASSWORD"));
        return source;
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
