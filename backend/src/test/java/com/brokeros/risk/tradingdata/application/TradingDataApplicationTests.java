package com.brokeros.risk.tradingdata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.security.domain.AuthorizationReason;
import com.brokeros.risk.security.domain.Capability;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingdata.application.port.TradingDataAppendOutcome;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventPublisher;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventStore;
import com.brokeros.risk.tradingdata.application.port.TradingDataMetricsPort;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class TradingDataApplicationTests {

    private static final Instant NOW = Instant.parse("2026-09-06T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void acceptedEnvelopePersistsThenPublishesWithOpaqueBytesUntouched() {
        RecordingStore store = new RecordingStore();
        RecordingPublisher publisher = new RecordingPublisher();
        RecordingMetrics metrics = new RecordingMetrics();
        TradingDataIngestionService service = service(allowAll(), store, publisher, metrics);

        TradingDataIngestionResult result = service.ingest(
                actor(ActorType.SERVICE), command(7, new byte[] {(byte) 0xff, 0x00}));

        assertThat(result.outcome()).isEqualTo(TradingDataIngestionOutcome.ACCEPTED);
        assertThat(result.gapDetected()).isFalse();
        assertThat(store.appended.payload()).containsExactly((byte) 0xff, 0x00);
        assertThat(publisher.published).isSameAs(store.appended);
        assertThat(publisher.published.tradingAccountId()).isEqualTo("account-1");
        assertThat(metrics.outcomes).containsExactly(TradingDataIngestionOutcome.ACCEPTED);
    }

    @Test
    void duplicateIsSuccessfulNoOpAndIsNotRepublished() {
        RecordingStore store = new RecordingStore();
        store.appendOutcome = TradingDataAppendOutcome.DUPLICATE;
        RecordingPublisher publisher = new RecordingPublisher();

        TradingDataIngestionResult result = service(
                allowAll(), store, publisher, new RecordingMetrics())
                .ingest(actor(ActorType.SERVICE), command(7, new byte[] {1}));

        assertThat(result.outcome()).isEqualTo(TradingDataIngestionOutcome.DUPLICATE);
        assertThat(result.gapDetected()).isFalse();
        assertThat(publisher.published).isNull();
        assertThat(store.gaps).isEmpty();
    }

    @Test
    void sequenceJumpCreatesVisibleGapBeforePublishing() {
        RecordingStore store = new RecordingStore();
        store.previous = OptionalLong.of(3);
        RecordingMetrics metrics = new RecordingMetrics();

        TradingDataIngestionResult result = service(
                allowAll(), store, new RecordingPublisher(), metrics)
                .ingest(actor(ActorType.SERVICE), command(7, new byte[] {1}));

        assertThat(result.gapDetected()).isTrue();
        assertThat(store.gaps).containsExactly("server-1:4-6");
        assertThat(metrics.gaps).isEqualTo(1);
    }

    @Test
    void capabilityIsCheckedBeforeServiceActorType() {
        RecordingMetrics metrics = new RecordingMetrics();
        AuthorizationGuard deny = new AuthorizationGuard((context, capability) ->
                AuthorizationDecision.deny(
                        context.actorRef(), capability,
                        AuthorizationReason.CAPABILITY_NOT_GRANTED, NOW, 1L, null));

        assertThatThrownBy(() -> service(
                deny, new RecordingStore(), new RecordingPublisher(), metrics)
                .ingest(actor(ActorType.HUMAN), command(1, new byte[] {1})))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(metrics.denied).containsExactly(TradingDataCapabilities.INGEST);
    }

    @Test
    void nonServiceActorAndMalformedMetadataUseApprovedCodes() {
        TradingDataIngestionService service = service(
                allowAll(), new RecordingStore(), new RecordingPublisher(),
                new RecordingMetrics());

        assertThatThrownBy(() -> service.ingest(
                actor(ActorType.HUMAN), command(1, new byte[] {1})))
                .isInstanceOf(TradingDataException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_DATA_ACTOR_TYPE_NOT_PERMITTED));
        assertThatThrownBy(() -> service.ingest(
                actor(ActorType.SERVICE),
                new IngestTradingDataCommand(
                        1, "UNKNOWN", "server-1", 1, "account-1", NOW,
                        new byte[] {1})))
                .isInstanceOf(TradingDataException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_DATA_REQUEST_INVALID));
    }

    @Test
    void backpressureIsSurfacedAndCounted() {
        RecordingMetrics metrics = new RecordingMetrics();
        RecordingStore store = new RecordingStore();
        store.previous = OptionalLong.of(3);
        TradingDataEventPublisher publisher = envelope -> {
            throw new TradingDataBackpressureException(new RuntimeException("full"));
        };

        assertThatThrownBy(() -> service(
                allowAll(), store, publisher, metrics)
                .ingest(actor(ActorType.SERVICE), command(7, new byte[] {1})))
                .isInstanceOf(TradingDataBackpressureException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getResultCode())
                        .isEqualTo(ResultCode.TRADING_DATA_BACKPRESSURE));
        assertThat(metrics.backpressure).isEqualTo(1);
        assertThat(metrics.gaps).isZero();
    }

    private TradingDataIngestionService service(
            AuthorizationGuard guard,
            TradingDataEventStore store,
            TradingDataEventPublisher publisher,
            TradingDataMetricsPort metrics) {
        return new TradingDataIngestionService(
                guard, store, publisher, metrics, CLOCK, transactionManager());
    }

    private AuthorizationGuard allowAll() {
        return new AuthorizationGuard((context, capability) ->
                AuthorizationDecision.allow(
                        context.actorRef(), capability, NOW, 1, 1));
    }

    private PlatformTransactionManager transactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }

    private ActorContext actor(ActorType type) {
        return new ActorContext(
                new ActorRef("00000000-0000-4000-8000-000000000001"),
                type,
                new ExternalPrincipalKey("urn:brokeros:risk:test", "ingestor", type),
                AuthenticationMethod.TRUSTED_IN_PROCESS,
                NOW,
                null,
                UUID.fromString("00000000-0000-4000-8000-000000000099"),
                null,
                null);
    }

    private IngestTradingDataCommand command(long sequence, byte[] payload) {
        return new IngestTradingDataCommand(
                1, "MT5", "server-1", sequence, "account-1", NOW, payload);
    }

    private static final class RecordingStore implements TradingDataEventStore {
        private OptionalLong previous = OptionalLong.empty();
        private TradingDataAppendOutcome appendOutcome = TradingDataAppendOutcome.INSERTED;
        private TradingDataEnvelope appended;
        private final List<String> gaps = new ArrayList<>();

        @Override
        public OptionalLong lastContiguousSequence(String sourceServerId) {
            return previous;
        }

        @Override
        public TradingDataAppendOutcome append(
                TradingDataEnvelope envelope, Instant receivedAt) {
            appended = envelope;
            return appendOutcome;
        }

        @Override
        public void recordGap(
                String sourceServerId, long fromSequence, long toSequence,
                Instant detectedAt) {
            gaps.add(sourceServerId + ":" + fromSequence + "-" + toSequence);
        }
    }

    private static final class RecordingPublisher implements TradingDataEventPublisher {
        private TradingDataEnvelope published;

        @Override
        public void publish(TradingDataEnvelope envelope) {
            published = envelope;
        }
    }

    private static final class RecordingMetrics implements TradingDataMetricsPort {
        private final List<TradingDataIngestionOutcome> outcomes = new ArrayList<>();
        private final List<Capability> denied = new ArrayList<>();
        private int gaps;
        private int backpressure;

        @Override
        public void recordOperation(TradingDataIngestionOutcome outcome) {
            outcomes.add(outcome);
        }

        @Override
        public void recordGapDetected() {
            gaps++;
        }

        @Override
        public void recordAuthorizationDenied(Capability capability) {
            denied.add(capability);
        }

        @Override
        public void recordBackpressure() {
            backpressure++;
        }

        @Override
        public void recordDuration(Duration duration) {
        }
    }
}
