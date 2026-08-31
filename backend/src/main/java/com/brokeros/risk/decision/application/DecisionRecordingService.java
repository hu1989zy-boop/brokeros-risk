package com.brokeros.risk.decision.application;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.port.DecisionMetricsPort;
import com.brokeros.risk.decision.application.port.DecisionMutationPort;
import com.brokeros.risk.decision.application.port.DecisionQueryPort;
import com.brokeros.risk.decision.domain.ConclusionText;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionOperationType;
import com.brokeros.risk.decision.domain.DecisionSemanticFingerprint;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceProvenanceQueryService;
import com.brokeros.risk.evidence.domain.EvidenceProvenanceOutcome;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthorizationDecision;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.domain.EligibilityDecision;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountReferenceEligibility;

public final class DecisionRecordingService {

    private final AuthorizationGuard authorizationGuard;
    private final DecisionQueryPort queryPort;
    private final DecisionMutationPort mutationPort;
    private final DecisionFingerprintFactory fingerprintFactory;
    private final TradingAccountReferenceEligibilityService eligibilityService;
    private final EvidenceProvenanceQueryService evidenceQueryService;
    private final AuthorizedMutationFactory mutationFactory;
    private final DecisionMetricsPort metrics;

    public DecisionRecordingService(
            AuthorizationGuard authorizationGuard,
            DecisionQueryPort queryPort,
            DecisionMutationPort mutationPort,
            DecisionFingerprintFactory fingerprintFactory,
            TradingAccountReferenceEligibilityService eligibilityService,
            EvidenceProvenanceQueryService evidenceQueryService,
            AuthorizedMutationFactory mutationFactory,
            DecisionMetricsPort metrics) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.mutationPort = Objects.requireNonNull(mutationPort);
        this.fingerprintFactory = Objects.requireNonNull(fingerprintFactory);
        this.eligibilityService = Objects.requireNonNull(eligibilityService);
        this.evidenceQueryService = Objects.requireNonNull(evidenceQueryService);
        this.mutationFactory = Objects.requireNonNull(mutationFactory);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public CompletedDecisionOperation record(
            ActorContext actorContext,
            RecordDecisionCommand command) {
        long started = System.nanoTime();
        try {
            CompletedDecisionOperation result = doRecord(actorContext, command);
            metrics.recordOperation(DecisionMetricOperation.RECORD, result.outcome());
            return result;
        } catch (DecisionConflictException exception) {
            metrics.recordConflict(exception.getResultCode());
            throw exception;
        } finally {
            metrics.recordDuration(
                    DecisionMetricOperation.RECORD,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private CompletedDecisionOperation doRecord(
            ActorContext actorContext,
            RecordDecisionCommand command) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        Objects.requireNonNull(command, "command must not be null");

        AuthorizationDecision authorizationDecision;
        try {
            authorizationDecision = authorizationGuard.requireAllowed(
                    actorContext, DecisionCapabilities.RECORD);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(DecisionCapabilities.RECORD);
            throw exception;
        }
        requireHuman(actorContext);

        DecisionSemanticFingerprint fingerprint = fingerprintFactory.forRecord(
                command.subjectRef(), command.evidenceRefs(), command.conclusionText());
        DecisionOperationId operationId = operationId(command.operationId());
        Optional<CompletedDecisionOperation> completed = queryPort.findOperation(operationId);
        if (completed.isPresent()) {
            return replay(completed.orElseThrow(), fingerprint);
        }

        ConclusionText conclusionText = conclusionText(command.conclusionText());
        Set<EvidenceRef> evidenceRefs = evidenceRefs(command.evidenceRefs());
        TradingAccountRef subjectRef = subjectRef(command.subjectRef());

        TradingAccountReferenceEligibility eligibility;
        try {
            eligibility = eligibilityService.validateForNewRiskCaseAssociation(
                    actorContext, subjectRef);
        } catch (TradingAccountAuthorityUnavailableException exception) {
            throw new DecisionException(
                    ResultCode.DECISION_SUBJECT_AUTHORITY_UNAVAILABLE, exception);
        }
        if (eligibility.decision() == EligibilityDecision.NOT_RECOGNIZED) {
            throw new DecisionException(ResultCode.DECISION_SUBJECT_NOT_RECOGNIZED);
        }

        for (EvidenceRef evidenceRef : evidenceRefs) {
            try {
                if (evidenceQueryService.confirmProvenance(actorContext, evidenceRef).outcome()
                        == EvidenceProvenanceOutcome.NOT_FOUND) {
                    throw new DecisionException(
                            ResultCode.DECISION_EVIDENCE_NOT_RECOGNIZED);
                }
            } catch (EvidenceAuthorityUnavailableException exception) {
                throw new DecisionException(
                        ResultCode.DECISION_EVIDENCE_AUTHORITY_UNAVAILABLE, exception);
            }
        }

        AuthorizedMutationContext context = mutationFactory.create(
                operationId, fingerprint, actorContext,
                authorizationDecision, DecisionCapabilities.RECORD);
        return mutationPort.record(
                new RecordDecisionSpec(
                        operationId, subjectRef, evidenceRefs, conclusionText),
                context);
    }

    private CompletedDecisionOperation replay(
            CompletedDecisionOperation completed,
            DecisionSemanticFingerprint fingerprint) {
        if (completed.operationType() != DecisionOperationType.RECORD
                || !completed.fingerprint().equals(fingerprint)) {
            throw new DecisionConflictException();
        }
        return completed;
    }

    private DecisionOperationId operationId(String value) {
        try {
            return new DecisionOperationId(value);
        } catch (IllegalArgumentException exception) {
            throw new DecisionException(ResultCode.DECISION_REQUEST_INVALID, exception);
        }
    }

    private ConclusionText conclusionText(String value) {
        try {
            return new ConclusionText(value);
        } catch (IllegalArgumentException exception) {
            throw new DecisionException(ResultCode.DECISION_CONTENT_INVALID, exception);
        }
    }

    private Set<EvidenceRef> evidenceRefs(java.util.List<String> values) {
        if (values.isEmpty()) {
            throw new DecisionException(ResultCode.DECISION_CONTENT_INVALID);
        }
        TreeSet<EvidenceRef> sorted = new TreeSet<>(Comparator.comparing(EvidenceRef::value));
        try {
            for (String value : values) {
                sorted.add(new EvidenceRef(value));
            }
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new DecisionException(ResultCode.DECISION_CONTENT_INVALID, exception);
        }
        if (sorted.isEmpty()) {
            throw new DecisionException(ResultCode.DECISION_CONTENT_INVALID);
        }
        return new LinkedHashSet<>(sorted);
    }

    private TradingAccountRef subjectRef(String value) {
        try {
            return new TradingAccountRef(value);
        } catch (IllegalArgumentException exception) {
            throw new DecisionException(ResultCode.DECISION_REQUEST_INVALID, exception);
        }
    }

    private void requireHuman(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.HUMAN) {
            throw new DecisionException(ResultCode.DECISION_ACTOR_TYPE_NOT_PERMITTED);
        }
    }
}
