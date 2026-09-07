package com.brokeros.risk.tradingdata.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import com.brokeros.risk.tradingdata.application.TradingDataAuthorityUnavailableException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public final class CanonicalTradingDataDeadLetterPublisher {

    public static final String TOPIC = "brokeros.risk.tradingdata.ingestion-rejected";
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public CanonicalTradingDataDeadLetterPublisher(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ConsumerRecord<String, byte[]> input) {
        ProducerRecord<String, byte[]> output = new ProducerRecord<>(TOPIC, input.key(), input.value());
        // Copy no caller headers or exception diagnostics. Provenance comes from the broker.
        header(output, "rejection-version", "1");
        header(output, "rejection-code", "TRADING_DATA_REQUEST_INVALID");
        header(output, "original-topic", input.topic());
        header(output, "original-partition", Integer.toString(input.partition()));
        header(output, "original-offset", Long.toString(input.offset()));
        try {
            kafkaTemplate.send(output).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TradingDataAuthorityUnavailableException(null);
        } catch (Exception exception) {
            // Even producer errors can include the original record; expose no cause.
            throw new TradingDataAuthorityUnavailableException(null);
        }
    }

    private void header(ProducerRecord<String, byte[]> record, String name, String value) {
        record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
    }
}
