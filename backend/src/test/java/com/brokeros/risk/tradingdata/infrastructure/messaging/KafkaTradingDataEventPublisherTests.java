package com.brokeros.risk.tradingdata.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;
import com.brokeros.risk.tradingdata.application.TradingDataBackpressureException;
import com.brokeros.risk.tradingdata.domain.PlatformTag;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.SendResult;

class KafkaTradingDataEventPublisherTests {

    @Test
    void publishesWithAccountKeyAndPreservesOpaquePayloadEncoding() throws Exception {
        RecordingSender sender = new RecordingSender(
                CompletableFuture.completedFuture(null));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaTradingDataEventPublisher publisher =
                new KafkaTradingDataEventPublisher(sender, objectMapper);
        byte[] arbitrary = {(byte) 0xff, 0x00, (byte) 0xc3, 0x28};

        publisher.publish(envelope(arbitrary));

        assertThat(sender.topic).isEqualTo("trading-data.canonical");
        assertThat(sender.key).isEqualTo("account-1");
        JsonNode json = objectMapper.readTree(sender.message);
        assertThat(json.path("envelopeVersion").asInt()).isEqualTo(1);
        assertThat(json.path("platform").asText()).isEqualTo("MT5");
        assertThat(json.path("payloadBase64").asText())
                .isEqualTo(Base64.getEncoder().encodeToString(arbitrary));
    }

    @Test
    void boundedSendTimeoutBecomesThrottleResponse() throws Exception {
        CompletableFuture<SendResult<String, byte[]>> future = new CompletableFuture<>() {
            @Override
            public SendResult<String, byte[]> get(long timeout, TimeUnit unit)
                    throws TimeoutException {
                throw new TimeoutException("full");
            }
        };

        assertThatThrownBy(() -> new KafkaTradingDataEventPublisher(
                new RecordingSender(future),
                new ObjectMapper().findAndRegisterModules())
                .publish(envelope(new byte[] {1})))
                .isInstanceOf(TradingDataBackpressureException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_DATA_BACKPRESSURE));
    }

    private TradingDataEnvelope envelope(byte[] payload) {
        return new TradingDataEnvelope(
                1, PlatformTag.MT5, "server-1", 1, "account-1",
                Instant.parse("2026-09-06T00:00:00Z"), payload);
    }

    private static final class RecordingSender
            implements KafkaTradingDataEventPublisher.KafkaSender {
        private final CompletableFuture<SendResult<String, byte[]>> result;
        private String topic;
        private String key;
        private byte[] message;

        private RecordingSender(
                CompletableFuture<SendResult<String, byte[]>> result) {
            this.result = result;
        }

        @Override
        public CompletableFuture<SendResult<String, byte[]>> send(
                String topic, String key, byte[] message) {
            this.topic = topic;
            this.key = key;
            this.message = message.clone();
            return result;
        }
    }
}
