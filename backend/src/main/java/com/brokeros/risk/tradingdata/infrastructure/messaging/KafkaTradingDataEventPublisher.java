package com.brokeros.risk.tradingdata.infrastructure.messaging;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.brokeros.risk.tradingdata.application.TradingDataAuthorityUnavailableException;
import com.brokeros.risk.tradingdata.application.TradingDataBackpressureException;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventPublisher;
import com.brokeros.risk.tradingdata.domain.PlatformTag;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
public class KafkaTradingDataEventPublisher implements TradingDataEventPublisher {

    public static final String TOPIC = "trading-data.canonical";
    static final long SEND_TIMEOUT_SECONDS = 5;

    private final KafkaSender kafkaSender;
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaTradingDataEventPublisher(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            ObjectMapper objectMapper) {
        this(kafkaTemplate::send, objectMapper);
    }

    KafkaTradingDataEventPublisher(
            KafkaSender kafkaSender,
            ObjectMapper objectMapper) {
        this.kafkaSender = kafkaSender;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(TradingDataEnvelope envelope) {
        byte[] message = serialize(envelope);
        try {
            kafkaSender.send(TOPIC, envelope.tradingAccountId(), message)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new TradingDataBackpressureException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TradingDataAuthorityUnavailableException(exception);
        } catch (ExecutionException | RuntimeException exception) {
            throw new TradingDataAuthorityUnavailableException(exception);
        }
    }

    private byte[] serialize(TradingDataEnvelope envelope) {
        try {
            return objectMapper.writeValueAsBytes(new KafkaEnvelopeMessage(
                    envelope.envelopeVersion(),
                    envelope.platform(),
                    envelope.sourceServerId(),
                    envelope.sourceSequence(),
                    envelope.tradingAccountId(),
                    envelope.occurredAt(),
                    Base64.getEncoder().encodeToString(envelope.payload())));
        } catch (JsonProcessingException exception) {
            throw new TradingDataAuthorityUnavailableException(exception);
        }
    }

    private record KafkaEnvelopeMessage(
            int envelopeVersion,
            PlatformTag platform,
            String sourceServerId,
            long sourceSequence,
            String tradingAccountId,
            Instant occurredAt,
            String payloadBase64) {
    }

    @FunctionalInterface
    interface KafkaSender {
        CompletableFuture<SendResult<String, byte[]>> send(
                String topic, String key, byte[] message);
    }
}
