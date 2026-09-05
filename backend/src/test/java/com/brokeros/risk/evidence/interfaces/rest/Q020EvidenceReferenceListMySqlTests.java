package com.brokeros.risk.evidence.interfaces.rest;

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

import com.brokeros.risk.evidence.application.EvidenceCapabilities;
import com.brokeros.risk.evidence.application.EvidenceCorrectionService;
import com.brokeros.risk.evidence.application.EvidenceDetailReadService;
import com.brokeros.risk.evidence.application.EvidenceRecordingService;
import com.brokeros.risk.evidence.application.EvidenceReferenceListService;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.infrastructure.persistence.JdbcEvidenceQueryAdapter;
import com.brokeros.risk.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "Q011_MYSQL_TEST_URL", matches = ".+")
class Q020EvidenceReferenceListMySqlTests {

    private static final String SUBJECT = reference("ta-", 10);
    private static final String UNKNOWN_SUBJECT = reference("ta-", 11);

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateAndSeed() {
        DataSource dataSource = migratedDataSource();
        jdbc = new JdbcTemplate(dataSource);
        for (int value = 1; value <= 202; value++) {
            jdbc.update("""
                    INSERT INTO evidence_record (
                        evidence_ref, subject_ref, source, observation_text, status,
                        supersedes_id, superseded_by_id, recorded_by_actor_ref, recorded_at)
                    VALUES (?, ?, 'MANUAL', ?, 'ACTIVE', NULL, NULL, ?, ?)
                    """, reference("ev-", value), SUBJECT,
                    ("private evidence " + value).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    ACTOR_REF, Timestamp.from(NOW.plusSeconds((value + 1L) / 2L)));
        }
    }

    @Test
    void scopedEndpointIsAuthorizedBoundedContentFreeAndEmptyForUnknownKey()
            throws Exception {
        EvidenceMetricsPort metrics = mock(EvidenceMetricsPort.class);
        MockMvc allowed = mockMvc(true, metrics);

        allowed.perform(get("/api/evidence").param("subjectRef", SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items.length()").value(200))
                .andExpect(jsonPath("$.data.items[0].evidenceRef")
                        .value(reference("ev-", 202)))
                .andExpect(jsonPath("$.data.items[0].subjectRef").value(SUBJECT))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].recordedAt").isString())
                .andExpect(jsonPath("$.data.items[199].evidenceRef")
                        .value(reference("ev-", 3)))
                .andExpect(jsonPath("$.data.items[0].observationText").doesNotExist());
        allowed.perform(get("/api/evidence").param("subjectRef", UNKNOWN_SUBJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
        allowed.perform(get("/api/evidence").param("subjectRef", "not-a-subject"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVIDENCE_REQUEST_INVALID"));

        EvidenceMetricsPort deniedMetrics = mock(EvidenceMetricsPort.class);
        mockMvc(false, deniedMetrics)
                .perform(get("/api/evidence").param("subjectRef", UNKNOWN_SUBJECT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_DENIED"));
        verify(deniedMetrics).recordAuthorizationDenied(EvidenceCapabilities.READ);

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_access_log", Integer.class)).isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'evidence_record'
                  AND index_name = 'idx_evidence_record_subject'
                """, Integer.class)).isPositive();
    }

    private MockMvc mockMvc(boolean allowed, EvidenceMetricsPort metrics) {
        EvidenceReferenceListService service = new EvidenceReferenceListService(
                authorizationGuard(allowed), new JdbcEvidenceQueryAdapter(jdbc), metrics);
        EvidenceController controller = new EvidenceController(
                () -> actorContext(),
                mock(EvidenceRecordingService.class),
                mock(EvidenceCorrectionService.class),
                mock(EvidenceDetailReadService.class),
                service);
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jsonConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
