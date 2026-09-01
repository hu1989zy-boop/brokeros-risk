package com.brokeros.risk.actionoutcome.infrastructure.persistence;

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
import com.brokeros.risk.action.application.ActionMetricOperation;
import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.domain.ActionOperationOutcome;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.infrastructure.persistence.JdbcActionQueryAdapter;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeCapabilities;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeDetailReadService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeFingerprintFactory;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeMetricOperation;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeProvenanceQueryService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeRecordingService;
import com.brokeros.risk.actionoutcome.application.AuthorizedMutationFactory;
import com.brokeros.risk.actionoutcome.application.RecordActionOutcomeCommand;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeProvenanceOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;
import com.brokeros.risk.api.ResultCode;
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

@EnabledIfEnvironmentVariable(named = "Q014_MYSQL_TEST_URL", matches = ".+")
class Q014SecurityMySqlIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-09-01T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String HUMAN_ACTOR =
            "00000000-0000-4000-8000-000000000001";
    private static final String SERVICE_ACTOR =
            "00000000-0000-4000-8000-000000000002";
    private static final String ACTION =
            "act-00000000-0000-4000-8000-000000000003";
    private static final String ACTION_OUTCOME =
            "aoc-00000000-0000-4000-8000-000000000004";

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
        provisionRecognizedAction();
    }

    @Test
    void realQ009AndQ013GrantsPermitHumanRecordingWithSameActorContext() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                ActionOutcomeCapabilities.RECORD.value(),
                ActionCapabilities.READ.value());

        CompletedActionOutcomeOperation result = recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN),
                command(operationId(1)));

        assertThat(result.actionOutcomeRef())
                .isEqualTo(new ActionOutcomeRef(ACTION_OUTCOME));
        assertThat(jdbc.queryForObject(
                "SELECT action_ref FROM action_outcome_record", String.class))
                .isEqualTo(ACTION);
        assertThat(jdbc.queryForObject(
                "SELECT recorded_by_actor_ref FROM action_outcome_record",
                String.class)).isEqualTo(HUMAN_ACTOR);
    }

    @Test
    void realActionOutcomeRecordDenialPreventsAllActionOutcomeDataAccess() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                ActionCapabilities.READ.value());

        assertThatThrownBy(() -> recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN),
                command(operationId(1))))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertActionOutcomeTablesEmpty();
    }

    @Test
    void realActionReadDenialPreventsActionOutcomeCreation() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                ActionOutcomeCapabilities.RECORD.value());

        assertThatThrownBy(() -> recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN),
                command(operationId(1))))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertActionOutcomeTablesEmpty();
    }

    @Test
    void serviceActorRemainsRejectedDespiteAllRealRecordingGrants() {
        provisionActor(SERVICE_ACTOR, ActorType.SERVICE,
                ActionOutcomeCapabilities.RECORD.value(),
                ActionCapabilities.READ.value());

        assertThatThrownBy(() -> recordingService().record(
                actor(SERVICE_ACTOR, ActorType.SERVICE),
                command(operationId(1))))
                .isInstanceOf(ActionOutcomeException.class)
                .satisfies(error -> assertThat(
                        ((ActionOutcomeException) error).getResultCode())
                        .isEqualTo(
                                ResultCode.ACTION_OUTCOME_ACTOR_TYPE_NOT_PERMITTED));
        assertActionOutcomeTablesEmpty();
    }

    @Test
    void authorizedServiceActorCanUseNarrowAndAuditedFullDetailReads() {
        provisionActor(HUMAN_ACTOR, ActorType.HUMAN,
                ActionOutcomeCapabilities.RECORD.value(),
                ActionCapabilities.READ.value());
        provisionActor(SERVICE_ACTOR, ActorType.SERVICE,
                ActionOutcomeCapabilities.READ.value());
        CompletedActionOutcomeOperation recorded = recordingService().record(
                actor(HUMAN_ACTOR, ActorType.HUMAN), command(operationId(1)));
        AuthorizationGuard guard = guard();
        JdbcActionOutcomeQueryAdapter query =
                new JdbcActionOutcomeQueryAdapter(jdbc);
        ActorContext serviceActor = actor(SERVICE_ACTOR, ActorType.SERVICE);

        ActionOutcomeProvenanceQueryService provenance =
                new ActionOutcomeProvenanceQueryService(
                        guard, query, noOpActionOutcomeMetrics());
        assertThat(provenance.confirmProvenance(
                serviceActor, recorded.actionOutcomeRef()).outcome())
                .isEqualTo(ActionOutcomeProvenanceOutcome.RECOGNIZED);

        ActionOutcomeDetailReadService detail = new ActionOutcomeDetailReadService(
                guard, query,
                new JdbcActionOutcomeAccessLogAdapter(
                        jdbc, new DataSourceTransactionManager(dataSource)),
                noOpActionOutcomeMetrics(), CLOCK);
        assertThat(detail.readDetail(
                serviceActor, recorded.actionOutcomeRef().value())
                .outcomeText().value()).isEqualTo("observed outcome");
        assertThat(jdbc.queryForObject(
                "SELECT accessing_actor_ref FROM action_outcome_access_log",
                String.class)).isEqualTo(SERVICE_ACTOR);
    }

    private ActionOutcomeRecordingService recordingService() {
        AuthorizationGuard guard = guard();
        ActionProvenanceQueryService action = new ActionProvenanceQueryService(
                guard, new JdbcActionQueryAdapter(jdbc), noOpActionMetrics());
        JdbcActionOutcomeQueryAdapter query =
                new JdbcActionOutcomeQueryAdapter(jdbc);
        JdbcActionOutcomeMutationAdapter mutation =
                new JdbcActionOutcomeMutationAdapter(
                        jdbc, new DataSourceTransactionManager(dataSource),
                        () -> new ActionOutcomeRef(ACTION_OUTCOME));
        return new ActionOutcomeRecordingService(
                guard, query, mutation, new ActionOutcomeFingerprintFactory(),
                action, new AuthorizedMutationFactory(CLOCK),
                noOpActionOutcomeMetrics());
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
                VALUES (?, ?, 'ACTIVE', 0, 'test', 'q014-integration', ?, ?)
                """, actorRef, actorType.name(), now, now);
        long actorId = jdbc.queryForObject(
                "SELECT id FROM security_actor WHERE actor_ref = ?",
                Long.class, actorRef);
        for (String capability : capabilities) {
            jdbc.update("""
                    INSERT INTO security_actor_capability (
                        actor_id, capability, status, version,
                        provisioning_source, provisioning_ref,
                        granted_at, revoked_at, updated_at)
                    VALUES (?, ?, 'GRANTED', 0, 'test', 'q014-integration',
                            ?, NULL, ?)
                    """, actorId, capability, now, now);
        }
    }

    private void provisionRecognizedAction() {
        Timestamp now = Timestamp.from(NOW);
        jdbc.update("""
                INSERT INTO action_record (
                    action_ref, decision_ref, source, status, intent_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, 'MANUAL', 'PROPOSED', ?, ?, ?)
                """, ACTION,
                "dec-00000000-0000-4000-8000-000000000010",
                "intent".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                HUMAN_ACTOR, now);
    }

    private RecordActionOutcomeCommand command(String operationId) {
        return new RecordActionOutcomeCommand(
                operationId, ACTION, "observed outcome");
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

    private void assertActionOutcomeTablesEmpty() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_record", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_operation", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_access_log", Integer.class))
                .isZero();
    }

    private ActionOutcomeMetricsPort noOpActionOutcomeMetrics() {
        return new ActionOutcomeMetricsPort() {
            @Override
            public void recordOperation(
                    ActionOutcomeMetricOperation operation,
                    ActionOutcomeOperationOutcome outcome) {
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
                    ActionOutcomeMetricOperation operation,
                    Duration duration) {
            }
        };
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
