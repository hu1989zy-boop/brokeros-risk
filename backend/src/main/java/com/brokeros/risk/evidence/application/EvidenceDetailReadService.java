package com.brokeros.risk.evidence.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.port.EvidenceAccessLogPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.domain.ActorContext;

public final class EvidenceDetailReadService {

    private final AuthorizationGuard authorizationGuard;
    private final EvidenceQueryPort queryPort;
    private final EvidenceAccessLogPort accessLogPort;
    private final EvidenceMetricsPort metrics;
    private final Clock clock;

    public EvidenceDetailReadService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceAccessLogPort accessLogPort,
            EvidenceMetricsPort metrics,
            Clock clock) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.accessLogPort = Objects.requireNonNull(accessLogPort);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    public EvidenceRecord read(ActorContext actorContext, String evidenceRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        long started = System.nanoTime();
        try {
            try {
                authorizationGuard.requireAllowed(actorContext, EvidenceCapabilities.READ);
            } catch (AuthorizationDeniedException exception) {
                metrics.recordAuthorizationDenied(EvidenceCapabilities.READ);
                throw exception;
            }
            EvidenceRef ref;
            try {
                ref = new EvidenceRef(evidenceRef);
            } catch (IllegalArgumentException exception) {
                throw new EvidenceException(ResultCode.EVIDENCE_REQUEST_INVALID, exception);
            }
            EvidenceRecord record = queryPort.findByRef(ref)
                    .orElseThrow(() -> new EvidenceException(ResultCode.EVIDENCE_NOT_FOUND));
            accessLogPort.recordFullDetailAccess(
                    ref, actorContext.actorRef(), clock.instant());
            metrics.recordAccessRead("RECOGNIZED");
            return record;
        } catch (EvidenceException exception) {
            metrics.recordAccessRead(exception.getResultCode().code());
            throw exception;
        } finally {
            metrics.recordDuration(
                    EvidenceMetricOperation.FULL_DETAIL_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
