package com.brokeros.risk.evidence.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.port.EvidenceMutationPort;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.domain.CorrectionReason;
import com.brokeros.risk.evidence.domain.EvidenceFingerprint;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceOperationType;
import com.brokeros.risk.evidence.domain.EvidenceRecord;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.evidence.domain.EvidenceStatus;
import com.brokeros.risk.evidence.domain.ObservationText;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthorizationDecision;

public final class EvidenceCorrectionService {

    private final AuthorizationGuard authorizationGuard;
    private final EvidenceQueryPort queryPort;
    private final EvidenceMutationPort mutationPort;
    private final EvidenceFingerprintFactory fingerprintFactory;
    private final EvidenceMetricsPort metrics;
    private final Clock clock;

    public EvidenceCorrectionService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMutationPort mutationPort,
            EvidenceFingerprintFactory fingerprintFactory,
            EvidenceMetricsPort metrics,
            Clock clock) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.mutationPort = Objects.requireNonNull(mutationPort);
        this.fingerprintFactory = Objects.requireNonNull(fingerprintFactory);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    public EvidenceCorrectionResult correct(
            ActorContext actorContext,
            CorrectEvidenceCommand command) {
        long started = System.nanoTime();
        try {
            EvidenceCorrectionResult result = doCorrect(actorContext, command);
            metrics.recordOperation(EvidenceMetricOperation.CORRECT, result.outcome());
            return result;
        } catch (EvidenceConflictException exception) {
            metrics.recordConflict(exception.getResultCode());
            throw exception;
        } finally {
            metrics.recordDuration(
                    EvidenceMetricOperation.CORRECT,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private EvidenceCorrectionResult doCorrect(
            ActorContext actorContext,
            CorrectEvidenceCommand command) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(command, "command must not be null");

        AuthorizationDecision decision;
        try {
            decision = authorizationGuard.requireAllowed(
                    actorContext, EvidenceCapabilities.CORRECT);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(EvidenceCapabilities.CORRECT);
            throw exception;
        }
        requireHuman(actorContext);

        EvidenceFingerprint fingerprint = fingerprintFactory.forCorrection(
                command.targetEvidenceRef(),
                command.correctionReason(),
                command.observationText());
        EvidenceOperationId operationId = operationId(command.operationId());
        Optional<CompletedEvidenceOperation> completed = queryPort.findOperation(operationId);
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), fingerprint);
        }

        CorrectionReason reason;
        ObservationText observationText;
        try {
            reason = new CorrectionReason(command.correctionReason());
            observationText = new ObservationText(command.observationText());
        } catch (IllegalArgumentException exception) {
            throw new EvidenceException(ResultCode.EVIDENCE_CONTENT_INVALID, exception);
        }
        EvidenceRef targetRef = evidenceRef(command.targetEvidenceRef());
        EvidenceRecord target = queryPort.findByRef(targetRef)
                .orElseThrow(() -> new EvidenceException(ResultCode.EVIDENCE_NOT_FOUND));
        if (target.status() != EvidenceStatus.ACTIVE) {
            throw new EvidenceConflictException(ResultCode.EVIDENCE_ALREADY_SUPERSEDED);
        }

        AuthorizedMutationContext context = new AuthorizedMutationContext(
                operationId, fingerprint, actorContext, decision,
                EvidenceCapabilities.CORRECT, clock.instant());
        return mutationPort.correct(
                new CorrectEvidenceSpec(
                        operationId, targetRef, reason, observationText),
                context);
    }

    private EvidenceCorrectionResult replay(
            CompletedEvidenceOperation completed,
            EvidenceFingerprint fingerprint) {
        if (completed.operationType() != EvidenceOperationType.CORRECT
                || !completed.fingerprint().equals(fingerprint)) {
            throw new EvidenceConflictException(ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT);
        }
        return new EvidenceCorrectionResult(
                completed.resultEvidenceRef(), completed.outcome(), completed.occurredAt());
    }

    private EvidenceOperationId operationId(String value) {
        try {
            return new EvidenceOperationId(value);
        } catch (IllegalArgumentException exception) {
            throw new EvidenceException(ResultCode.EVIDENCE_REQUEST_INVALID, exception);
        }
    }

    private EvidenceRef evidenceRef(String value) {
        try {
            return new EvidenceRef(value);
        } catch (IllegalArgumentException exception) {
            throw new EvidenceException(ResultCode.EVIDENCE_REQUEST_INVALID, exception);
        }
    }

    private void requireHuman(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.HUMAN) {
            throw new EvidenceException(ResultCode.EVIDENCE_ACTOR_TYPE_NOT_PERMITTED);
        }
    }
}
