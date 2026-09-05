package com.brokeros.risk.decision.application;

import static com.brokeros.risk.api.ReferenceListLimits.REFERENCE_LIST_MAX;

import java.util.List;
import java.util.Objects;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public final class DecisionReferenceListService {

    private final AuthorizationGuard authorizationGuard;
    private final DecisionQueryPort queryPort;
    private final DecisionMetricsPort metrics;

    public DecisionReferenceListService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public List<DecisionReferenceSummary> listBySubject(
            ActorContext actorContext,
            String subjectRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(actorContext, DecisionCapabilities.READ);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(DecisionCapabilities.READ);
            throw exception;
        }
        TradingAccountRef subject;
        try {
            subject = new TradingAccountRef(subjectRef);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new DecisionException(ResultCode.DECISION_REQUEST_INVALID, exception);
        }
        return queryPort.findSummariesBySubject(subject, REFERENCE_LIST_MAX);
    }
}
