package com.brokeros.risk.decision.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.port.DecisionAccessLogPort;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class DecisionDetailReadService {

    private final AuthorizationGuard authorizationGuard;
    private final DecisionQueryPort queryPort;
    private final DecisionAccessLogPort accessLogPort;
    private final DecisionMetricsPort metrics;
    private final Clock clock;

    public DecisionDetailReadService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionAccessLogPort accessLogPort,
            DecisionMetricsPort metrics,
            Clock clock) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.accessLogPort = Objects.requireNonNull(accessLogPort);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    public DecisionRecord readDetail(
            ActorContext actorContext,
            String decisionRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(actorContext, DecisionCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(DecisionCapabilities.READ);
                throw exception;
            }
            DecisionRef ref;
            try {
                ref = new DecisionRef(decisionRef);
            } catch (IllegalArgumentException exception) {
                throw new DecisionException(ResultCode.DECISION_REQUEST_INVALID, exception);
            }
            DecisionRecord record = queryPort.findByRef(ref)
                    .orElseThrow(() -> new DecisionException(ResultCode.DECISION_NOT_FOUND));
            accessLogPort.recordFullDetailAccess(
                    ref, actorContext.actorRef(), clock.instant());
            metrics.recordAccessRead("RECOGNIZED");
            return record;
        } catch (DecisionException exception) {
            metrics.recordAccessRead(exception.getResultCode().code());
            throw exception;
        } finally {
            metrics.recordDuration(
                    DecisionMetricOperation.FULL_DETAIL_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
