package com.brokeros.risk.action.application;

import java.time.Duration;
import java.util.Objects;

import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.action.domain.ActionProvenanceView;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class ActionProvenanceQueryService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionQueryPort queryPort;
    private final ActionMetricsPort metrics;

    public ActionProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            ActionQueryPort queryPort,
            ActionMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public ActionProvenanceView confirmProvenance(
            ActorContext actorContext,
            ActionRef actionRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(actionRef, "actionRef must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(actorContext, ActionCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(ActionCapabilities.READ);
                throw exception;
            }
            ActionProvenanceView view = queryPort.findByRef(actionRef)
                    .map(ActionProvenanceView::recognized)
                    .orElseGet(() -> ActionProvenanceView.notFound(actionRef));
            metrics.recordAccessRead(view.outcome().name());
            return view;
        } finally {
            metrics.recordDuration(
                    ActionMetricOperation.PROVENANCE_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
