package com.brokeros.risk.actionoutcome.interfaces.rest;

import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.ACTOR_REF;
import static com.brokeros.risk.reference.Q020ReferenceListMySqlSupport.NOW;
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

import com.brokeros.risk.actionoutcome.application.ActionOutcomeCapabilities;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeDetailReadService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeRecordingService;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeReferenceListService;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.infrastructure.persistence.JdbcActionOutcomeQueryAdapter;
import com.brokeros.risk.exception.GlobalExceptionHandler;
import com.brokeros.risk.reference.Q020ReferenceListMySqlSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "Q011_MYSQL_TEST_URL", matches = ".+")
class Q020ActionOutcomeReferenceListMySqlTests {

    private static final String ACTION = reference("act-", 40);
    private static final String UNKNOWN_ACTION = reference("act-", 41);

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateAndSeed() {
        DataSource dataSource = migratedDataSource();
        jdbc = new JdbcTemplate(dataSource);
        for (int value = 1; value <= 202; value++) {
            jdbc.update("""
                    INSERT INTO action_outcome_record (
                        action_outcome_ref, action_ref, source, outcome_text,
                        recorded_by_actor_ref, recorded_at)
                    VALUES (?, ?, 'MANUAL', ?, ?, ?)
                    """, reference("aoc-", value), ACTION,
                    ("private outcome " + value).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    ACTOR_REF, Timestamp.from(NOW.plusSeconds((value + 1L) / 2L)));
        }
    }

    @Test
    void scopedEndpointIsAuthorizedBoundedContentFreeAndEmptyForUnknownKey()
            throws Exception {
        ActionOutcomeMetricsPort metrics = mock(ActionOutcomeMetricsPort.class);
        MockMvc allowed = mockMvc(true, metrics);

        allowed.perform(get("/api/action-outcomes").param("actionRef", ACTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(200))
                .andExpect(jsonPath("$.data.items[0].actionOutcomeRef")
                        .value(reference("aoc-", 202)))
                .andExpect(jsonPath("$.data.items[0].actionRef").value(ACTION))
                .andExpect(jsonPath("$.data.items[0].recordedAt").isString())
                .andExpect(jsonPath("$.data.items[199].actionOutcomeRef")
                        .value(reference("aoc-", 3)))
                .andExpect(jsonPath("$.data.items[0].outcomeText").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].status").doesNotExist());
        allowed.perform(get("/api/action-outcomes").param("actionRef", UNKNOWN_ACTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
        allowed.perform(get("/api/action-outcomes").param("actionRef", "not-an-action"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTION_OUTCOME_REQUEST_INVALID"));

        ActionOutcomeMetricsPort deniedMetrics = mock(ActionOutcomeMetricsPort.class);
        mockMvc(false, deniedMetrics)
                .perform(get("/api/action-outcomes").param("actionRef", UNKNOWN_ACTION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_DENIED"));
        verify(deniedMetrics).recordAuthorizationDenied(ActionOutcomeCapabilities.READ);

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_access_log", Integer.class)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'action_outcome_record'
                  AND index_name = 'idx_action_outcome_record_action'
                """, Integer.class)).isPositive();
    }

    private MockMvc mockMvc(boolean allowed, ActionOutcomeMetricsPort metrics) {
        ActionOutcomeReferenceListService service = new ActionOutcomeReferenceListService(
                authorizationGuard(allowed), new JdbcActionOutcomeQueryAdapter(jdbc), metrics);
        ActionOutcomeController controller = new ActionOutcomeController(
                Q020ReferenceListMySqlSupport::actorContext,
                mock(ActionOutcomeRecordingService.class),
                mock(ActionOutcomeDetailReadService.class),
                service);
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
