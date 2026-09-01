package com.brokeros.risk.actionoutcome.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeAccessLogPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeQueryPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class ActionOutcomeDetailReadService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionOutcomeQueryPort queryPort;
    private final ActionOutcomeAccessLogPort accessLogPort;
    private final ActionOutcomeMetricsPort metrics;
    private final Clock clock;

    public ActionOutcomeDetailReadService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeAccessLogPort accessLogPort,
            ActionOutcomeMetricsPort metrics,
            Clock clock) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.accessLogPort = Objects.requireNonNull(accessLogPort);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    public ActionOutcomeRecord readDetail(
            ActorContext actorContext,
            String actionOutcomeRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(
                        actorContext, ActionOutcomeCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(ActionOutcomeCapabilities.READ);
                throw exception;
            }
            ActionOutcomeRef ref;
            try {
                ref = new ActionOutcomeRef(actionOutcomeRef);
            } catch (IllegalArgumentException exception) {
                throw new ActionOutcomeException(
                        ResultCode.ACTION_OUTCOME_REQUEST_INVALID, exception);
            }
            ActionOutcomeRecord record = queryPort.findByRef(ref)
                    .orElseThrow(() -> new ActionOutcomeException(
                            ResultCode.ACTION_OUTCOME_NOT_FOUND));
            accessLogPort.recordFullDetailAccess(
                    ref, actorContext.actorRef(), clock.instant());
            metrics.recordAccessRead("RECOGNIZED");
            return record;
        } catch (ActionOutcomeException exception) {
            metrics.recordAccessRead(exception.getResultCode().code());
            throw exception;
        } finally {
            metrics.recordDuration(
                    ActionOutcomeMetricOperation.FULL_DETAIL_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
