package com.brokeros.risk.riskcase.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.riskcase.application.RiskCaseAuditFactory;
import com.brokeros.risk.riskcase.application.RiskCaseMetricOperation;
import com.brokeros.risk.riskcase.application.RiskCasePage;
import com.brokeros.risk.riskcase.application.RiskCaseQueryService;
import com.brokeros.risk.riskcase.application.RiskCaseSummary;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q008_MYSQL_TEST_URL", matches = ".+")
class Q016RiskCaseListMySqlTests {

    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final ActorRef ACTOR =
            new ActorRef("16000000-0000-4000-8000-000000000001");

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private JdbcRiskCaseRepository repository;

    @BeforeEach
    void migrateDisposableDatabase() {
        dataSource = dataSource();
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcRiskCaseRepository(jdbc);
    }

    @Test
    void listFiltersAndSummaryProjectionRunAgainstRealMysql() {
        insertCase(1, "OPEN", "HIGH", subject(1), ACTOR.value(), NOW);
        insertCase(2, "OPEN", "NORMAL", subject(2), null, NOW.minusSeconds(1));
        insertCase(3, "CANCELLED", "HIGH", subject(3), null, NOW.minusSeconds(2));

        RiskCasePage<RiskCaseSummary> page = service().listCases(
                actorContext(), "OPEN", "HIGH", subject(1), ACTOR.value(), 0, 20);

        assertThat(page.items()).singleElement().satisfies(summary -> {
            assertThat(summary.caseNumber().value()).isEqualTo(caseNumber(1));
            assertThat(summary.subjectRef().value()).isEqualTo(subject(1));
            assertThat(summary.status().name()).isEqualTo("OPEN");
            assertThat(summary.priority().name()).isEqualTo("HIGH");
            assertThat(summary.assigneeRef()).isEqualTo(ACTOR);
            assertThat(summary.version()).isEqualTo(1);
        });
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void oversizedRequestIsCappedAndStableOrderSupportsNextPage() {
        for (int value = 1; value <= 102; value++) {
            insertCase(value, "OPEN", "NORMAL", subject(value), null, NOW);
        }

        RiskCasePage<RiskCaseSummary> first = service().listCases(
                actorContext(), null, null, null, null, 0, 1_000);
        RiskCasePage<RiskCaseSummary> second = service().listCases(
                actorContext(), null, null, null, null, 1, 1_000);

        assertThat(first.size()).isEqualTo(100);
        assertThat(first.items()).hasSize(100);
        assertThat(first.items().getFirst().caseNumber().value())
                .isEqualTo(caseNumber(102));
        assertThat(first.items().getLast().caseNumber().value())
                .isEqualTo(caseNumber(3));
        assertThat(first.hasNext()).isTrue();
        assertThat(second.items()).extracting(summary -> summary.caseNumber().value())
                .containsExactly(caseNumber(2), caseNumber(1));
        assertThat(second.hasNext()).isFalse();
    }

    private RiskCaseQueryService service() {
        AuthorizationGuard guard = new AuthorizationGuard((context, capability) ->
                AuthorizationDecision.allow(context.actorRef(), capability, NOW, 1, 1));
        return new RiskCaseQueryService(
                guard, repository, record -> { }, new RiskCaseAuditFactory(),
                NOOP_METRICS, Clock.fixed(NOW, ZoneOffset.UTC),
                new DataSourceTransactionManager(dataSource));
    }

    private void insertCase(
            int value,
            String status,
            String priority,
            String subjectRef,
            String assigneeRef,
            Instant updatedAt) {
        Timestamp assignedAt = assigneeRef == null ? null : Timestamp.from(updatedAt);
        jdbc.update("""
                INSERT INTO risk_case (
                    case_number, subject_type, subject_ref, intake_source, intake_summary,
                    status, priority, current_assignee_ref, assigned_by_ref, assigned_at,
                    current_decision_ref, current_cycle_no,
                    creation_idempotency_key_hash, creation_request_hash,
                    created_by_ref, created_at, updated_by_ref, updated_at, version)
                VALUES (?, 'TRADING_ACCOUNT', ?, 'MANUAL', 'q016 list fixture',
                        ?, ?, ?, ?, ?, NULL, 1, ?, ?, ?, ?, ?, ?, 1)
                """,
                caseNumber(value), subjectRef, status, priority,
                assigneeRef, assigneeRef == null ? null : ACTOR.value(), assignedAt,
                hash(value), hash(value + 10_000), ACTOR.value(),
                Timestamp.from(NOW.minusSeconds(60)), ACTOR.value(),
                Timestamp.from(updatedAt));
    }

    private byte[] hash(int value) {
        byte[] bytes = new byte[32];
        ByteBuffer.wrap(bytes).putInt(28, value);
        return bytes;
    }

    private String caseNumber(int value) {
        return "RC-16000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String subject(int value) {
        return "ta-26000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private ActorContext actorContext() {
        return new ActorContext(
                ACTOR, ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:q016-test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("16000000-0000-4000-8000-000000000099"),
                "q016-request", "16000000000000000000000000000001");
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q008_MYSQL_TEST_URL"));
        source.setUsername(required("Q008_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q008_MYSQL_TEST_PASSWORD"));
        return source;
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static final RiskCaseMetricsPort NOOP_METRICS = new RiskCaseMetricsPort() {
        @Override
        public void recordSuccess(RiskCaseMetricOperation operation) {
        }

        @Override
        public void recordConflict(String category) {
        }

        @Override
        public void recordAuthorizationDenied(Capability capability) {
        }

        @Override
        public void recordDuration(RiskCaseMetricOperation operation, Duration duration) {
        }
    };
}
