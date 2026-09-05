package com.brokeros.risk.action.application;

import static com.brokeros.risk.api.ReferenceListLimits.REFERENCE_LIST_MAX;

import java.util.List;
import java.util.Objects;

import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class ActionReferenceListService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionQueryPort queryPort;
    private final ActionMetricsPort metrics;

    public ActionReferenceListService(
            AuthorizationGuard authorizationGuard,
            ActionQueryPort queryPort,
            ActionMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public List<ActionReferenceSummary> listByDecision(
            ActorContext actorContext,
            String decisionRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(actorContext, ActionCapabilities.READ);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(ActionCapabilities.READ);
            throw exception;
        }
        DecisionRef decision;
        try {
            decision = new DecisionRef(decisionRef);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ActionException(ResultCode.ACTION_REQUEST_INVALID, exception);
        }
        return queryPort.findSummariesByDecision(decision, REFERENCE_LIST_MAX);
    }
}
