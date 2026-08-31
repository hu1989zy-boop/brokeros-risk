package com.brokeros.risk.decision.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.AuthorizedMutationFactory;
import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.DecisionCapabilities;
import com.brokeros.risk.decision.application.DecisionDetailReadService;
import com.brokeros.risk.decision.application.DecisionException;
import com.brokeros.risk.decision.application.DecisionFingerprintFactory;
import com.brokeros.risk.decision.application.DecisionMetricOperation;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.decision.application.DecisionRecordingService;
import com.brokeros.risk.decision.application.RecordDecisionCommand;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.domain.DecisionProvenanceOutcome;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.application.EvidenceProvenanceQueryService;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.domain.EvidenceOperationOutcome;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.Capability;
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

@EnabledIfEnvironmentVariable(named = "Q012_MYSQL_TEST_URL", matches = ".+")
class Q012SecurityMySqlIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-08-31T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String HUMAN_ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String SERVICE_ACTOR = "00000000-0000-4000-8000-000000000002";
    private static final String SUBJECT = "ta-00000000-0000-4000-8000-000000000003";
    private static final String EVIDENCE = "ev-00000000-0000-4000-8000-000000000004";
    private static final String DECISION = "dec-00000000-0000-4000-8000-000000000005";

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
        provisionRecognizedSubjectAndEvidence();
    }

    @Test
    void realQ009Q010AndQ011GrantsPermitHumanRecordingWithSameActorContext() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                DecisionCapabilities.RECORD.value(),
                "trading-account-reference:read", "evidence:read");

        CompletedDecisionOperation result = recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN),
                command(operationId(1)));

        assertThat(result.decisionRef()).isEqualTo(new DecisionRef(DECISION));
        assertThat(jdbc.queryForObject(
                "SELECT subject_ref FROM decision_record", String.class)).isEqualTo(SUBJECT);
        assertThat(jdbc.queryForObject(
                "SELECT recorded_by_actor_ref FROM decision_record", String.class))
                .isEqualTo(HUMAN_ACTOR);
        assertThat(jdbc.queryForObject(
                "SELECT evidence_ref FROM decision_evidence_reference", String.class))
                .isEqualTo(EVIDENCE);
    }

    @Test
    void realDecisionRecordDenialPreventsAllDecisionDataAccess() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                "trading-account-reference:read", "evidence:read");

        assertThatThrownBy(() -> recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1))))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertDecisionTablesEmpty();
    }

    @Test
    void realQ010ReadDenialPreventsDecisionCreation() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                DecisionCapabilities.RECORD.value(), "evidence:read");

        assertThatThrownBy(() -> recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1))))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertDecisionTablesEmpty();
    }

    @Test
    void realQ011ReadDenialPreventsDecisionCreation() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                DecisionCapabilities.RECORD.value(), "trading-account-reference:read");

        assertThatThrownBy(() -> recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1))))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertDecisionTablesEmpty();
    }

    @Test
    void serviceActorRemainsRejectedDespiteAllRealRecordingGrants() {
        provisionActor(SERVICE_ACTOR, ActorType.SERVICE,
                DecisionCapabilities.RECORD.value(),
                "trading-account-reference:read", "evidence:read");

        assertThatThrownBy(() -> recordingService().record(
                actor(SERVICE_ACTOR, ActorType.SERVICE), command(operationId(1))))
                .isInstanceOf(DecisionException.class)
                .satisfies(error -> assertThat(((DecisionException) error).getResultCode())
                        .isEqualTo(ResultCode.DECISION_ACTOR_TYPE_NOT_PERMITTED));
        assertDecisionTablesEmpty();
    }

    @Test
    void authorizedServiceActorCanUseNarrowAndAuditedFullDetailReads() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                DecisionCapabilities.RECORD.value(),
                "trading-account-reference:read", "evidence:read");
        provisionActor(SERVICE_ACTOR, ActorType.SERVICE,
                DecisionCapabilities.READ.value());
        CompletedDecisionOperation recorded = recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1)));
        AuthorizationGuard guard = guard();
        JdbcDecisionQueryAdapter query = new JdbcDecisionQueryAdapter(jdbc);
        ActorContext serviceActor = actor(SERVICE_ACTOR, ActorType.SERVICE);

        DecisionProvenanceQueryService provenance = new DecisionProvenanceQueryService(
                guard, query, noOpDecisionMetrics());
        assertThat(provenance.confirmProvenance(
                serviceActor, recorded.decisionRef()).outcome())
                .isEqualTo(DecisionProvenanceOutcome.RECOGNIZED);

        DecisionDetailReadService detail = new DecisionDetailReadService(
                guard, query,
                new JdbcDecisionAccessLogAdapter(
                        jdbc, new DataSourceTransactionManager(dataSource)),
                noOpDecisionMetrics(), CLOCK);
        assertThat(detail.readDetail(serviceActor, recorded.decisionRef().value())
                .conclusionText().value()).isEqualTo("controlled conclusion");
        assertThat(jdbc.queryForObject(
                "SELECT accessing_actor_ref FROM decision_access_log", String.class))
                .isEqualTo(SERVICE_ACTOR);
    }

    private DecisionRecordingService recordingService() {
        AuthorizationGuard guard = guard();
        TradingAccountReferenceEligibilityService eligibility =
                new TradingAccountReferenceEligibilityService(
                        guard,
                        new JdbcTradingAccountAuthorityQueryAdapter(jdbc),
                        new AuthorityEvidenceFactory());
        EvidenceProvenanceQueryService evidence = new EvidenceProvenanceQueryService(
                guard, new com.brokeros.risk.evidence.infrastructure.persistence
                        .JdbcEvidenceQueryAdapter(jdbc), noOpEvidenceMetrics());
        JdbcDecisionQueryAdapter query = new JdbcDecisionQueryAdapter(jdbc);
        JdbcDecisionMutationAdapter mutation = new JdbcDecisionMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource),
                () -> new DecisionRef(DECISION));
        return new DecisionRecordingService(
                guard, query, mutation, new DecisionFingerprintFactory(),
                eligibility, evidence, new AuthorizedMutationFactory(CLOCK),
                noOpDecisionMetrics());
    }

    private AuthorizationGuard guard() {
        return new AuthorizationGuard(new JdbcAuthorizationAdapter(jdbc, CLOCK));
    }

    private void provisionActor(
            String actorRef,
            ActorType actorType,
            String... capabilities) {
        Timestamp now = Timestamp.from(NOW);
        jdbc.update("""
                INSERT INTO security_actor (
                    actor_ref, actor_type, status, version,
                    provisioning_source, provisioning_ref, created_at, updated_at)
                VALUES (?, ?, 'ACTIVE', 0, 'test', 'q012-integration', ?, ?)
                """, actorRef, actorType.name(), now, now);
        long actorId = jdbc.queryForObject(
                "SELECT id FROM security_actor WHERE actor_ref = ?", Long.class, actorRef);
        for (String capability : capabilities) {
            jdbc.update("""
                    INSERT INTO security_actor_capability (
                        actor_id, capability, status, version,
                        provisioning_source, provisioning_ref,
                        granted_at, revoked_at, updated_at)
                    VALUES (?, ?, 'GRANTED', 0, 'test', 'q012-integration', ?, NULL, ?)
                    """, actorId, capability, now, now);
        }
    }

    private void provisionRecognizedSubjectAndEvidence() {
        Timestamp now = Timestamp.from(NOW);
        String scopeRef = "aas-00000000-0000-4000-8000-000000000010";
        jdbc.update("""
                INSERT INTO trading_account_authority_scope (
                    authority_scope_ref, lifecycle_status, version,
                    registration_attestation_source, registration_attestation_ref,
                    registered_by_actor_ref, last_operation_id, created_at, updated_at)
                VALUES (?, 'ACTIVE', 0, 'test', 'q012-scope', ?, ?, ?, ?)
                """, scopeRef, HUMAN_ACTOR, operationId(10), now, now);
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
                        'test', 'q012-account', ?, ?, ?, ?)
                """, SUBJECT, scopeId,
                "account-1".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                HUMAN_ACTOR, operationId(11), now, now);
        jdbc.update("""
                INSERT INTO evidence_record (
                    evidence_ref, subject_ref, source, observation_text, status,
                    supersedes_id, superseded_by_id, recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', ?, 'ACTIVE', NULL, NULL, ?, ?)
                """, EVIDENCE, SUBJECT,
                "observation".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                HUMAN_ACTOR, now);
    }

    private RecordDecisionCommand command(String operationId) {
        return new RecordDecisionCommand(
                operationId, SUBJECT, List.of(EVIDENCE), "controlled conclusion");
    }

    private ActorContext actor(String actorRef, ActorType actorType) {
        return new ActorContext(
                new ActorRef(actorRef), actorType,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test",
                        "operator-" + actorType.name().toLowerCase(), actorType),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.randomUUID(), "request-1",
                "0123456789abcdef0123456789abcdef");
    }

    private void assertDecisionTablesEmpty() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_record", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_evidence_reference", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_operation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_access_log", Integer.class)).isZero();
    }

    private DecisionMetricsPort noOpDecisionMetrics() {
        return new DecisionMetricsPort() {
            @Override
            public void recordOperation(
                    DecisionMetricOperation operation,
                    DecisionOperationOutcome outcome) {
            }

            @Override
            public void recordConflict(ResultCode category) {
            }

            @Override
            public void recordAuthorizationDenied(Capability capability) {
            }

            @Override
            public void recordAccessRead(String outcome) {
            }

            @Override
            public void recordDuration(
                    DecisionMetricOperation operation,
                    Duration duration) {
            }
        };
    }

    private EvidenceMetricsPort noOpEvidenceMetrics() {
        return new EvidenceMetricsPort() {
            @Override
            public void recordOperation(
                    com.brokeros.risk.evidence.application.EvidenceMetricOperation operation,
                    EvidenceOperationOutcome outcome) {
            }

            @Override
            public void recordConflict(ResultCode category) {
            }

            @Override
            public void recordAuthorizationDenied(Capability capability) {
            }

            @Override
            public void recordAccessRead(String outcome) {
            }

            @Override
            public void recordDuration(
                    com.brokeros.risk.evidence.application.EvidenceMetricOperation operation,
                    Duration duration) {
            }
        };
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
