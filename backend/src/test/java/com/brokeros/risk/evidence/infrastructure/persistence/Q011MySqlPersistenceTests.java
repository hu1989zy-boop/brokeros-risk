package com.brokeros.risk.evidence.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.AuthorizedMutationContext;
import com.brokeros.risk.evidence.application.CorrectEvidenceSpec;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceConflictException;
import com.brokeros.risk.evidence.application.EvidenceCorrectionResult;
import com.brokeros.risk.evidence.application.EvidenceException;
import com.brokeros.risk.evidence.application.EvidenceFingerprintFactory;
import com.brokeros.risk.evidence.application.EvidenceRecordingResult;
import com.brokeros.risk.evidence.application.EvidenceCapabilities;
import com.brokeros.risk.evidence.application.RecordEvidenceSpec;
import com.brokeros.risk.evidence.domain.CorrectionReason;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.EvidenceStatus;
import com.brokeros.risk.evidence.domain.ObservationText;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q011_MYSQL_TEST_URL", matches = ".+")
class Q011MySqlPersistenceTests {

    private static final Instant NOW = Instant.parse("2026-08-29T01:00:00Z");
    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String SUBJECT = "ta-00000000-0000-4000-8000-000000000002";
    private static final EvidenceFingerprintFactory FINGERPRINTS =
            new EvidenceFingerprintFactory();

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
    void recordCorrectReplayQueryAndAccessLogAreDurableAndConsistent() {
        JdbcEvidenceMutationAdapter adapter = adapter(evidenceRef(1), evidenceRef(2));
        RecordEvidenceSpec recordSpec = recordSpec(operationId(1));
        EvidenceRecordingResult recorded = adapter.record(recordSpec, context(recordSpec));
        assertThat(recorded.evidenceRef()).isEqualTo(evidenceRef(1));
        assertThat(recorded.outcome()).isEqualTo(EvidenceOperationOutcome.CREATED);
        assertThat(adapter.record(recordSpec, context(recordSpec))).isEqualTo(recorded);

        AuthorizedMutationContext changedContext = context(new RecordEvidenceSpec(
                recordSpec.operationId(), recordSpec.subjectRef(), new ObservationText("changed")));
        assertThatThrownBy(() -> adapter.record(recordSpec, changedContext))
                .isInstanceOf(EvidenceConflictException.class)
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT));

        CorrectEvidenceSpec correctSpec = correctSpec(
                operationId(2), recorded.evidenceRef());
        EvidenceCorrectionResult corrected = adapter.correct(
                correctSpec, context(correctSpec));
        assertThat(corrected.evidenceRef()).isEqualTo(evidenceRef(2));
        assertThat(corrected.outcome()).isEqualTo(EvidenceOperationOutcome.CORRECTED);
        assertThat(adapter.correct(correctSpec, context(correctSpec))).isEqualTo(corrected);

        JdbcEvidenceQueryAdapter query = new JdbcEvidenceQueryAdapter(jdbc);
        var target = query.findByRef(recorded.evidenceRef()).orElseThrow();
        var replacement = query.findByRef(corrected.evidenceRef()).orElseThrow();
        assertThat(target.status()).isEqualTo(EvidenceStatus.SUPERSEDED);
        assertThat(target.supersededByRef()).isEqualTo(corrected.evidenceRef());
        assertThat(replacement.status()).isEqualTo(EvidenceStatus.ACTIVE);
        assertThat(replacement.supersedesRef()).isEqualTo(recorded.evidenceRef());
        assertThat(replacement.subjectRef().value()).isEqualTo(SUBJECT);
        assertThat(query.findOperation(correctSpec.operationId()).orElseThrow()
                .resultEvidenceRef()).isEqualTo(corrected.evidenceRef());

        CorrectEvidenceSpec secondCorrection = correctSpec(
                operationId(3), recorded.evidenceRef());
        assertThatThrownBy(() -> adapter.correct(
                secondCorrection, context(secondCorrection)))
                .isInstanceOf(EvidenceConflictException.class)
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_ALREADY_SUPERSEDED));

        new JdbcEvidenceAccessLogAdapter(
                jdbc, new DataSourceTransactionManager(dataSource))
                .recordFullDetailAccess(recorded.evidenceRef(), new ActorRef(ACTOR), NOW);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_access_log", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM evidence_operation_history history_row
                JOIN evidence_operation operation_row
                    ON operation_row.id = history_row.operation_row_id
                WHERE history_row.operation_type <> operation_row.operation_type
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isEqualTo(2);
    }

    @Test
    void generatedRefCollisionRetriesExactlyThreeTimesAndNeverOverwrites() {
        RecordEvidenceSpec first = recordSpec(operationId(1));
        adapter(evidenceRef(1)).record(first, context(first));
        AtomicInteger generated = new AtomicInteger();
        JdbcEvidenceMutationAdapter colliding = new JdbcEvidenceMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource), () -> {
                    generated.incrementAndGet();
                    return evidenceRef(1);
                });
        RecordEvidenceSpec second = new RecordEvidenceSpec(
                new EvidenceOperationId(operationId(2)),
                new TradingAccountRef(SUBJECT), new ObservationText("second"));

        assertThatThrownBy(() -> colliding.record(second, context(second)))
                .isInstanceOf(EvidenceAuthorityUnavailableException.class);
        assertThat(generated).hasValue(3);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isEqualTo(1);
    }

    @Test
    void historyFailureRollsBackRecordAndOperationAtomically() {
        jdbc.execute("""
                CREATE TRIGGER q011_force_history_failure
                BEFORE INSERT ON evidence_operation_history
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced history failure'
                """);
        RecordEvidenceSpec spec = recordSpec(operationId(1));
        try {
            assertThatThrownBy(() -> adapter(evidenceRef(1)).record(spec, context(spec)))
                    .isInstanceOf(EvidenceAuthorityUnavailableException.class);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM evidence_record", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER q011_force_history_failure");
        }
    }

    @Test
    void concurrentSameRecordOperationReturnsOneCommitAndOneReplay() throws Exception {
        RecordEvidenceSpec spec = recordSpec(operationId(1));
        AuthorizedMutationContext context = context(spec);
        JdbcEvidenceMutationAdapter firstAdapter = adapter(evidenceRef(1));
        JdbcEvidenceMutationAdapter secondAdapter = adapter(evidenceRef(2));
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
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentDifferentCorrectionsElectExactlyOneWinner() throws Exception {
        RecordEvidenceSpec recordSpec = recordSpec(operationId(1));
        EvidenceRecordingResult target = adapter(evidenceRef(1))
                .record(recordSpec, context(recordSpec));
        CorrectEvidenceSpec firstSpec = correctSpec(operationId(2), target.evidenceRef());
        CorrectEvidenceSpec secondSpec = new CorrectEvidenceSpec(
                new EvidenceOperationId(operationId(3)), target.evidenceRef(),
                new CorrectionReason("second reason"),
                new ObservationText("second replacement"));
        JdbcEvidenceMutationAdapter firstAdapter = adapter(evidenceRef(2));
        JdbcEvidenceMutationAdapter secondAdapter = adapter(evidenceRef(3));
        CountDownLatch start = new CountDownLatch(1);
        List<Object> results;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> runCorrection(
                    firstAdapter, firstSpec, context(firstSpec), start));
            var second = executor.submit(() -> runCorrection(
                    secondAdapter, secondSpec, context(secondSpec), start));
            start.countDown();
            results = List.of(first.get(), second.get());
        }
        assertThat(results.stream().filter(EvidenceCorrectionResult.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(EvidenceConflictException.class::isInstance)).hasSize(1);
        EvidenceConflictException conflict = (EvidenceConflictException) results.stream()
                .filter(EvidenceConflictException.class::isInstance).findFirst().orElseThrow();
        assertThat(conflict.getResultCode()).isEqualTo(ResultCode.EVIDENCE_ALREADY_SUPERSEDED);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record WHERE supersedes_id IS NOT NULL",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isEqualTo(2);
    }

    @Test
    void concurrentSameCorrectionOperationReturnsOneCommitAndOneReplay() throws Exception {
        RecordEvidenceSpec recordSpec = recordSpec(operationId(1));
        EvidenceRecordingResult target = adapter(evidenceRef(1))
                .record(recordSpec, context(recordSpec));
        CorrectEvidenceSpec spec = correctSpec(operationId(2), target.evidenceRef());
        AuthorizedMutationContext context = context(spec);
        JdbcEvidenceMutationAdapter firstAdapter = adapter(evidenceRef(2));
        JdbcEvidenceMutationAdapter secondAdapter = adapter(evidenceRef(3));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                start.await();
                return firstAdapter.correct(spec, context);
            });
            var second = executor.submit(() -> {
                start.await();
                return secondAdapter.correct(spec, context);
            });
            start.countDown();
            assertThat(first.get()).isEqualTo(second.get());
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isEqualTo(2);
    }

    @Test
    void failedAccessLogIsIsolatedFromConcurrentUnrelatedRecording() throws Exception {
        RecordEvidenceSpec firstSpec = recordSpec(operationId(1));
        EvidenceRecordingResult first = adapter(evidenceRef(1))
                .record(firstSpec, context(firstSpec));
        jdbc.execute("""
                CREATE TRIGGER q011_force_access_log_failure
                BEFORE INSERT ON evidence_access_log
                FOR EACH ROW SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'forced access log failure'
                """);
        JdbcEvidenceAccessLogAdapter accessLog = new JdbcEvidenceAccessLogAdapter(
                jdbc, new DataSourceTransactionManager(dataSource));
        RecordEvidenceSpec secondSpec = new RecordEvidenceSpec(
                new EvidenceOperationId(operationId(2)),
                new TradingAccountRef(SUBJECT), new ObservationText("unrelated"));
        JdbcEvidenceMutationAdapter recording = adapter(evidenceRef(2));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var audit = executor.submit(() -> {
                start.await();
                try {
                    accessLog.recordFullDetailAccess(
                            first.evidenceRef(), new ActorRef(ACTOR), NOW);
                    return null;
                } catch (EvidenceAuthorityUnavailableException exception) {
                    return exception;
                }
            });
            var unrelated = executor.submit(() -> {
                start.await();
                return recording.record(secondSpec, context(secondSpec));
            });
            start.countDown();
            assertThat(audit.get()).isInstanceOf(EvidenceAuthorityUnavailableException.class);
            assertThat(unrelated.get().outcome()).isEqualTo(EvidenceOperationOutcome.CREATED);
        } finally {
            jdbc.execute("DROP TRIGGER q011_force_access_log_failure");
        }
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_access_log", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isEqualTo(2);
    }

    @Test
    void malformedStoredUtf8FailsClosedInsteadOfReturningReplacementContent() {
        RecordEvidenceSpec spec = recordSpec(operationId(1));
        EvidenceRecordingResult recorded = adapter(evidenceRef(1)).record(spec, context(spec));
        jdbc.update(
                "UPDATE evidence_record SET observation_text = ? WHERE evidence_ref = ?",
                new byte[] {(byte) 0xc3, 0x28}, recorded.evidenceRef().value());

        assertThatThrownBy(() -> new JdbcEvidenceQueryAdapter(jdbc)
                .findByRef(recorded.evidenceRef()))
                .isInstanceOf(EvidenceAuthorityUnavailableException.class);
    }

    private Object runCorrection(
            JdbcEvidenceMutationAdapter adapter,
            CorrectEvidenceSpec spec,
            AuthorizedMutationContext context,
            CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            return adapter.correct(spec, context);
        } catch (EvidenceConflictException exception) {
            return exception;
        }
    }

    private JdbcEvidenceMutationAdapter adapter(EvidenceRef... refs) {
        var sequence = new ArrayList<>(List.of(refs));
        return new JdbcEvidenceMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource), sequence::removeFirst);
    }

    private RecordEvidenceSpec recordSpec(String operationId) {
        return new RecordEvidenceSpec(
                new EvidenceOperationId(operationId),
                new TradingAccountRef(SUBJECT),
                new ObservationText("observation"));
    }

    private CorrectEvidenceSpec correctSpec(String operationId, EvidenceRef targetRef) {
        return new CorrectEvidenceSpec(
                new EvidenceOperationId(operationId), targetRef,
                new CorrectionReason("controlled correction"),
                new ObservationText("replacement observation"));
    }

    private AuthorizedMutationContext context(RecordEvidenceSpec spec) {
        return context(
                spec.operationId(),
                FINGERPRINTS.forRecord(
                        spec.subjectRef().value(), spec.observationText().value()),
                EvidenceCapabilities.RECORD);
    }

    private AuthorizedMutationContext context(CorrectEvidenceSpec spec) {
        return context(
                spec.operationId(),
                FINGERPRINTS.forCorrection(
                        spec.targetEvidenceRef().value(), spec.correctionReason().value(),
                        spec.observationText().value()),
                EvidenceCapabilities.CORRECT);
    }

    private AuthorizedMutationContext context(
            EvidenceOperationId operationId,
            com.brokeros.risk.evidence.domain.EvidenceFingerprint fingerprint,
            Capability capability) {
        ActorContext actor = new ActorContext(
                new ActorRef(ACTOR), ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"), null, null);
        AuthorizationDecision decision = AuthorizationDecision.allow(
                actor.actorRef(), capability, NOW, 1, 1);
        return new AuthorizedMutationContext(
                operationId, fingerprint, actor, decision, capability, NOW);
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
        source.setUrl(required("Q011_MYSQL_TEST_URL"));
        source.setUsername(required("Q011_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q011_MYSQL_TEST_PASSWORD"));
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
