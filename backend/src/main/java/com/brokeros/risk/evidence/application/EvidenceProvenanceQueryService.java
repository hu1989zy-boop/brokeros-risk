package com.brokeros.risk.evidence.application;

import java.util.Objects;
import java.time.Duration;

import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.domain.EvidenceProvenanceView;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.domain.ActorContext;

public final class EvidenceProvenanceQueryService {

    private final AuthorizationGuard authorizationGuard;
    private final EvidenceQueryPort queryPort;
    private final EvidenceMetricsPort metrics;

    public EvidenceProvenanceQueryService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public EvidenceProvenanceView confirmProvenance(
            ActorContext actorContext,
            EvidenceRef evidenceRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(evidenceRef, "evidenceRef must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(actorContext, EvidenceCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(EvidenceCapabilities.READ);
                throw exception;
            }
            EvidenceProvenanceView view = queryPort.findByRef(evidenceRef)
                    .map(EvidenceProvenanceView::recognized)
                    .orElseGet(() -> EvidenceProvenanceView.notFound(evidenceRef));
            metrics.recordAccessRead(view.outcome().name());
            return view;
        } finally {
            metrics.recordDuration(
                    EvidenceMetricOperation.PROVENANCE_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
