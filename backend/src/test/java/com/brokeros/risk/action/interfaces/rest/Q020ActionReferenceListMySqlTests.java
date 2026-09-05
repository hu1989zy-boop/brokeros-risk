package com.brokeros.risk.action.interfaces.rest;

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

import com.brokeros.risk.action.application.ActionCapabilities;
import com.brokeros.risk.action.application.ActionDetailReadService;
import com.brokeros.risk.action.application.ActionRecordingService;
import com.brokeros.risk.action.application.ActionReferenceListService;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.infrastructure.persistence.JdbcActionQueryAdapter;
import com.brokeros.risk.exception.GlobalExceptionHandler;
import com.brokeros.risk.reference.Q020ReferenceListMySqlSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "Q011_MYSQL_TEST_URL", matches = ".+")
class Q020ActionReferenceListMySqlTests {

    private static final String DECISION = reference("dec-", 30);
    private static final String UNKNOWN_DECISION = reference("dec-", 31);

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateAndSeed() {
        DataSource dataSource = migratedDataSource();
        jdbc = new JdbcTemplate(dataSource);
        for (int value = 1; value <= 202; value++) {
            jdbc.update("""
                    INSERT INTO action_record (
                        action_ref, decision_ref, source, status, intent_text,
                        recorded_by_actor_ref, recorded_at)
                    VALUES (?, ?, 'MANUAL', 'PROPOSED', ?, ?, ?)
                    """, reference("act-", value), DECISION,
                    ("private action " + value).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    ACTOR_REF, Timestamp.from(NOW.plusSeconds((value + 1L) / 2L)));
        }
    }

    @Test
    void scopedEndpointIsAuthorizedBoundedContentFreeAndEmptyForUnknownKey()
            throws Exception {
        ActionMetricsPort metrics = mock(ActionMetricsPort.class);
        MockMvc allowed = mockMvc(true, metrics);

        allowed.perform(get("/api/actions").param("decisionRef", DECISION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(200))
                .andExpect(jsonPath("$.data.items[0].actionRef")
                        .value(reference("act-", 202)))
                .andExpect(jsonPath("$.data.items[0].decisionRef").value(DECISION))
                .andExpect(jsonPath("$.data.items[0].status").value("PROPOSED"))
                .andExpect(jsonPath("$.data.items[0].recordedAt").isString())
                .andExpect(jsonPath("$.data.items[199].actionRef")
                        .value(reference("act-", 3)))
                .andExpect(jsonPath("$.data.items[0].intentText").doesNotExist());
        allowed.perform(get("/api/actions").param("decisionRef", UNKNOWN_DECISION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
        allowed.perform(get("/api/actions").param("decisionRef", "not-a-decision"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACTION_REQUEST_INVALID"));

        ActionMetricsPort deniedMetrics = mock(ActionMetricsPort.class);
        mockMvc(false, deniedMetrics)
                .perform(get("/api/actions").param("decisionRef", UNKNOWN_DECISION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_DENIED"));
        verify(deniedMetrics).recordAuthorizationDenied(ActionCapabilities.READ);

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_access_log", Integer.class)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'action_record'
                  AND index_name = 'idx_action_record_decision'
                """, Integer.class)).isPositive();
    }

    private MockMvc mockMvc(boolean allowed, ActionMetricsPort metrics) {
        ActionReferenceListService service = new ActionReferenceListService(
                authorizationGuard(allowed), new JdbcActionQueryAdapter(jdbc), metrics);
        ActionController controller = new ActionController(
                Q020ReferenceListMySqlSupport::actorContext,
                mock(ActionRecordingService.class),
                mock(ActionDetailReadService.class),
                service);
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
