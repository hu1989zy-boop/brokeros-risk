package com.brokeros.risk.tradingdata.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.tradingdata.application.port.TradingDataAppendOutcome;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventPublisher;
import com.brokeros.risk.tradingdata.application.port.TradingDataEventStore;
import com.brokeros.risk.tradingdata.application.port.TradingDataMetricsPort;
import com.brokeros.risk.tradingdata.domain.PlatformTag;
import com.brokeros.risk.tradingdata.domain.TradingDataEnvelope;
import com.brokeros.risk.tradingdata.domain.TradingDataIngestionOutcome;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class TradingDataIngestionService {

    private final AuthorizationGuard authorizationGuard;
    private final TradingDataEventStore eventStore;
    private final TradingDataEventPublisher eventPublisher;
    private final TradingDataMetricsPort metrics;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public TradingDataIngestionService(
            AuthorizationGuard authorizationGuard,
            TradingDataEventStore eventStore,
            TradingDataEventPublisher eventPublisher,
            TradingDataMetricsPort metrics,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.eventStore = Objects.requireNonNull(eventStore);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager));
    }

    public TradingDataIngestionResult ingest(
            ActorContext actorContext,
            IngestTradingDataCommand command) {
        long started = System.nanoTime();
        try {
            requireAuthorized(actorContext);
            requireService(actorContext);
            TradingDataEnvelope envelope = envelope(command);
            TradingDataIngestionResult result = transactionTemplate.execute(
                    status -> ingestTransaction(envelope));
            if (result == null) {
                throw new TradingDataAuthorityUnavailableException(
                        new IllegalStateException("ingestion transaction returned no result"));
            }
            if (result.gapDetected()) {
                metrics.recordGapDetected();
            }
            metrics.recordOperation(result.outcome());
            return result;
        } catch (TradingDataBackpressureException exception) {
            metrics.recordBackpressure();
            throw exception;
        } finally {
            metrics.recordDuration(Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private TradingDataIngestionResult ingestTransaction(TradingDataEnvelope envelope) {
        OptionalLong previousSequence =
                eventStore.lastContiguousSequence(envelope.sourceServerId());
        Instant receivedAt = clock.instant();
        TradingDataAppendOutcome appendOutcome = eventStore.append(envelope, receivedAt);
        if (appendOutcome == TradingDataAppendOutcome.DUPLICATE) {
            return new TradingDataIngestionResult(
                    TradingDataIngestionOutcome.DUPLICATE, false);
        }

        boolean gapDetected = previousSequence.isPresent()
                && envelope.sourceSequence() > previousSequence.getAsLong()
                && envelope.sourceSequence() - previousSequence.getAsLong() > 1;
        if (gapDetected) {
            eventStore.recordGap(
                    envelope.sourceServerId(),
                    previousSequence.getAsLong() + 1,
                    envelope.sourceSequence() - 1,
                    receivedAt);
        }
        eventPublisher.publish(envelope);
        return new TradingDataIngestionResult(
                TradingDataIngestionOutcome.ACCEPTED, gapDetected);
    }

    private void requireAuthorized(ActorContext actorContext) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(
                    actorContext, TradingDataCapabilities.INGEST);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(TradingDataCapabilities.INGEST);
            throw exception;
        }
    }

    private void requireService(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.SERVICE) {
            throw new TradingDataException(
                    ResultCode.TRADING_DATA_ACTOR_TYPE_NOT_PERMITTED);
        }
    }

    private TradingDataEnvelope envelope(IngestTradingDataCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        try {
            return new TradingDataEnvelope(
                    command.envelopeVersion(),
                    PlatformTag.valueOf(command.platform()),
                    command.sourceServerId(),
                    command.sourceSequence(),
                    command.tradingAccountId(),
                    command.occurredAt(),
                    command.payload());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new TradingDataException(
                    ResultCode.TRADING_DATA_REQUEST_INVALID, exception);
        }
    }
}
