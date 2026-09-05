package com.brokeros.risk.decision.interfaces.rest;

import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.ACTOR_REF;
import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.NOW;
import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.actorContext;
import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.authorizationGuard;
import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.migratedDataSource;
import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.jsonConverter;
import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.reference;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;

import javax.sql.DataSource;

import com.brokeros.risk.decision.application.DecisionCapabilities;
import com.brokeros.risk.decision.application.DecisionDetailReadService;
import com.brokeros.risk.decision.application.DecisionRecordingService;
import com.brokeros.risk.decision.application.DecisionReferenceListService;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.infrastructure.persistence.JdbcDecisionQueryAdapter;
import com.brokeros.risk.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "Q011_MYSQL_TEST_URL", matches = ".+")
class Q020DecisionReferenceListMySqlTests {

    private static final String SUBJECT = reference("ta-", 20);
    private static final String UNKNOWN_SUBJECT = reference("ta-", 21);

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateAndSeed() {
        DataSource dataSource = migratedDataSource();
        jdbc = new JdbcTemplate(dataSource);
        for (int value = 1; value <= 202; value++) {
            jdbc.update("""
                    INSERT INTO decision_record (
                        decision_ref, subject_ref, source, conclusion_text,
                        recorded_by_actor_ref, recorded_at)
                    VALUES (?, ?, 'MANUAL', ?, ?, ?)
                    """, reference("dec-", value), SUBJECT,
                    ("private decision " + value).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    ACTOR_REF, Timestamp.from(NOW.plusSeconds((value + 1L) / 2L)));
        }
    }

    @Test
    void scopedEndpointIsAuthorizedBoundedContentFreeAndEmptyForUnknownKey()
            throws Exception {
        DecisionMetricsPort metrics = mock(DecisionMetricsPort.class);
        MockMvc allowed = mockMvc(true, metrics);

        allowed.perform(get("/api/decisions").param("subjectRef", SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(200))
                .andExpect(jsonPath("$.data.items[0].decisionRef")
                        .value(reference("dec-", 202)))
                .andExpect(jsonPath("$.data.items[0].subjectRef").value(SUBJECT))
                .andExpect(jsonPath("$.data.items[0].recordedAt").isString())
                .andExpect(jsonPath("$.data.items[199].decisionRef")
                        .value(reference("dec-", 3)))
                .andExpect(jsonPath("$.data.items[0].conclusionText").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].status").doesNotExist());
        allowed.perform(get("/api/decisions").param("subjectRef", UNKNOWN_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
        allowed.perform(get("/api/decisions").param("subjectRef", "not-a-subject"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DECISION_REQUEST_INVALID"));

        DecisionMetricsPort deniedMetrics = mock(DecisionMetricsPort.class);
        mockMvc(false, deniedMetrics)
                .perform(get("/api/decisions").param("subjectRef", UNKNOWN_SUBJECT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_DENIED"));
        verify(deniedMetrics).recordAuthorizationDenied(DecisionCapabilities.READ);

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_access_log", Integer.class)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'decision_record'
                  AND index_name = 'idx_decision_record_subject'
                """, Integer.class)).isPositive();
    }

    private MockMvc mockMvc(boolean allowed, DecisionMetricsPort metrics) {
        DecisionReferenceListService service = new DecisionReferenceListService(
                authorizationGuard(allowed), new JdbcDecisionQueryAdapter(jdbc), metrics);
        DecisionController controller = new DecisionController(
                () -> actorContext(),
                mock(DecisionRecordingService.class),
                mock(DecisionDetailReadService.class),
                service);
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
