package com.brokeros.risk.decision.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.AuthorizedMutationContext;
import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionCapabilities;
import com.brokeros.risk.decision.application.DecisionConflictException;
import com.brokeros.risk.decision.application.DecisionException;
import com.brokeros.risk.decision.application.DecisionFingerprintFactory;
import com.brokeros.risk.decision.application.RecordDecisionSpec;
import com.brokeros.risk.decision.domain.ConclusionText;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q012_MYSQL_TEST_URL", matches = ".+")
class Q012MySqlPersistenceTests {

    private static final Instant NOW = Instant.parse("2026-08-31T01:00:00Z");
    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String SUBJECT = "ta-00000000-0000-4000-8000-000000000002";
    private static final DecisionFingerprintFactory FINGERPRINTS =
            new DecisionFingerprintFactory();

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
        JdbcDecisionMutationAdapter adapter = adapter(decisionRef(1));
        RecordDecisionSpec spec = recordSpec(operationId(1));
        CompletedDecisionOperation recorded = adapter.record(spec, context(spec));

        assertThat(recorded.decisionRef()).isEqualTo(decisionRef(1));
        assertThat(recorded.outcome()).isEqualTo(DecisionOperationOutcome.CREATED);
        assertThat(recorded.decisionRecord().evidenceRefs())
                .containsExactly(evidenceRef(1), evidenceRef(2));
        assertThat(adapter.record(spec, context(spec))).isEqualTo(recorded);

        RecordDecisionSpec changed = new RecordDecisionSpec(
                spec.operationId(), spec.subjectRef(), spec.evidenceRefs(),
                new ConclusionText("changed"));
        assertThatThrownBy(() -> adapter.record(spec, context(changed)))
                .isInstanceOf(DecisionConflictException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_IDEMPOTENCY_CONFLICT));

        JdbcDecisionQueryAdapter query = new JdbcDecisionQueryAdapter(jdbc);
        assertThat(query.findByRef(recorded.decisionRef()).orElseThrow())
                .isEqualTo(recorded.decisionRecord());
        assertThat(query.findOperation(spec.operationId()).orElseThrow())
                .isEqualTo(recorded);

        new JdbcDecisionAccessLogAdapter(
                jdbc, new DataSourceTransactionManager(dataSource))
                .recordFullDetailAccess(recorded.decisionRef(), new ActorRef(ACTOR), NOW);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_access_log", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_evidence_reference", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_operation", Integer.class)).isEqualTo(1);
    }

    @Test
    void generatedRefCollisionRetriesExactlyThreeTimesAndNeverOverwrites() {
        RecordDecisionSpec first = recordSpec(operationId(1));
        adapter(decisionRef(1)).record(first, context(first));
        AtomicInteger generated = new AtomicInteger();
        JdbcDecisionMutationAdapter colliding = new JdbcDecisionMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource), () -> {
                    generated.incrementAndGet();
                    return decisionRef(1);
                });
        RecordDecisionSpec second = new RecordDecisionSpec(
                new DecisionOperationId(operationId(2)),
                new TradingAccountRef(SUBJECT), Set.of(evidenceRef(1)),
                new ConclusionText("second"));

        assertThatThrownBy(() -> colliding.record(second, context(second)))
                .isInstanceOf(DecisionAuthorityUnavailableException.class);
        assertThat(generated).hasValue(3);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_evidence_reference", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_operation", Integer.class)).isEqualTo(1);
    }

    @Test
    void evidenceReferenceFailureRollsBackDecisionAndOperationAtomically() {
        jdbc.execute("""
                CREATE TRIGGER q012_force_evidence_reference_failure
                BEFORE INSERT ON decision_evidence_reference
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced decision evidence reference failure'
                """);
        RecordDecisionSpec spec = recordSpec(operationId(1));
        try {
            assertThatThrownBy(() -> adapter(decisionRef(1)).record(spec, context(spec)))
                    .isInstanceOf(DecisionAuthorityUnavailableException.class);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM decision_record", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM decision_evidence_reference", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM decision_operation", Integer.class)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER q012_force_evidence_reference_failure");
        }
    }

    @Test
    void concurrentSameOperationReturnsOneCommitAndOneReplay() throws Exception {
        RecordDecisionSpec spec = recordSpec(operationId(1));
        AuthorizedMutationContext context = context(spec);
        JdbcDecisionMutationAdapter firstAdapter = adapter(decisionRef(1));
        JdbcDecisionMutationAdapter secondAdapter = adapter(decisionRef(2));
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
                "SELECT COUNT(*) FROM decision_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_evidence_reference", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_operation", Integer.class)).isEqualTo(1);
    }

    @Test
    void failedAccessLogIsIsolatedFromConcurrentUnrelatedRecording() throws Exception {
        RecordDecisionSpec firstSpec = recordSpec(operationId(1));
        CompletedDecisionOperation first = adapter(decisionRef(1))
                .record(firstSpec, context(firstSpec));
        jdbc.execute("""
                CREATE TRIGGER q012_force_access_log_failure
                BEFORE INSERT ON decision_access_log
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced decision access log failure'
                """);
        JdbcDecisionAccessLogAdapter accessLog = new JdbcDecisionAccessLogAdapter(
                jdbc, new DataSourceTransactionManager(dataSource));
        RecordDecisionSpec secondSpec = new RecordDecisionSpec(
                new DecisionOperationId(operationId(2)),
                new TradingAccountRef(SUBJECT), Set.of(evidenceRef(1)),
                new ConclusionText("unrelated"));
        JdbcDecisionMutationAdapter recording = adapter(decisionRef(2));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var audit = executor.submit(() -> {
                start.await();
                try {
                    accessLog.recordFullDetailAccess(
                            first.decisionRef(), new ActorRef(ACTOR), NOW);
                    return null;
                } catch (DecisionAuthorityUnavailableException exception) {
                    return exception;
                }
            });
            var unrelated = executor.submit(() -> {
                start.await();
                return recording.record(secondSpec, context(secondSpec));
            });
            start.countDown();
            assertThat(audit.get()).isInstanceOf(DecisionAuthorityUnavailableException.class);
            assertThat(unrelated.get().outcome()).isEqualTo(DecisionOperationOutcome.CREATED);
        } finally {
            jdbc.execute("DROP TRIGGER q012_force_access_log_failure");
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_access_log", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_record", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_operation", Integer.class)).isEqualTo(2);
    }

    @Test
    void malformedStoredUtf8FailsClosedInsteadOfReturningReplacementContent() {
        RecordDecisionSpec spec = recordSpec(operationId(1));
        CompletedDecisionOperation recorded = adapter(decisionRef(1))
                .record(spec, context(spec));
        jdbc.update(
                "UPDATE decision_record SET conclusion_text = ? WHERE decision_ref = ?",
                new byte[] {(byte) 0xc3, 0x28}, recorded.decisionRef().value());

        assertThatThrownBy(() -> new JdbcDecisionQueryAdapter(jdbc)
                .findByRef(recorded.decisionRef()))
                .isInstanceOf(DecisionAuthorityUnavailableException.class);
    }

    private JdbcDecisionMutationAdapter adapter(DecisionRef... refs) {
        var sequence = new ArrayList<>(List.of(refs));
        return new JdbcDecisionMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource), sequence::removeFirst);
    }

    private RecordDecisionSpec recordSpec(String operationId) {
        return new RecordDecisionSpec(
                new DecisionOperationId(operationId),
                new TradingAccountRef(SUBJECT),
                Set.of(evidenceRef(2), evidenceRef(1)),
                new ConclusionText("conclusion"));
    }

    private AuthorizedMutationContext context(RecordDecisionSpec spec) {
        ActorContext actor = new ActorContext(
                new ActorRef(ACTOR), ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"), null, null);
        AuthorizationDecision decision = AuthorizationDecision.allow(
                actor.actorRef(), DecisionCapabilities.RECORD, NOW, 1, 1);
        return new AuthorizedMutationContext(
                spec.operationId(),
                FINGERPRINTS.forRecord(
                        spec.subjectRef().value(),
                        spec.evidenceRefs().stream().map(EvidenceRef::value).toList(),
                        spec.conclusionText().value()),
                actor, decision, DecisionCapabilities.RECORD, NOW);
    }

    private DecisionRef decisionRef(int value) {
        return new DecisionRef(
                "dec-00000000-0000-4000-8000-" + String.format("%012d", value));
    }

    private EvidenceRef evidenceRef(int value) {
        return new EvidenceRef(
                "ev-00000000-0000-4000-8000-" + String.format("%012d", value));
    }

    private String operationId(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q012_MYSQL_TEST_URL"));
        source.setUsername(required("Q012_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q012_MYSQL_TEST_PASSWORD"));
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
