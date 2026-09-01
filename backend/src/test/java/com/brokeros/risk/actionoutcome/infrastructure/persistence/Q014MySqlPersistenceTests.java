package com.brokeros.risk.actionoutcome.infrastructure.persistence;

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

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeAuthorityUnavailableException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeCapabilities;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeConflictException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeFingerprintFactory;
import com.brokeros.risk.actionoutcome.application.AuthorizedMutationContext;
import com.brokeros.risk.actionoutcome.application.RecordActionOutcomeSpec;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;
import com.brokeros.risk.actionoutcome.domain.OutcomeText;
import com.brokeros.risk.api.ResultCode;
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

@EnabledIfEnvironmentVariable(named = "Q014_MYSQL_TEST_URL", matches = ".+")
class Q014MySqlPersistenceTests {

    private static final Instant NOW = Instant.parse("2026-09-01T01:00:00Z");
    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final ActionRef ACTION = new ActionRef(
            "act-00000000-0000-4000-8000-000000000002");
    private static final ActionOutcomeFingerprintFactory FINGERPRINTS =
            new ActionOutcomeFingerprintFactory();

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
        JdbcActionOutcomeMutationAdapter adapter = adapter(actionOutcomeRef(1));
        RecordActionOutcomeSpec spec = recordSpec(operationId(1), "outcome");
        CompletedActionOutcomeOperation recorded = adapter.record(spec, context(spec));

        assertThat(recorded.actionOutcomeRef()).isEqualTo(actionOutcomeRef(1));
        assertThat(recorded.outcome())
                .isEqualTo(ActionOutcomeOperationOutcome.CREATED);
        assertThat(recorded.actionOutcomeRecord().actionRef()).isEqualTo(ACTION);
        assertThat(recorded.actionOutcomeRecord().source().name()).isEqualTo("MANUAL");
        assertThat(adapter.record(spec, context(spec))).isEqualTo(recorded);

        RecordActionOutcomeSpec changed =
                recordSpec(operationId(1), "changed");
        assertThatThrownBy(() -> adapter.record(spec, context(changed)))
                .isInstanceOf(ActionOutcomeConflictException.class)
                .satisfies(error -> assertThat(
                        ((ActionOutcomeException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_OUTCOME_IDEMPOTENCY_CONFLICT));

        JdbcActionOutcomeQueryAdapter query =
                new JdbcActionOutcomeQueryAdapter(jdbc);
        assertThat(query.findByRef(recorded.actionOutcomeRef()).orElseThrow())
                .isEqualTo(recorded.actionOutcomeRecord());
        assertThat(query.findOperation(spec.operationId()).orElseThrow())
                .isEqualTo(recorded);

        new JdbcActionOutcomeAccessLogAdapter(
                jdbc, new DataSourceTransactionManager(dataSource))
                .recordFullDetailAccess(
                        recorded.actionOutcomeRef(), new ActorRef(ACTOR), NOW);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_access_log", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_record", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_operation", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void sameActionRefCanBeRecordedTwiceWithDifferentOutcomeFacts() {
        RecordActionOutcomeSpec first = recordSpec(operationId(1), "first");
        RecordActionOutcomeSpec second = recordSpec(operationId(2), "second");

        CompletedActionOutcomeOperation firstRecorded =
                adapter(actionOutcomeRef(1)).record(first, context(first));
        CompletedActionOutcomeOperation secondRecorded =
                adapter(actionOutcomeRef(2)).record(second, context(second));

        assertThat(firstRecorded.actionOutcomeRecord().actionRef()).isEqualTo(ACTION);
        assertThat(secondRecorded.actionOutcomeRecord().actionRef()).isEqualTo(ACTION);
        assertThat(firstRecorded.actionOutcomeRef())
                .isNotEqualTo(secondRecorded.actionOutcomeRef());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_record WHERE action_ref = ?",
                Integer.class, ACTION.value())).isEqualTo(2);
    }

    @Test
    void generatedRefCollisionRetriesExactlyThreeTimesAndNeverOverwrites() {
        RecordActionOutcomeSpec first = recordSpec(operationId(1), "first");
        adapter(actionOutcomeRef(1)).record(first, context(first));
        AtomicInteger generated = new AtomicInteger();
        JdbcActionOutcomeMutationAdapter colliding =
                new JdbcActionOutcomeMutationAdapter(
                        jdbc, new DataSourceTransactionManager(dataSource), () -> {
                            generated.incrementAndGet();
                            return actionOutcomeRef(1);
                        });
        RecordActionOutcomeSpec second = recordSpec(operationId(2), "second");

        assertThatThrownBy(() -> colliding.record(second, context(second)))
                .isInstanceOf(ActionOutcomeAuthorityUnavailableException.class);
        assertThat(generated).hasValue(3);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_record", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_operation", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void operationFailureRollsBackRecordAndLedgerAtomically() {
        jdbc.execute("""
                CREATE TRIGGER q014_force_operation_failure
                BEFORE INSERT ON action_outcome_operation
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced action outcome operation failure'
                """);
        RecordActionOutcomeSpec spec = recordSpec(operationId(1), "outcome");
        try {
            assertThatThrownBy(() ->
                    adapter(actionOutcomeRef(1)).record(spec, context(spec)))
                    .isInstanceOf(ActionOutcomeAuthorityUnavailableException.class);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM action_outcome_record", Integer.class))
                    .isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM action_outcome_operation", Integer.class))
                    .isZero();
        } finally {
            jdbc.execute("DROP TRIGGER q014_force_operation_failure");
        }
    }

    @Test
    void concurrentSameOperationReturnsOneCommitAndOneReplay() throws Exception {
        RecordActionOutcomeSpec spec = recordSpec(operationId(1), "outcome");
        AuthorizedMutationContext context = context(spec);
        JdbcActionOutcomeMutationAdapter firstAdapter =
                adapter(actionOutcomeRef(1));
        JdbcActionOutcomeMutationAdapter secondAdapter =
                adapter(actionOutcomeRef(2));
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
                "SELECT COUNT(*) FROM action_outcome_record", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_operation", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void failedAccessLogIsIsolatedFromConcurrentUnrelatedRecording()
            throws Exception {
        RecordActionOutcomeSpec firstSpec = recordSpec(operationId(1), "first");
        CompletedActionOutcomeOperation first =
                adapter(actionOutcomeRef(1)).record(firstSpec, context(firstSpec));
        jdbc.execute("""
                CREATE TRIGGER q014_force_access_log_failure
                BEFORE INSERT ON action_outcome_access_log
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced action outcome access log failure'
                """);
        JdbcActionOutcomeAccessLogAdapter accessLog =
                new JdbcActionOutcomeAccessLogAdapter(
                        jdbc, new DataSourceTransactionManager(dataSource));
        RecordActionOutcomeSpec secondSpec =
                recordSpec(operationId(2), "unrelated");
        JdbcActionOutcomeMutationAdapter recording =
                adapter(actionOutcomeRef(2));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var audit = executor.submit(() -> {
                start.await();
                try {
                    accessLog.recordFullDetailAccess(
                            first.actionOutcomeRef(), new ActorRef(ACTOR), NOW);
                    return null;
                } catch (ActionOutcomeAuthorityUnavailableException exception) {
                    return exception;
                }
            });
            var unrelated = executor.submit(() -> {
                start.await();
                return recording.record(secondSpec, context(secondSpec));
            });
            start.countDown();
            assertThat(audit.get())
                    .isInstanceOf(ActionOutcomeAuthorityUnavailableException.class);
            assertThat(unrelated.get().outcome())
                    .isEqualTo(ActionOutcomeOperationOutcome.CREATED);
        } finally {
            jdbc.execute("DROP TRIGGER q014_force_access_log_failure");
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_access_log", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_record", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_operation", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void malformedStoredUtf8FailsClosedInsteadOfReturningReplacementContent() {
        RecordActionOutcomeSpec spec = recordSpec(operationId(1), "outcome");
        CompletedActionOutcomeOperation recorded =
                adapter(actionOutcomeRef(1)).record(spec, context(spec));
        jdbc.update(
                "UPDATE action_outcome_record SET outcome_text = ?"
                        + " WHERE action_outcome_ref = ?",
                new byte[] {(byte) 0xc3, 0x28},
                recorded.actionOutcomeRef().value());

        assertThatThrownBy(() -> new JdbcActionOutcomeQueryAdapter(jdbc)
                .findByRef(recorded.actionOutcomeRef()))
                .isInstanceOf(ActionOutcomeAuthorityUnavailableException.class);
    }

    private JdbcActionOutcomeMutationAdapter adapter(ActionOutcomeRef... refs) {
        var sequence = new ArrayList<>(List.of(refs));
        return new JdbcActionOutcomeMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource),
                sequence::removeFirst);
    }

    private RecordActionOutcomeSpec recordSpec(
            String operationId,
            String outcomeText) {
        return new RecordActionOutcomeSpec(
                new ActionOutcomeOperationId(operationId),
                ACTION,
                new OutcomeText(outcomeText));
    }

    private AuthorizedMutationContext context(RecordActionOutcomeSpec spec) {
        ActorContext actor = new ActorContext(
                new ActorRef(ACTOR), ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"),
                null, null);
        AuthorizationDecision decision = AuthorizationDecision.allow(
                actor.actorRef(), ActionOutcomeCapabilities.RECORD, NOW, 1, 1);
        return new AuthorizedMutationContext(
                spec.operationId(),
                FINGERPRINTS.forRecord(
                        spec.actionRef().value(), spec.outcomeText().value()),
                actor, decision, ActionOutcomeCapabilities.RECORD, NOW);
    }

    private ActionOutcomeRef actionOutcomeRef(int value) {
        return new ActionOutcomeRef(
                "aoc-00000000-0000-4000-8000-" + String.format("%012d", value));
    }

    private String operationId(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q014_MYSQL_TEST_URL"));
        source.setUsername(required("Q014_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q014_MYSQL_TEST_PASSWORD"));
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
