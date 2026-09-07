package com.brokeros.risk.tradingdata.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventPublisher;
import com.brokeros.risk.tradingdata.infrastructure.configuration.TradingDataKafkaConfiguration;
import com.brokeros.risk.tradingdata.infrastructure.configuration.TradingDataModuleConfiguration;
import com.brokeros.risk.tradingdata.infrastructure.observability.MicrometerTradingDataMetrics;
import com.brokeros.risk.tradingdata.infrastructure.persistence.JdbcTradingDataEventStore;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

@EnabledIfEnvironmentVariable(named = "Q015_MYSQL_TEST_URL", matches = ".+")
@ActiveProfiles("q015-canonical-integration")
@SpringBootTest(classes = Q015CanonicalKafkaMySqlTests.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.kafka.listener.auto-startup=false",
                "spring.kafka.consumer.group-id=q015-canonical-store-test",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.flyway.enabled=false",
                "logging.level.org.apache.kafka=WARN",
                "logging.level.kafka=WARN", "logging.level.org.springframework.kafka=WARN"})
@EmbeddedKafka(partitions = 3, topics = {KafkaTradingDataEventPublisher.TOPIC,
        CanonicalTradingDataDeadLetterPublisher.TOPIC},
        brokerProperties = "auto.create.topics.enable=false",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class Q015CanonicalKafkaMySqlTests {
    private static final String INPUT = KafkaTradingDataEventPublisher.TOPIC;
    private static final String DLT = CanonicalTradingDataDeadLetterPublisher.TOPIC;

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EmbeddedKafkaBroker broker;
    @Autowired private KafkaListenerEndpointRegistry registry;
    @Autowired private FailureInjectingTemplate producer;
    @Autowired private MeterRegistry metrics;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> required("Q015_MYSQL_TEST_URL"));
        registry.add("spring.datasource.username", () -> required("Q015_MYSQL_TEST_USERNAME"));
        registry.add("spring.datasource.password", () -> required("Q015_MYSQL_TEST_PASSWORD"));
    }

    @BeforeEach
    void migrateAndStartListener() {
        Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        startListener();
    }

    @Test
    void recordedCanonicalEventsReachStoreExactlyAndDuplicatesGapsAndAccountOrderAreVisible() throws Exception {
        List<String> golden = CanonicalFixtureSupport.golden();
        for (int i = 0; i < golden.size(); i++) {
            send(golden.get(i), i + 1);
            int count = i + 1;
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(count()).isEqualTo(count));
            assertThat(jdbc.queryForObject("SELECT payload FROM trading_data_event WHERE source_sequence=?",
                    byte[].class, i + 1)).isEqualTo(golden.get(i).getBytes(StandardCharsets.UTF_8));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_data_ingestion_gap", Integer.class)).isZero();
        for (int i = 0; i < golden.size(); i++) {
            send(golden.get(i), i + 1);
        }
        String payload = golden.getFirst();
        // A contiguous burst with one account key must retain partition order in insertion IDs.
        for (long sequence : List.of(20L, 21L, 22L)) {
            send(payload, sequence);
        }
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(count()).isEqualTo(15));
        assertThat(jdbc.queryForList("SELECT source_sequence FROM trading_data_event "
                + "WHERE source_sequence >= 20 ORDER BY id", Long.class)).containsExactly(20L, 21L, 22L);
        assertThat(jdbc.queryForList("SELECT CONCAT(from_sequence,'-',to_sequence) "
                + "FROM trading_data_ingestion_gap", String.class)).containsExactly("13-19");
        // No publication back to INPUT; the test publisher port throws if called.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
                metrics.counter("brokeros.risk.tradingdata.operations", "outcome", "DUPLICATE").count())
                .isEqualTo(12));
    }

    @Test
    void malformedRecordsAreQuarantinedWithOriginalBytesAndSafeProvenanceThenConsumptionContinues() throws Exception {
        byte[] malformed = "{\"event\": broken".getBytes(StandardCharsets.UTF_8);
        ProducerRecord<String, byte[]> input = new ProducerRecord<>(INPUT, "900001", malformed);
        input.headers().add("untrusted-header", "must-not-forward".getBytes(StandardCharsets.UTF_8));
        var sent = producer.send(input).get(10, TimeUnit.SECONDS).getRecordMetadata();
        try (Consumer<String, byte[]> consumer = deadLetterConsumer()) {
            ConsumerRecord<String, byte[]> rejected = KafkaTestUtils.getSingleRecord(consumer, DLT, Duration.ofSeconds(15));
            assertThat(rejected.value()).isEqualTo(malformed);
            assertThat(header(rejected, "original-topic")).isEqualTo(INPUT);
            assertThat(header(rejected, "original-partition")).isEqualTo(Integer.toString(sent.partition()));
            assertThat(header(rejected, "original-offset")).isEqualTo(Long.toString(sent.offset()));
            assertThat(header(rejected, "rejection-code")).isEqualTo("TRADING_DATA_REQUEST_INVALID");
            assertThat(rejected.headers().lastHeader("untrusted-header")).isNull();
        }
        awaitCommitted(sent.partition(), sent.offset() + 1);
        assertThat(count()).isZero();
        send(CanonicalFixtureSupport.golden().getFirst(), 1);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(count()).isEqualTo(1));
        assertThat(metrics.counter("brokeros.risk.tradingdata.kafka.rejected").count()).isEqualTo(1);
    }

    @Test
    void databaseFailureRollsBackEventAndGapStopsWithoutCommittingAndReplaysAfterRestart() throws Exception {
        String payload = CanonicalFixtureSupport.golden().getFirst();
        var first = send(payload, 1).getRecordMetadata();
        awaitCommitted(first.partition(), first.offset() + 1);
        jdbc.execute("CREATE TRIGGER q015_fail_gap BEFORE INSERT ON trading_data_ingestion_gap "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'injected gap failure'");
        var failed = send(payload, 4).getRecordMetadata();
        await().atMost(Duration.ofSeconds(15)).until(() -> !listener().isRunning());
        assertThat(count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trading_data_ingestion_gap", Integer.class)).isZero();
        assertThat(committed(failed.partition())).isEqualTo(failed.offset());
        jdbc.execute("DROP TRIGGER q015_fail_gap");
        startListener();
        awaitCommitted(failed.partition(), failed.offset() + 1);
        assertThat(count()).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT CONCAT(from_sequence,'-',to_sequence) "
                + "FROM trading_data_ingestion_gap", String.class)).containsExactly("2-3");
    }

    @Test
    void failedDeadLetterPublishCannotAdvanceOffsetAndRestartReplaysOriginal() throws Exception {
        producer.rejectDeadLetters.set(true);
        byte[] malformed = new byte[] {(byte) 0xff, 0x00, 0x01};
        var failed = producer.send(INPUT, "900001", malformed).get(10, TimeUnit.SECONDS).getRecordMetadata();
        await().atMost(Duration.ofSeconds(15)).until(() -> !listener().isRunning());
        assertThat(committed(failed.partition())).isLessThanOrEqualTo(failed.offset());
        assertThat(count()).isZero();
        assertThat(metrics.counter("brokeros.risk.tradingdata.kafka.rejected").count()).isZero();
        producer.rejectDeadLetters.set(false);
        startListener();
        try (Consumer<String, byte[]> consumer = deadLetterConsumer()) {
            assertThat(KafkaTestUtils.getSingleRecord(consumer, DLT, Duration.ofSeconds(15)).value())
                    .isEqualTo(malformed);
        }
        awaitCommitted(failed.partition(), failed.offset() + 1);
        assertThat(metrics.counter("brokeros.risk.tradingdata.kafka.rejected").count()).isEqualTo(1);
    }

    private SendResult<String, byte[]> send(String payload, long sequence) throws Exception {
        return producer.send(INPUT, CanonicalFixtureSupport.account(payload),
                CanonicalFixtureSupport.envelope(payload, sequence)).get(10, TimeUnit.SECONDS);
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM trading_data_event", Integer.class);
    }

    private MessageListenerContainer listener() {
        return registry.getListenerContainer(CanonicalTradingDataKafkaListener.ID);
    }

    private void startListener() {
        listener().start();
        ContainerTestUtils.waitForAssignment(listener(), 3);
    }

    private Consumer<String, byte[]> deadLetterConsumer() {
        Map<String, Object> properties = new HashMap<>(KafkaTestUtils.consumerProps("dead-letter-test", "false", broker));
        properties.put("auto.offset.reset", "earliest");
        Consumer<String, byte[]> consumer = new DefaultKafkaConsumerFactory<>(properties,
                new StringDeserializer(), new ByteArrayDeserializer()).createConsumer();
        broker.consumeFromAnEmbeddedTopic(consumer, DLT);
        return consumer;
    }

    private long committed(int partition) throws Exception {
        try (Admin admin = Admin.create(Map.of("bootstrap.servers", broker.getBrokersAsString()))) {
            var offsets = admin.listConsumerGroupOffsets("q015-canonical-store-test")
                    .partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
            var offset = offsets.get(new TopicPartition(INPUT, partition));
            return offset == null ? -1 : offset.offset();
        }
    }

    private void awaitCommitted(int partition, long offset) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(committed(partition)).isEqualTo(offset));
    }

    private String header(ConsumerRecord<String, byte[]> record, String key) {
        return new String(record.headers().lastHeader(key).value(), StandardCharsets.UTF_8);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing test setting " + name);
        }
        return value;
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("q015-canonical-integration")
    @EnableAutoConfiguration
    @Import({TradingDataKafkaConfiguration.class, TradingDataModuleConfiguration.class,
            JdbcTradingDataEventStore.class, MicrometerTradingDataMetrics.class,
            CanonicalTradingDataMessageReader.class, CanonicalTradingDataKafkaListener.class,
            CanonicalTradingDataDeadLetterPublisher.class})
    static class TestConfiguration {
        @Bean Clock securityClock() { return Clock.systemUTC(); }

        @Bean AuthorizationGuard guard() {
            return new AuthorizationGuard((actor, capability) -> {
                throw new AssertionError("Kafka ingestion must use its Kafka trust boundary");
            });
        }

        @Bean TradingDataEventPublisher publisher() {
            return envelope -> { throw new AssertionError("Consumed records must never be republished"); };
        }

        @Bean @Primary FailureInjectingTemplate failureInjectingTemplate(
                DefaultKafkaProducerFactory<String, byte[]> factory) {
            return new FailureInjectingTemplate(factory);
        }
    }

    static class FailureInjectingTemplate extends KafkaTemplate<String, byte[]> {
        final AtomicBoolean rejectDeadLetters = new AtomicBoolean();

        FailureInjectingTemplate(DefaultKafkaProducerFactory<String, byte[]> factory) {
            super(factory);
        }

        @Override
        public CompletableFuture<SendResult<String, byte[]>> send(ProducerRecord<String, byte[]> record) {
            if (DLT.equals(record.topic()) && rejectDeadLetters.get()) {
                return CompletableFuture.failedFuture(new IllegalStateException("injected publish failure"));
            }
            return super.send(record);
        }
    }
}
