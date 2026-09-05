package com.brokeros.risk.actionoutcome.application;

import static com.brokeros.risk.api.ReferenceListLimits.REFERENCE_LIST_MAX;

import java.util.List;
import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeQueryPort;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;

public final class ActionOutcomeReferenceListService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionOutcomeQueryPort queryPort;
    private final ActionOutcomeMetricsPort metrics;

    public ActionOutcomeReferenceListService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public List<ActionOutcomeReferenceSummary> listByAction(
            ActorContext actorContext,
            String actionRef) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(
                    actorContext, ActionOutcomeCapabilities.READ);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(ActionOutcomeCapabilities.READ);
            throw exception;
        }
        ActionRef action;
        try {
            action = new ActionRef(actionRef);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ActionOutcomeException(
                    ResultCode.ACTION_OUTCOME_REQUEST_INVALID, exception);
        }
        return queryPort.findSummariesByAction(action, REFERENCE_LIST_MAX);
    }
}
