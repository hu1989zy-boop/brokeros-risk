package com.brokeros.risk.evidence.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.port.EvidenceMutationPort;
import com.brokeros.risk.evidence.application.port.EvidenceMetricsPort;
import com.brokeros.risk.evidence.application.port.EvidenceQueryPort;
import com.brokeros.risk.evidence.domain.EvidenceFingerprint;
import com.brokeros.risk.evidence.domain.EvidenceOperationId;
import com.brokeros.risk.evidence.domain.EvidenceOperationType;
import com.brokeros.risk.evidence.domain.ObservationText;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.domain.EligibilityDecision;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountReferenceEligibility;

public final class EvidenceRecordingService {

    private final AuthorizationGuard authorizationGuard;
    private final EvidenceQueryPort queryPort;
    private final EvidenceMutationPort mutationPort;
    private final EvidenceFingerprintFactory fingerprintFactory;
    private final TradingAccountReferenceEligibilityService eligibilityService;
    private final EvidenceMetricsPort metrics;
    private final Clock clock;

    public EvidenceRecordingService(
            AuthorizationGuard authorizationGuard,
            EvidenceQueryPort queryPort,
            EvidenceMutationPort mutationPort,
            EvidenceFingerprintFactory fingerprintFactory,
            TradingAccountReferenceEligibilityService eligibilityService,
            EvidenceMetricsPort metrics,
            Clock clock) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.mutationPort = Objects.requireNonNull(mutationPort);
        this.fingerprintFactory = Objects.requireNonNull(fingerprintFactory);
        this.eligibilityService = Objects.requireNonNull(eligibilityService);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
    }

    public EvidenceRecordingResult record(
            ActorContext actorContext,
            RecordEvidenceCommand command) {
        long started = System.nanoTime();
        try {
            EvidenceRecordingResult result = doRecord(actorContext, command);
            metrics.recordOperation(EvidenceMetricOperation.RECORD, result.outcome());
            return result;
        } catch (EvidenceConflictException exception) {
            metrics.recordConflict(exception.getResultCode());
            throw exception;
        } finally {
            metrics.recordDuration(
                    EvidenceMetricOperation.RECORD,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private EvidenceRecordingResult doRecord(
            ActorContext actorContext,
            RecordEvidenceCommand command) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(command, "command must not be null");

        AuthorizationDecision decision;
        try {
            decision = authorizationGuard.requireAllowed(
                    actorContext, EvidenceCapabilities.RECORD);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(EvidenceCapabilities.RECORD);
            throw exception;
        }
        requireHuman(actorContext);

        EvidenceFingerprint fingerprint = fingerprintFactory.forRecord(
                command.subjectRef(), command.observationText());
        EvidenceOperationId operationId = operationId(command.operationId());
        Optional<CompletedEvidenceOperation> completed = queryPort.findOperation(operationId);
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), fingerprint);
        }

        ObservationText observationText;
        TradingAccountRef subjectRef;
        try {
            observationText = new ObservationText(command.observationText());
        } catch (IllegalArgumentException exception) {
            throw new EvidenceException(ResultCode.EVIDENCE_CONTENT_INVALID, exception);
        }
        try {
            subjectRef = new TradingAccountRef(command.subjectRef());
        } catch (IllegalArgumentException exception) {
            throw new EvidenceException(ResultCode.EVIDENCE_REQUEST_INVALID, exception);
        }

        TradingAccountReferenceEligibility eligibility;
        try {
            eligibility = eligibilityService.validateForNewRiskCaseAssociation(
                    actorContext, subjectRef);
        } catch (TradingAccountAuthorityUnavailableException exception) {
            throw new EvidenceException(
                    ResultCode.EVIDENCE_SUBJECT_AUTHORITY_UNAVAILABLE, exception);
        }
        if (eligibility.decision() == EligibilityDecision.NOT_RECOGNIZED) {
            throw new EvidenceException(ResultCode.EVIDENCE_SUBJECT_NOT_RECOGNIZED);
        }

        AuthorizedMutationContext context = new AuthorizedMutationContext(
                operationId, fingerprint, actorContext, decision,
                EvidenceCapabilities.RECORD, clock.instant());
        return mutationPort.record(
                new RecordEvidenceSpec(operationId, subjectRef, observationText), context);
    }

    private EvidenceRecordingResult replay(
            CompletedEvidenceOperation completed,
            EvidenceFingerprint fingerprint) {
        if (completed.operationType() != EvidenceOperationType.RECORD
                || !completed.fingerprint().equals(fingerprint)) {
            throw new EvidenceConflictException(ResultCode.EVIDENCE_IDEMPOTENCY_CONFLICT);
        }
        return new EvidenceRecordingResult(
                completed.resultEvidenceRef(), completed.outcome(), completed.occurredAt());
    }

    private EvidenceOperationId operationId(String value) {
        try {
            return new EvidenceOperationId(value);
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
