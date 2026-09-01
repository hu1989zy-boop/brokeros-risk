package com.brokeros.risk.actionoutcome.application;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.action.domain.ActionProvenanceOutcome;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMetricsPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeMutationPort;
import com.brokeros.risk.actionoutcome.application.port.ActionOutcomeQueryPort;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationType;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeSemanticFingerprint;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;
import com.brokeros.risk.actionoutcome.domain.OutcomeText;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthorizationDecision;

public final class ActionOutcomeRecordingService {

    private final AuthorizationGuard authorizationGuard;
    private final ActionOutcomeQueryPort queryPort;
    private final ActionOutcomeMutationPort mutationPort;
    private final ActionOutcomeFingerprintFactory fingerprintFactory;
    private final ActionProvenanceQueryService actionQueryService;
    private final AuthorizedMutationFactory mutationFactory;
    private final ActionOutcomeMetricsPort metrics;

    public ActionOutcomeRecordingService(
            AuthorizationGuard authorizationGuard,
            ActionOutcomeQueryPort queryPort,
            ActionOutcomeMutationPort mutationPort,
            ActionOutcomeFingerprintFactory fingerprintFactory,
            ActionProvenanceQueryService actionQueryService,
            AuthorizedMutationFactory mutationFactory,
            ActionOutcomeMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.mutationPort = Objects.requireNonNull(mutationPort);
        this.fingerprintFactory = Objects.requireNonNull(fingerprintFactory);
        this.actionQueryService = Objects.requireNonNull(actionQueryService);
        this.mutationFactory = Objects.requireNonNull(mutationFactory);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public CompletedActionOutcomeOperation record(
            ActorContext actorContext,
            RecordActionOutcomeCommand command) {
        long started = System.nanoTime();
        try {
            CompletedActionOutcomeOperation result = doRecord(actorContext, command);
            metrics.recordOperation(ActionOutcomeMetricOperation.RECORD, result.outcome());
            return result;
        } catch (ActionOutcomeConflictException exception) {
            metrics.recordConflict(exception.getResultCode());
            throw exception;
        } finally {
            metrics.recordDuration(
                    ActionOutcomeMetricOperation.RECORD,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private CompletedActionOutcomeOperation doRecord(
            ActorContext actorContext,
            RecordActionOutcomeCommand command) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(command, "command must not be null");

        AuthorizationDecision authorizationDecision;
        try {
            authorizationDecision = authorizationGuard.requireAllowed(
                    actorContext, ActionOutcomeCapabilities.RECORD);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(ActionOutcomeCapabilities.RECORD);
            throw exception;
        }
        requireHuman(actorContext);

        ActionOutcomeSemanticFingerprint fingerprint = fingerprintFactory.forRecord(
                command.actionRef(), command.outcomeText());
        ActionOutcomeOperationId operationId = operationId(command.operationId());
        Optional<CompletedActionOutcomeOperation> completed =
                queryPort.findOperation(operationId);
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), fingerprint);
        }

        OutcomeText outcomeText = outcomeText(command.outcomeText());
        ActionRef actionRef = actionRef(command.actionRef());
        try {
            if (actionQueryService.confirmProvenance(actorContext, actionRef).outcome()
                    == ActionProvenanceOutcome.NOT_FOUND) {
                throw new ActionOutcomeException(
                        ResultCode.ACTION_OUTCOME_ACTION_NOT_RECOGNIZED);
            }
        } catch (ActionAuthorityUnavailableException exception) {
            throw new ActionOutcomeException(
                    ResultCode.ACTION_OUTCOME_ACTION_AUTHORITY_UNAVAILABLE, exception);
        }

        AuthorizedMutationContext context = mutationFactory.create(
                operationId, fingerprint, actorContext,
                authorizationDecision, ActionOutcomeCapabilities.RECORD);
        return mutationPort.record(
                new RecordActionOutcomeSpec(operationId, actionRef, outcomeText), context);
    }

    private CompletedActionOutcomeOperation replay(
            CompletedActionOutcomeOperation completed,
            ActionOutcomeSemanticFingerprint fingerprint) {
        if (completed.operationType() != ActionOutcomeOperationType.RECORD
                || !completed.fingerprint().equals(fingerprint)) {
            throw new ActionOutcomeConflictException();
        }
        return completed;
    }

    private ActionOutcomeOperationId operationId(String value) {
        try {
            return new ActionOutcomeOperationId(value);
        } catch (IllegalArgumentException exception) {
            throw new ActionOutcomeException(
                    ResultCode.ACTION_OUTCOME_REQUEST_INVALID, exception);
        }
    }

    private OutcomeText outcomeText(String value) {
        try {
            return new OutcomeText(value);
        } catch (IllegalArgumentException exception) {
            throw new ActionOutcomeException(
                    ResultCode.ACTION_OUTCOME_CONTENT_INVALID, exception);
        }
    }

    private ActionRef actionRef(String value) {
        try {
            return new ActionRef(value);
        } catch (IllegalArgumentException exception) {
            throw new ActionOutcomeException(
                    ResultCode.ACTION_OUTCOME_CONTENT_INVALID, exception);
        }
    }

    private void requireHuman(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.HUMAN) {
            throw new ActionOutcomeException(
                    ResultCode.ACTION_OUTCOME_ACTOR_TYPE_NOT_PERMITTED);
        }
    }
}
