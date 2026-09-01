package com.brokeros.risk.action.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import com.brokeros.risk.action.application.port.ActionAccessLogPort;
import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class ActionDetailReadService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionQueryPort queryPort;
    private final ActionAccessLogPort accessLogPort;
    private final ActionMetricsPort metrics;
    private final Clock clock;

    public ActionDetailReadService(
            AuthorizationGuard authorizationGuard,
            ActionQueryPort queryPort,
            ActionAccessLogPort accessLogPort,
            ActionMetricsPort metrics,
            Clock clock) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.accessLogPort = Objects.requireNonNull(accessLogPort);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    public ActionRecord readDetail(
            ActorContext actorContext,
            String actionRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(actorContext, ActionCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(ActionCapabilities.READ);
                throw exception;
            }
            ActionRef ref;
            try {
                ref = new ActionRef(actionRef);
            } catch (IllegalArgumentException exception) {
                throw new ActionException(ResultCode.ACTION_REQUEST_INVALID, exception);
            }
            ActionRecord record = queryPort.findByRef(ref)
                    .orElseThrow(() -> new ActionException(ResultCode.ACTION_NOT_FOUND));
            accessLogPort.recordFullDetailAccess(
                    ref, actorContext.actorRef(), clock.instant());
            metrics.recordAccessRead("RECOGNIZED");
            return record;
        } catch (ActionException exception) {
            metrics.recordAccessRead(exception.getResultCode().code());
            throw exception;
        } finally {
            metrics.recordDuration(
                    ActionMetricOperation.FULL_DETAIL_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
