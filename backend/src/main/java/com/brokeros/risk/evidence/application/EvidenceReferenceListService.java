package com.brokeros.risk.evidence.application;

import static com.brokeros.risk.api.ReferenceListLimits.REFERENCE_LIST_MAX;

import java.util.List;
import java.util.Objects;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public final class EvidenceReferenceListService {

    private final AuthorizationGuard authorizationGuard;
    private final EvidenceQueryPort queryPort;
    private final EvidenceMetricsPort metrics;

    public EvidenceReferenceListService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public List<EvidenceReferenceSummary> listBySubject(
            ActorContext actorContext,
            String subjectRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(actorContext, EvidenceCapabilities.READ);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(EvidenceCapabilities.READ);
            throw exception;
        }
        TradingAccountRef subject;
        try {
            subject = new TradingAccountRef(subjectRef);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new EvidenceException(ResultCode.EVIDENCE_REQUEST_INVALID, exception);
        }
        return queryPort.findSummariesBySubject(subject, REFERENCE_LIST_MAX);
    }
}
