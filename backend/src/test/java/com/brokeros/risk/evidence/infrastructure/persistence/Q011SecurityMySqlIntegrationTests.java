package com.brokeros.risk.evidence.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.EvidenceCapabilities;
import com.brokeros.risk.evidence.application.EvidenceException;
import com.brokeros.risk.evidence.application.EvidenceFingerprintFactory;
import com.brokeros.risk.evidence.application.EvidenceMetricOperation;
import com.brokeros.risk.evidence.application.EvidenceRecordingService;
import com.brokeros.risk.evidence.application.RecordEvidenceCommand;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.infrastructure.persistence.JdbcAuthorizationAdapter;
import com.brokeros.risk.tradingaccount.application.AuthorityEvidenceFactory;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.infrastructure.persistence.JdbcTradingAccountAuthorityQueryAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q011_MYSQL_TEST_URL", matches = ".+")
class Q011SecurityMySqlIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-08-29T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String SUBJECT = "ta-00000000-0000-4000-8000-000000000002";

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
        provisionRecognizedSubject();
    }

    @Test
    void realQ009AndQ010GrantsPermitHumanRecordingWithSameActorContext() {
        provisionActor(ActorType.HUMAN, true, true);
        EvidenceRecordingService service = service();

        var result = service.record(
                actor(ActorType.HUMAN),
                new RecordEvidenceCommand(
                        operationId(1), SUBJECT, "controlled observation"));

        assertThat(result.evidenceRef()).isEqualTo(evidenceRef(1));
        assertThat(jdbc.queryForObject(
                "SELECT subject_ref FROM evidence_record", String.class)).isEqualTo(SUBJECT);
        assertThat(jdbc.queryForObject(
                "SELECT recorded_by_actor_ref FROM evidence_record", String.class)).isEqualTo(ACTOR);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isEqualTo(1);
    }

    @Test
    void realQ009EvidenceDenialPreventsAllQ011DataAccess() {
        provisionActor(ActorType.HUMAN, false, true);
        EvidenceRecordingService service = service();

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                new RecordEvidenceCommand(
                        operationId(1), SUBJECT, "controlled observation")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertEvidenceTablesEmpty();
    }

    @Test
    void realQ010ReadDenialPreventsEvidenceCreation() {
        provisionActor(ActorType.HUMAN, true, false);
        EvidenceRecordingService service = service();

        assertThatThrownBy(() -> service.record(
                actor(ActorType.HUMAN),
                new RecordEvidenceCommand(
                        operationId(1), SUBJECT, "controlled observation")))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertEvidenceTablesEmpty();
    }

    @Test
    void serviceActorRemainsRejectedDespiteBothRealCapabilityGrants() {
        provisionActor(ActorType.SERVICE, true, true);
        EvidenceRecordingService service = service();

        assertThatThrownBy(() -> service.record(
                actor(ActorType.SERVICE),
                new RecordEvidenceCommand(
                        operationId(1), SUBJECT, "controlled observation")))
                .isInstanceOf(EvidenceException.class)
                .satisfies(error -> assertThat(((EvidenceException) error).getResultCode())
                        .isEqualTo(ResultCode.EVIDENCE_ACTOR_TYPE_NOT_PERMITTED));
        assertEvidenceTablesEmpty();
    }

    private EvidenceRecordingService service() {
        AuthorizationGuard guard = new AuthorizationGuard(
                new JdbcAuthorizationAdapter(jdbc, CLOCK));
        TradingAccountReferenceEligibilityService eligibility =
                new TradingAccountReferenceEligibilityService(
                        guard,
                        new JdbcTradingAccountAuthorityQueryAdapter(jdbc),
                        new AuthorityEvidenceFactory());
        JdbcEvidenceQueryAdapter query = new JdbcEvidenceQueryAdapter(jdbc);
        JdbcEvidenceMutationAdapter mutation = new JdbcEvidenceMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource), () -> evidenceRef(1));
        return new EvidenceRecordingService(
                guard, query, mutation, new EvidenceFingerprintFactory(),
                eligibility, noOpMetrics(), CLOCK);
    }

    private EvidenceMetricsPort noOpMetrics() {
        return new EvidenceMetricsPort() {
            @Override
            public void recordOperation(
                    EvidenceMetricOperation operation,
                    com.brokeros.risk.evidence.domain.EvidenceOperationOutcome outcome) {
            }

            @Override
            public void recordConflict(ResultCode category) {
            }

            @Override
            public void recordAuthorizationDenied(
                    com.brokeros.risk.security.domain.Capability capability) {
            }

            @Override
            public void recordAccessRead(String outcome) {
            }

            @Override
            public void recordDuration(
                    EvidenceMetricOperation operation,
                    Duration duration) {
            }
        };
    }

    private void provisionActor(
            ActorType actorType,
            boolean evidenceRecord,
            boolean tradingAccountRead) {
        Timestamp now = Timestamp.from(NOW);
        jdbc.update("""
                INSERT INTO security_actor (
                    actor_ref, actor_type, status, version,
                    provisioning_source, provisioning_ref, created_at, updated_at)
                VALUES (?, ?, 'ACTIVE', 0, 'test', 'q011-integration', ?, ?)
                """, ACTOR, actorType.name(), now, now);
        long actorId = jdbc.queryForObject(
                "SELECT id FROM security_actor WHERE actor_ref = ?", Long.class, ACTOR);
        if (evidenceRecord) {
            grant(actorId, EvidenceCapabilities.RECORD.value());
        }
        if (tradingAccountRead) {
            grant(actorId, "trading-account-reference:read");
        }
    }

    private void grant(long actorId, String capability) {
        Timestamp now = Timestamp.from(NOW);
        jdbc.update("""
                INSERT INTO security_actor_capability (
                    actor_id, capability, status, version,
                    provisioning_source, provisioning_ref,
                    granted_at, revoked_at, updated_at)
                VALUES (?, ?, 'GRANTED', 0, 'test', 'q011-integration', ?, NULL, ?)
                """, actorId, capability, now, now);
    }

    private void provisionRecognizedSubject() {
        Timestamp now = Timestamp.from(NOW);
        String scopeRef = "aas-00000000-0000-4000-8000-000000000010";
        jdbc.update("""
                INSERT INTO trading_account_authority_scope (
                    authority_scope_ref, lifecycle_status, version,
                    registration_attestation_source, registration_attestation_ref,
                    registered_by_actor_ref, last_operation_id, created_at, updated_at)
                VALUES (?, 'ACTIVE', 0, 'test', 'q011-scope', ?, ?, ?, ?)
                """, scopeRef, ACTOR, operationId(10), now, now);
        long scopeId = jdbc.queryForObject(
                "SELECT id FROM trading_account_authority_scope WHERE authority_scope_ref = ?",
                Long.class, scopeRef);
        jdbc.update("""
                INSERT INTO trading_account_reference (
                    trading_account_ref, authority_scope_id,
                    source_family, source_instance, source_server, source_environment,
                    external_account_key, lifecycle_status, version,
                    registration_attestation_source, registration_attestation_ref,
                    registered_by_actor_ref, last_operation_id, created_at, updated_at)
                VALUES (?, ?, 'test', 'instance', 'server-1', 'test', ?, 'ACTIVE', 0,
                        'test', 'q011-account', ?, ?, ?, ?)
                """, SUBJECT, scopeId, "account-1".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                ACTOR, operationId(11), now, now);
    }

    private ActorContext actor(ActorType actorType) {
        return new ActorContext(
                new ActorRef(ACTOR), actorType,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test", "operator", actorType),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"),
                "request-1", "0123456789abcdef0123456789abcdef");
    }

    private void assertEvidenceTablesEmpty() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isZero();
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
