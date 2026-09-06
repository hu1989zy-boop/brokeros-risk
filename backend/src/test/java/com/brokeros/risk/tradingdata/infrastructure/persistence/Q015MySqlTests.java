package com.brokeros.risk.tradingdata.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.brokeros.risk.exception.GlobalExceptionHandler;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingdata.application.IngestTradingDataCommand;
import com.brokeros.risk.tradingdata.application.TradingDataAuthorityUnavailableException;
import com.brokeros.risk.tradingdata.application.TradingDataIngestionService;
import com.brokeros.risk.tradingdata.application.port.TradingDataAppendOutcome;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventPublisher;
import com.brokeros.risk.tradingdata.application.port.TradingDataMetricsPort;
import com.brokeros.risk.tradingdata.domain.PlatformTag;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;
import com.brokeros.risk.tradingdata.interfaces.rest.TradingDataIngestionController;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@EnabledIfEnvironmentVariable(named = "Q015_MYSQL_TEST_URL", matches = ".+")
class Q015MySqlTests {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00.123456Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

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
    }

    @Test
    void migrationUpgradesV8ByDynamicCountAndCreatesOnlyTwoEmptyTables() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        assertThat(Flyway.configure().dataSource(dataSource).target("8").load()
                .migrate().migrationsExecuted).isEqualTo(8);

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        int pendingMigrationCount = flyway.info().pending().length;
        assertThat(pendingMigrationCount).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted)
                .isEqualTo(pendingMigrationCount);
        assertThat(tradingDataTables()).containsExactly(
                "trading_data_event", "trading_data_ingestion_gap");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_data_event", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_data_ingestion_gap", Integer.class)).isZero();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        String create = jdbc.queryForObject(
                "SHOW CREATE TABLE trading_data_event",
                (resultSet, rowNumber) -> resultSet.getString(2));
        assertThat(create)
                .contains("uq_trading_data_event_source_sequence", "`payload` blob")
                .doesNotContain("PARTITION BY");
    }

    @Test
    void storeRoundTripsOpaqueBytesSupportsHistoryAndEnforcesIdempotencyAndGap() {
        JdbcTradingDataEventStore store = new JdbcTradingDataEventStore(jdbc);
        byte[] arbitrary = {(byte) 0xff, 0x00, (byte) 0xc3, 0x28};
        TradingDataEnvelope first = envelope(1, "account-1", arbitrary, NOW.minusSeconds(2));
        TradingDataEnvelope second = envelope(2, "account-1", new byte[] {1}, NOW);

        assertThat(store.append(first, NOW)).isEqualTo(TradingDataAppendOutcome.INSERTED);
        assertThat(store.append(second, NOW)).isEqualTo(TradingDataAppendOutcome.INSERTED);
        assertThat(store.append(first, NOW)).isEqualTo(TradingDataAppendOutcome.DUPLICATE);
        assertThat(store.lastContiguousSequence("server-1").orElseThrow()).isEqualTo(2);
        store.recordGap("server-1", 3, 5, NOW);

        assertThat(jdbc.queryForObject(
                "SELECT payload FROM trading_data_event WHERE source_server_id = ? "
                        + "AND source_sequence = ?",
                byte[].class, "server-1", 1)).containsExactly(arbitrary);
        assertThat(jdbc.queryForList("""
                SELECT source_sequence
                FROM trading_data_event
                WHERE trading_account_id = ? AND occurred_at BETWEEN ? AND ?
                ORDER BY occurred_at
                """, Long.class, "account-1", NOW.minusSeconds(3), NOW.plusSeconds(1)))
                .containsExactly(1L, 2L);
        assertThat(jdbc.queryForObject(
                "SELECT CONCAT(from_sequence, '-', to_sequence) "
                        + "FROM trading_data_ingestion_gap",
                String.class)).isEqualTo("3-5");
        assertThat(explainKey("""
                SELECT * FROM trading_data_event
                FORCE INDEX (ix_trading_data_event_account_occurred_at)
                WHERE trading_account_id = 'account-1'
                  AND occurred_at BETWEEN '2026-09-05' AND '2026-09-07'
                """)).isEqualTo("ix_trading_data_event_account_occurred_at");
    }

    @Test
    void endpointAcceptsThenDeduplicatesAndPublishesOnlyOnce() throws Exception {
        RecordingPublisher publisher = new RecordingPublisher();
        TradingDataIngestionService service = service(publisher);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new TradingDataIngestionController(this::serviceActor, service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String request = """
                {
                  "envelopeVersion": 1,
                  "platform": "MT4",
                  "sourceServerId": "server-1",
                  "sourceSequence": 1,
                  "tradingAccountId": "account-1",
                  "occurredAt": "2026-09-06T00:00:00.123456Z",
                  "payloadBase64": "/wDDKA=="
                }
                """;

        mvc.perform(post("/api/trading-data/ingest")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.gapDetected").value(false));
        mvc.perform(post("/api/trading-data/ingest")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("DUPLICATE"));

        assertThat(publisher.envelopes).hasSize(1);
        assertThat(publisher.envelopes.getFirst().payload())
                .containsExactly((byte) 0xff, 0x00, (byte) 0xc3, 0x28);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_data_event", Integer.class)).isEqualTo(1);
    }

    @Test
    void publisherFailureRollsBackNewEventAndGapMarker() {
        JdbcTradingDataEventStore store = new JdbcTradingDataEventStore(jdbc);
        store.append(envelope(1, "account-1", new byte[] {1}, NOW), NOW);
        TradingDataEventPublisher failing = envelope -> {
            throw new TradingDataAuthorityUnavailableException(
                    new RuntimeException("synthetic publisher failure"));
        };

        assertThatThrownBy(() -> service(failing).ingest(
                serviceActor(), command(3, new byte[] {2})))
                .isInstanceOf(TradingDataAuthorityUnavailableException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_data_event", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM trading_data_ingestion_gap", Integer.class)).isZero();
    }

    @Test
    void databaseChecksRejectInvalidMetadataAndEmptyPayload() {
        assertRejected(() -> jdbc.update("""
                INSERT INTO trading_data_event (
                    envelope_version, platform, source_server_id, source_sequence,
                    trading_account_id, occurred_at, received_at, payload)
                VALUES (0, 'MT4', 'server-1', 1, 'account-1', ?, ?, X'01')
                """, NOW, NOW));
        assertRejected(() -> jdbc.update("""
                INSERT INTO trading_data_event (
                    envelope_version, platform, source_server_id, source_sequence,
                    trading_account_id, occurred_at, received_at, payload)
                VALUES (1, 'MT6', 'server-1', 1, 'account-1', ?, ?, X'01')
                """, NOW, NOW));
        assertRejected(() -> jdbc.update("""
                INSERT INTO trading_data_event (
                    envelope_version, platform, source_server_id, source_sequence,
                    trading_account_id, occurred_at, received_at, payload)
                VALUES (1, 'MT4', 'server-1', 1, 'account-1', ?, ?, X'')
                """, NOW, NOW));
    }

    private TradingDataIngestionService service(TradingDataEventPublisher publisher) {
        AuthorizationGuard guard = new AuthorizationGuard((context, capability) ->
                AuthorizationDecision.allow(
                        context.actorRef(), capability, NOW, 1, 1));
        return new TradingDataIngestionService(
                guard,
                new JdbcTradingDataEventStore(jdbc),
                publisher,
                new NoOpMetrics(),
                CLOCK,
                new JdbcTransactionManager(dataSource));
    }

    private TradingDataEnvelope envelope(
            long sequence, String account, byte[] payload, Instant occurredAt) {
        return new TradingDataEnvelope(
                1, PlatformTag.MT5, "server-1", sequence, account, occurredAt, payload);
    }

    private IngestTradingDataCommand command(long sequence, byte[] payload) {
        return new IngestTradingDataCommand(
                1, "MT5", "server-1", sequence, "account-1", NOW, payload);
    }

    private ActorContext serviceActor() {
        return new ActorContext(
                new ActorRef("00000000-0000-4000-8000-000000000015"),
                ActorType.SERVICE,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:test", "ingestor", ActorType.SERVICE),
                AuthenticationMethod.TRUSTED_IN_PROCESS,
                NOW,
                null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"),
                null,
                null);
    }

    private List<String> tradingDataTables() {
        return jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name LIKE 'trading_data_%'
                ORDER BY table_name
                """, String.class);
    }

    private String explainKey(String sql) {
        return jdbc.queryForObject("EXPLAIN " + sql,
                (resultSet, rowNumber) -> resultSet.getString("key"));
    }

    private void assertRejected(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOf(DataAccessException.class);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q015_MYSQL_TEST_URL"));
        source.setUsername(required("Q015_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q015_MYSQL_TEST_PASSWORD"));
        return source;
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }

    private static final class RecordingPublisher implements TradingDataEventPublisher {
        private final List<TradingDataEnvelope> envelopes = new ArrayList<>();

        @Override
        public void publish(TradingDataEnvelope envelope) {
            envelopes.add(envelope);
        }
    }

    private static final class NoOpMetrics implements TradingDataMetricsPort {
        @Override
        public void recordOperation(TradingDataIngestionOutcome outcome) {
        }

        @Override
        public void recordGapDetected() {
        }

        @Override
        public void recordAuthorizationDenied(Capability capability) {
        }

        @Override
        public void recordBackpressure() {
        }

        @Override
        public void recordDuration(Duration duration) {
        }
    }
}
