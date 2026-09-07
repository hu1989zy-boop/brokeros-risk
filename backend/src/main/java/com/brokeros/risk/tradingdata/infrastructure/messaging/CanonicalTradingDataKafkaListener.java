package com.brokeros.risk.tradingdata.infrastructure.messaging;

import com.brokeros.risk.tradingdata.application.TradingDataAuthorityUnavailableException;
import com.brokeros.risk.tradingdata.application.TradingDataIngestionService;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public final class CanonicalTradingDataKafkaListener {

    public static final String ID = "tradingDataCanonical";
    private static final Logger LOG = LoggerFactory.getLogger(CanonicalTradingDataKafkaListener.class);
    private final CanonicalTradingDataMessageReader reader;
    private final TradingDataIngestionService service;
    private final CanonicalTradingDataDeadLetterPublisher deadLetters;
    private final MeterRegistry metrics;

    public CanonicalTradingDataKafkaListener(CanonicalTradingDataMessageReader reader,
            TradingDataIngestionService service, CanonicalTradingDataDeadLetterPublisher deadLetters,
            MeterRegistry metrics) {
        this.reader = reader;
        this.service = service;
        this.deadLetters = deadLetters;
        this.metrics = metrics;
    }

    @KafkaListener(id = ID, idIsGroup = false, topics = KafkaTradingDataEventPublisher.TOPIC,
            containerFactory = "tradingDataKafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, byte[]> record) {
        try {
            TradingDataEnvelope envelope;
            try {
                envelope = reader.read(record.key(), record.value());
            } catch (InvalidCanonicalMessageException exception) {
                deadLetters.publish(record);
                metrics.counter("brokeros.risk.tradingdata.kafka.rejected").increment();
                LOG.warn("Canonical message quarantined: partition={}, offset={}",
                        record.partition(), record.offset());
                return;
            }
            service.ingestFromKafka(envelope);
            // RECORD acknowledgement occurs only after the local transaction committed.
        } catch (RuntimeException exception) {
            metrics.counter("brokeros.risk.tradingdata.kafka.failed").increment();
            LOG.error("Canonical ingestion stopped; replay required: partition={}, offset={}",
                    record.partition(), record.offset());
            // The stopping error handler must not receive SQL, parser or payload diagnostics.
            throw new TradingDataAuthorityUnavailableException(null);
        }
    }
}
