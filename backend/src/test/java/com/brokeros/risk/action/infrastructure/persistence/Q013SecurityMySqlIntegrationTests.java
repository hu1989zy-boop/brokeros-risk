package com.brokeros.risk.action.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.action.application.ActionCapabilities;
import com.brokeros.risk.action.application.ActionDetailReadService;
import com.brokeros.risk.action.application.ActionException;
import com.brokeros.risk.action.application.ActionFingerprintFactory;
import com.brokeros.risk.action.application.ActionMetricOperation;
import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.action.application.ActionRecordingService;
import com.brokeros.risk.action.application.AuthorizedMutationFactory;
import com.brokeros.risk.action.application.RecordActionCommand;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionProvenanceOutcome;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.DecisionMetricOperation;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.domain.DecisionOperationOutcome;
import com.brokeros.risk.decision.infrastructure.persistence.JdbcDecisionQueryAdapter;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.security.infrastructure.persistence.JdbcAuthorizationAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q013_MYSQL_TEST_URL", matches = ".+")
class Q013SecurityMySqlIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-09-01T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String HUMAN_ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String SERVICE_ACTOR = "00000000-0000-4000-8000-000000000002";
    private static final String DECISION =
            "dec-00000000-0000-4000-8000-000000000003";
    private static final String ACTION =
            "act-00000000-0000-4000-8000-000000000004";

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
        provisionRecognizedDecision();
    }

    @Test
    void realQ009AndQ012GrantsPermitHumanRecordingWithSameActorContext() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                ActionCapabilities.RECORD.value(), "decision:read");

        CompletedActionOperation result = recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN),
                command(operationId(1)));

        assertThat(result.actionRef()).isEqualTo(new ActionRef(ACTION));
        assertThat(jdbc.queryForObject(
                "SELECT decision_ref FROM action_record", String.class))
                .isEqualTo(DECISION);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM action_record", String.class))
                .isEqualTo("PROPOSED");
        assertThat(jdbc.queryForObject(
                "SELECT recorded_by_actor_ref FROM action_record", String.class))
                .isEqualTo(HUMAN_ACTOR);
    }

    @Test
    void realActionRecordDenialPreventsAllActionDataAccess() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN, "decision:read");

        assertThatThrownBy(() -> recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1))))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertActionTablesEmpty();
    }

    @Test
    void realDecisionReadDenialPreventsActionCreation() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                ActionCapabilities.RECORD.value());

        assertThatThrownBy(() -> recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1))))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertActionTablesEmpty();
    }

    @Test
    void serviceActorRemainsRejectedDespiteAllRealRecordingGrants() {
        provisionActor(SERVICE_ACTOR, ActorType.SERVICE,
                ActionCapabilities.RECORD.value(), "decision:read");

        assertThatThrownBy(() -> recordingService().record(
                actor(SERVICE_ACTOR, ActorType.SERVICE), command(operationId(1))))
                .isInstanceOf(ActionException.class)
                .satisfies(error -> assertThat(((ActionException) error).getResultCode())
                        .isEqualTo(ResultCode.ACTION_ACTOR_TYPE_NOT_PERMITTED));
        assertActionTablesEmpty();
    }

    @Test
    void authorizedServiceActorCanUseNarrowAndAuditedFullDetailReads() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                ActionCapabilities.RECORD.value(), "decision:read");
        provisionActor(SERVICE_ACTOR, ActorType.SERVICE,
                ActionCapabilities.READ.value());
        CompletedActionOperation recorded = recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1)));
        AuthorizationGuard guard = guard();
        JdbcActionQueryAdapter query = new JdbcActionQueryAdapter(jdbc);
        ActorContext serviceActor = actor(SERVICE_ACTOR, ActorType.SERVICE);

        ActionProvenanceQueryService provenance = new ActionProvenanceQueryService(
                guard, query, noOpActionMetrics());
        assertThat(provenance.confirmProvenance(
                serviceActor, recorded.actionRef()).outcome())
                .isEqualTo(ActionProvenanceOutcome.RECOGNIZED);

        ActionDetailReadService detail = new ActionDetailReadService(
                guard, query,
                new JdbcActionAccessLogAdapter(
                        jdbc, new DataSourceTransactionManager(dataSource)),
                noOpActionMetrics(), CLOCK);
        assertThat(detail.readDetail(serviceActor, recorded.actionRef().value())
                .intentText().value()).isEqualTo("controlled intent");
        assertThat(jdbc.queryForObject(
                "SELECT accessing_actor_ref FROM action_access_log", String.class))
                .isEqualTo(SERVICE_ACTOR);
    }

    private ActionRecordingService recordingService() {
        AuthorizationGuard guard = guard();
        DecisionProvenanceQueryService decision = new DecisionProvenanceQueryService(
                guard, new JdbcDecisionQueryAdapter(jdbc), noOpDecisionMetrics());
        JdbcActionQueryAdapter query = new JdbcActionQueryAdapter(jdbc);
        JdbcActionMutationAdapter mutation = new JdbcActionMutationAdapter(
                jdbc, new DataSourceTransactionManager(dataSource),
                () -> new ActionRef(ACTION));
        return new ActionRecordingService(
                guard, query, mutation, new ActionFingerprintFactory(),
                decision, new AuthorizedMutationFactory(CLOCK),
                noOpActionMetrics());
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
                VALUES (?, ?, 'ACTIVE', 0, 'test', 'q013-integration', ?, ?)
                """, actorRef, actorType.name(), now, now);
        long actorId = jdbc.queryForObject(
                "SELECT id FROM security_actor WHERE actor_ref = ?", Long.class, actorRef);
        for (String capability : capabilities) {
            jdbc.update("""
                    INSERT INTO security_actor_capability (
                        actor_id, capability, status, version,
                        provisioning_source, provisioning_ref,
                        granted_at, revoked_at, updated_at)
                    VALUES (?, ?, 'GRANTED', 0, 'test', 'q013-integration', ?, NULL, ?)
                    """, actorId, capability, now, now);
        }
    }

    private void provisionRecognizedDecision() {
        Timestamp now = Timestamp.from(NOW);
        String subject = "ta-00000000-0000-4000-8000-000000000010";
        jdbc.update("""
                INSERT INTO decision_record (
                    decision_ref, subject_ref, source, conclusion_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', ?, ?, ?)
                """, DECISION, subject,
                "conclusion".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                HUMAN_ACTOR, now);
        long decisionId = jdbc.queryForObject(
                "SELECT id FROM decision_record WHERE decision_ref = ?",
                Long.class, DECISION);
        jdbc.update("""
                INSERT INTO decision_evidence_reference (
                    decision_id, evidence_ref, created_at)
                VALUES (?, ?, ?)
                """, decisionId,
                "ev-00000000-0000-4000-8000-000000000011", now);
    }

    private RecordActionCommand command(String operationId) {
        return new RecordActionCommand(
                operationId, DECISION, "controlled intent");
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

    private void assertActionTablesEmpty() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_record", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_operation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_access_log", Integer.class)).isZero();
    }

    private ActionMetricsPort noOpActionMetrics() {
        return new ActionMetricsPort() {
            @Override
            public void recordOperation(
                    ActionMetricOperation operation,
                    ActionOperationOutcome outcome) {
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
                    ActionMetricOperation operation,
                    Duration duration) {
            }
        };
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
