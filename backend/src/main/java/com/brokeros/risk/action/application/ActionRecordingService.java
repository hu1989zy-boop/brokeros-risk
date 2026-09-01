package com.brokeros.risk.action.application;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.brokeros.risk.action.application.port.ActionMetricsPort;
import com.brokeros.risk.action.application.port.ActionMutationPort;
import com.brokeros.risk.action.application.port.ActionQueryPort;
import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.ActionOperationType;
import com.brokeros.risk.action.domain.ActionSemanticFingerprint;
import com.brokeros.risk.action.domain.CompletedActionOperation;
import com.brokeros.risk.action.domain.IntentText;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.decision.domain.DecisionProvenanceOutcome;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthorizationDecision;

public final class ActionRecordingService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionQueryPort queryPort;
    private final ActionMutationPort mutationPort;
    private final ActionFingerprintFactory fingerprintFactory;
    private final DecisionProvenanceQueryService decisionQueryService;
    private final AuthorizedMutationFactory mutationFactory;
    private final ActionMetricsPort metrics;

    public ActionRecordingService(
            AuthorizationGuard authorizationGuard,
            ActionQueryPort queryPort,
            ActionMutationPort mutationPort,
            ActionFingerprintFactory fingerprintFactory,
            DecisionProvenanceQueryService decisionQueryService,
            AuthorizedMutationFactory mutationFactory,
            ActionMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.mutationPort = Objects.requireNonNull(mutationPort);
        this.fingerprintFactory = Objects.requireNonNull(fingerprintFactory);
        this.decisionQueryService = Objects.requireNonNull(decisionQueryService);
        this.mutationFactory = Objects.requireNonNull(mutationFactory);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public CompletedActionOperation record(
            ActorContext actorContext,
            RecordActionCommand command) {
        long started = System.nanoTime();
        try {
            CompletedActionOperation result = doRecord(actorContext, command);
            metrics.recordOperation(ActionMetricOperation.RECORD, result.outcome());
            return result;
        } catch (ActionConflictException exception) {
            metrics.recordConflict(exception.getResultCode());
            throw exception;
        } finally {
            metrics.recordDuration(
                    ActionMetricOperation.RECORD,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private CompletedActionOperation doRecord(
            ActorContext actorContext,
            RecordActionCommand command) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(command, "command must not be null");

        AuthorizationDecision authorizationDecision;
        try {
            authorizationDecision = authorizationGuard.requireAllowed(
                    actorContext, ActionCapabilities.RECORD);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(ActionCapabilities.RECORD);
            throw exception;
        }
        requireHuman(actorContext);

        ActionSemanticFingerprint fingerprint = fingerprintFactory.forRecord(
                command.decisionRef(), command.intentText());
        ActionOperationId operationId = operationId(command.operationId());
        Optional<CompletedActionOperation> completed = queryPort.findOperation(operationId);
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), fingerprint);
        }

        IntentText intentText = intentText(command.intentText());
        DecisionRef decisionRef = decisionRef(command.decisionRef());
        try {
            if (decisionQueryService.confirmProvenance(actorContext, decisionRef).outcome()
                    == DecisionProvenanceOutcome.NOT_FOUND) {
                throw new ActionException(ResultCode.ACTION_DECISION_NOT_RECOGNIZED);
            }
        } catch (DecisionAuthorityUnavailableException exception) {
            throw new ActionException(
                    ResultCode.ACTION_DECISION_AUTHORITY_UNAVAILABLE, exception);
        }

        AuthorizedMutationContext context = mutationFactory.create(
                operationId, fingerprint, actorContext,
                authorizationDecision, ActionCapabilities.RECORD);
        return mutationPort.record(
                new RecordActionSpec(operationId, decisionRef, intentText), context);
    }

    private CompletedActionOperation replay(
            CompletedActionOperation completed,
            ActionSemanticFingerprint fingerprint) {
        if (completed.operationType() != ActionOperationType.RECORD
                || !completed.fingerprint().equals(fingerprint)) {
            throw new ActionConflictException();
        }
        return completed;
    }

    private ActionOperationId operationId(String value) {
        try {
            return new ActionOperationId(value);
        } catch (IllegalArgumentException exception) {
            throw new ActionException(ResultCode.ACTION_REQUEST_INVALID, exception);
        }
    }

    private IntentText intentText(String value) {
        try {
            return new IntentText(value);
        } catch (IllegalArgumentException exception) {
            throw new ActionException(ResultCode.ACTION_CONTENT_INVALID, exception);
        }
    }

    private DecisionRef decisionRef(String value) {
        try {
            return new DecisionRef(value);
        } catch (IllegalArgumentException exception) {
            throw new ActionException(ResultCode.ACTION_CONTENT_INVALID, exception);
        }
    }

    private void requireHuman(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.HUMAN) {
            throw new ActionException(ResultCode.ACTION_ACTOR_TYPE_NOT_PERMITTED);
        }
    }
}
