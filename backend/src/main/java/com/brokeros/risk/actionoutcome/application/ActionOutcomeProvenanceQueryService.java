package com.brokeros.risk.actionoutcome.application;

import java.time.Duration;
import java.util.Objects;

import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeQueryPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeProvenanceView;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class ActionOutcomeProvenanceQueryService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionOutcomeQueryPort queryPort;
    private final ActionOutcomeMetricsPort metrics;

    public ActionOutcomeProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public ActionOutcomeProvenanceView confirmProvenance(
            ActorContext actorContext,
            ActionOutcomeRef actionOutcomeRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(actionOutcomeRef, "actionOutcomeRef must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(
                        actorContext, ActionOutcomeCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(ActionOutcomeCapabilities.READ);
                throw exception;
            }
            ActionOutcomeProvenanceView view = queryPort.findByRef(actionOutcomeRef)
                    .map(ActionOutcomeProvenanceView::recognized)
                    .orElseGet(() ->
                            ActionOutcomeProvenanceView.notFound(actionOutcomeRef));
            metrics.recordAccessRead(view.outcome().name());
            return view;
        } finally {
            metrics.recordDuration(
                    ActionOutcomeMetricOperation.PROVENANCE_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
