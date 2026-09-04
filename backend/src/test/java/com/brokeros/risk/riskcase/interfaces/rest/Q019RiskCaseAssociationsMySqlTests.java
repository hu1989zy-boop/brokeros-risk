package com.brokeros.risk.riskcase.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.exception.GlobalExceptionHandler;
import com.brokeros.risk.riskcase.application.RiskCaseAssociationService;
import com.brokeros.risk.riskcase.application.RiskCaseAuditFactory;
import com.brokeros.risk.riskcase.application.RiskCaseCommandService;
import com.brokeros.risk.riskcase.application.RiskCaseCreationService;
import com.brokeros.risk.riskcase.application.RiskCaseMetricOperation;
import com.brokeros.risk.riskcase.application.RiskCaseQueryService;
import com.brokeros.risk.riskcase.application.RiskCaseResolutionService;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.infrastructure.persistence.JdbcRiskCaseRepository;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.port.ActorContextProvider;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "Q008_MYSQL_TEST_URL", matches = ".+")
class Q019RiskCaseAssociationsMySqlTests {

    private static final Instant NOW = Instant.parse("2026-09-03T12:00:00Z");
    private static final String CASE_NUMBER =
            "RC-19000000-0000-4000-8000-000000000001";
    private static final String ACTOR_REF =
            "19000000-0000-4000-8000-000000000002";
    private static final String EVIDENCE_EVENT_ONE =
            "19000000-0000-4000-8000-000000000003";
    private static final String EVIDENCE_EVENT_TWO =
            "19000000-0000-4000-8000-000000000004";
    private static final String EVIDENCE_ONE =
            "ev-19000000-0000-4000-8000-000000000005";
    private static final String EVIDENCE_TWO =
            "ev-19000000-0000-4000-8000-000000000006";
    private static final String DECISION_ONE =
            "dec-19000000-0000-4000-8000-000000000007";
    private static final String DECISION_TWO =
            "dec-19000000-0000-4000-8000-000000000008";
    private static final String ACTION_REF =
            "act-19000000-0000-4000-8000-000000000009";
    private static final String OUTCOME_REF =
            "aoc-19000000-0000-4000-8000-000000000010";

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private JdbcRiskCaseRepository repository;

    @BeforeEach
    void migrateAndSeedProjection() {
        dataSource = dataSource();
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcRiskCaseRepository(jdbc);
        seedProjection();
    }

    @Test
    void endpointReturnsAuthoritativeProjectionFromRealMysql() throws Exception {
        mockMvc(true).perform(get("/api/risk-cases/{caseNumber}/associations", CASE_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.caseNumber").value(CASE_NUMBER))
                .andExpect(jsonPath("$.data.version").value(7))
                .andExpect(jsonPath("$.data.evidenceAssociations.length()").value(2))
                .andExpect(jsonPath("$.data.evidenceAssociations[0].eventRef")
                        .value(EVIDENCE_EVENT_ONE))
                .andExpect(jsonPath("$.data.evidenceAssociations[0].disposition")
                        .value("ATTACHED"))
                .andExpect(jsonPath("$.data.evidenceAssociations[0].source")
                        .value("operator-review"))
                .andExpect(jsonPath("$.data.evidenceAssociations[1].eventRef")
                        .value(EVIDENCE_EVENT_TWO))
                .andExpect(jsonPath("$.data.evidenceAssociations[1].disposition")
                        .value("SUPERSEDED"))
                .andExpect(jsonPath("$.data.evidenceAssociations[1].replacementEvidenceRef")
                        .value(EVIDENCE_TWO))
                .andExpect(jsonPath("$.data.decisions.length()").value(2))
                .andExpect(jsonPath("$.data.decisions[0].decisionRef").value(DECISION_ONE))
                .andExpect(jsonPath("$.data.decisions[0].current").value(false))
                .andExpect(jsonPath("$.data.decisions[1].decisionRef").value(DECISION_TWO))
                .andExpect(jsonPath("$.data.decisions[1].current").value(true))
                .andExpect(jsonPath("$.data.actions.length()").value(1))
                .andExpect(jsonPath("$.data.actions[0].actionRef").value(ACTION_REF))
                .andExpect(jsonPath("$.data.actions[0].outcomeRefs[0]").value(OUTCOME_REF));
    }

    @Test
    void missingReadCapabilityReturns403BeforeCaseExistenceIsConsidered() throws Exception {
        mockMvc(false).perform(get(
                        "/api/risk-cases/{caseNumber}/associations",
                        "RC-19000000-0000-4000-8000-000000000099"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_DENIED"));
    }

    @Test
    void authorizedMissingCaseReturnsStandardNotFoundContract() throws Exception {
        mockMvc(true).perform(get(
                        "/api/risk-cases/{caseNumber}/associations",
                        "RC-19000000-0000-4000-8000-000000000099"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RISK_CASE_NOT_FOUND"));
    }

    @Test
    void projectionRejectsACollectionBeyondTheServerCap() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        for (int value = 3; value <= 501; value++) {
            rows.add(new Object[]{
                    value + 10L,
                    "dec-19100000-0000-4000-8000-" + String.format("%012d", value),
                    ACTOR_REF,
                    "bounded projection fixture",
                    Timestamp.from(NOW)});
        }
        jdbc.batchUpdate("""
                INSERT INTO risk_case_decision_association (
                    case_id, case_version, decision_ref, associated_by_ref,
                    reason, associated_at)
                VALUES ((SELECT id FROM risk_case WHERE case_number = ?), ?, ?, ?, ?, ?)
                """, rows, rows.size(), (statement, row) -> {
                    statement.setString(1, CASE_NUMBER);
                    statement.setLong(2, (Long) row[0]);
                    statement.setString(3, (String) row[1]);
                    statement.setString(4, (String) row[2]);
                    statement.setString(5, (String) row[3]);
                    statement.setTimestamp(6, (Timestamp) row[4]);
                });

        mockMvc(true).perform(get("/api/risk-cases/{caseNumber}/associations", CASE_NUMBER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RISK_CASE_INVARIANT_VIOLATION"));
    }

    private MockMvc mockMvc(boolean allowed) {
        ActorContext actorContext = actorContext();
        AuthorizationGuard guard = new AuthorizationGuard((context, capability) ->
                allowed
                        ? AuthorizationDecision.allow(
                                context.actorRef(), capability, NOW, 1, 1)
                        : AuthorizationDecision.deny(
                                context.actorRef(), capability,
                                AuthorizationReason.CAPABILITY_NOT_GRANTED,
                                NOW, 1L, null));
        RiskCaseQueryService queryService = new RiskCaseQueryService(
                guard, repository, record -> { }, new RiskCaseAuditFactory(),
                NOOP_METRICS, Clock.fixed(NOW, ZoneOffset.UTC),
                new DataSourceTransactionManager(dataSource));
        ActorContextProvider actorContextProvider = () -> actorContext;
        RiskCaseController controller = new RiskCaseController(
                actorContextProvider,
                org.mockito.Mockito.mock(RiskCaseCreationService.class),
                org.mockito.Mockito.mock(RiskCaseCommandService.class),
                org.mockito.Mockito.mock(RiskCaseAssociationService.class),
                org.mockito.Mockito.mock(RiskCaseResolutionService.class),
                queryService);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void seedProjection() {
        jdbc.update("""
                INSERT INTO risk_case (
                    case_number, subject_type, subject_ref, intake_source, intake_summary,
                    status, priority, current_assignee_ref, assigned_by_ref, assigned_at,
                    current_decision_ref, current_cycle_no,
                    creation_idempotency_key_hash, creation_request_hash,
                    created_by_ref, created_at, updated_by_ref, updated_at, version)
                VALUES (?, 'TRADING_ACCOUNT', ?, 'MANUAL', 'q019 projection fixture',
                        'IN_REVIEW', 'HIGH', ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, 7)
                """, CASE_NUMBER, "ta-19000000-0000-4000-8000-000000000011",
                ACTOR_REF, ACTOR_REF, Timestamp.from(NOW), DECISION_TWO,
                new byte[32], hashWithLastByte(1), ACTOR_REF, Timestamp.from(NOW),
                ACTOR_REF, Timestamp.from(NOW));
        long caseId = jdbc.queryForObject(
                "SELECT id FROM risk_case WHERE case_number = ?", Long.class, CASE_NUMBER);
        jdbc.update("""
                INSERT INTO risk_case_evidence_association_history (
                    event_ref, case_id, case_version, event_type, evidence_ref,
                    prior_event_id, replacement_evidence_ref, reason, source,
                    actor_ref, occurred_at)
                VALUES (?, ?, 2, 'ATTACHED', ?, NULL, NULL, ?, ?, ?, ?)
                """, EVIDENCE_EVENT_ONE, caseId, EVIDENCE_ONE,
                "initial evidence", "operator-review", ACTOR_REF, Timestamp.from(NOW));
        long evidenceEventId = jdbc.queryForObject(
                "SELECT id FROM risk_case_evidence_association_history WHERE event_ref = ?",
                Long.class, EVIDENCE_EVENT_ONE);
        jdbc.update("""
                INSERT INTO risk_case_evidence_association_history (
                    event_ref, case_id, case_version, event_type, evidence_ref,
                    prior_event_id, replacement_evidence_ref, reason, source,
                    actor_ref, occurred_at)
                VALUES (?, ?, 3, 'SUPERSEDED', ?, ?, ?, ?, ?, ?, ?)
                """, EVIDENCE_EVENT_TWO, caseId, EVIDENCE_ONE, evidenceEventId, EVIDENCE_TWO,
                "superseded evidence", "operator-review", ACTOR_REF,
                Timestamp.from(NOW.plusSeconds(1)));
        jdbc.update("""
                INSERT INTO risk_case_decision_association (
                    case_id, case_version, decision_ref, associated_by_ref,
                    reason, associated_at)
                VALUES (?, 4, ?, ?, ?, ?), (?, 5, ?, ?, ?, ?)
                """, caseId, DECISION_ONE, ACTOR_REF, "first decision", Timestamp.from(NOW),
                caseId, DECISION_TWO, ACTOR_REF, "current decision", Timestamp.from(NOW));
        jdbc.update("""
                INSERT INTO risk_case_action_association_history (
                    case_id, case_version, event_type, action_ref, decision_ref,
                    outcome_ref, prior_event_id, reason, actor_ref, occurred_at)
                VALUES (?, 6, 'ACTION_ASSOCIATED', ?, ?, NULL, NULL, ?, ?, ?)
                """, caseId, ACTION_REF, DECISION_TWO, "associated action", ACTOR_REF,
                Timestamp.from(NOW));
        long actionEventId = jdbc.queryForObject("""
                SELECT id FROM risk_case_action_association_history
                WHERE case_id = ? AND action_ref = ?
                """, Long.class, caseId, ACTION_REF);
        jdbc.update("""
                INSERT INTO risk_case_action_association_history (
                    case_id, case_version, event_type, action_ref, decision_ref,
                    outcome_ref, prior_event_id, reason, actor_ref, occurred_at)
                VALUES (?, 7, 'OUTCOME_REFERENCED', ?, ?, ?, ?, ?, ?, ?)
                """, caseId, ACTION_REF, DECISION_TWO, OUTCOME_REF, actionEventId,
                "referenced outcome", ACTOR_REF, Timestamp.from(NOW.plusSeconds(2)));
    }

    private byte[] hashWithLastByte(int value) {
        byte[] result = new byte[32];
        result[31] = (byte) value;
        return result;
    }

    private ActorContext actorContext() {
        ActorRef actorRef = new ActorRef(ACTOR_REF);
        return new ActorContext(
                actorRef, ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:q019-test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("19000000-0000-4000-8000-000000000012"),
                "q019-request", "19000000000000000000000000000001");
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
