package com.brokeros.risk.decision.application;

import java.time.Duration;
import java.util.Objects;

import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.decision.domain.DecisionProvenanceView;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class DecisionProvenanceQueryService {

    private final AuthorizationGuard authorizationGuard;
    private final DecisionQueryPort queryPort;
    private final DecisionMetricsPort metrics;

    public DecisionProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public DecisionProvenanceView confirmProvenance(
            ActorContext actorContext,
            DecisionRef decisionRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(actorContext, DecisionCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(DecisionCapabilities.READ);
                throw exception;
            }
            DecisionProvenanceView view = queryPort.findByRef(decisionRef)
                    .map(DecisionProvenanceView::recognized)
                    .orElseGet(() -> DecisionProvenanceView.notFound(decisionRef));
            metrics.recordAccessRead(view.outcome().name());
            return view;
        } finally {
            metrics.recordDuration(
                    DecisionMetricOperation.PROVENANCE_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
