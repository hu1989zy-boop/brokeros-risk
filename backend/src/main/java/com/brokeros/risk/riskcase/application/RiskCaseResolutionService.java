package com.brokeros.risk.riskcase.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.application.port.ActionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.DecisionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.EvidenceReferenceQuery;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.ResolutionOutcome;
import com.brokeros.risk.riskcase.domain.ResolutionRecord;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCaseDomainException;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.riskcase.domain.RiskCaseStatus;
import com.brokeros.risk.riskcase.domain.RiskCaseText;
import com.brokeros.risk.riskcase.domain.TransitionRecord;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class RiskCaseResolutionService {

    private final AuthorizationGuard authorizationGuard;
    private final DecisionReferenceQuery decisionQuery;
    private final EvidenceReferenceQuery evidenceQuery;
    private final ActionReferenceQuery actionQuery;
    private final RiskCaseRepository repository;
    private final AuditRecordWriter auditWriter;
    private final RiskCaseAuditFactory auditFactory;
    private final RiskCaseMetricsPort metrics;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public RiskCaseResolutionService(
            AuthorizationGuard authorizationGuard,
            DecisionReferenceQuery decisionQuery,
            EvidenceReferenceQuery evidenceQuery,
            ActionReferenceQuery actionQuery,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory auditFactory,
            RiskCaseMetricsPort metrics,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.decisionQuery = Objects.requireNonNull(decisionQuery);
        this.evidenceQuery = Objects.requireNonNull(evidenceQuery);
        this.actionQuery = Objects.requireNonNull(actionQuery);
        this.repository = Objects.requireNonNull(repository);
        this.auditWriter = Objects.requireNonNull(auditWriter);
        this.auditFactory = Objects.requireNonNull(auditFactory);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public RiskCaseResolutionResult resolve(
            ActorContext actorContext,
            String rawCaseNumber,
            String rawOutcome,
            String summary,
            Set<String> selectedEvidenceRefs,
            Set<String> selectedActionRefs,
            long expectedVersion) {
        long started = System.nanoTime();
        requireAuthorized(actorContext);
        CaseNumber caseNumber = caseNumber(rawCaseNumber);
        ResolutionOutcome outcome = outcome(rawOutcome);
        String resolutionSummary;
        try {
            resolutionSummary = RiskCaseText.require(summary, 2000, "resolution summary");
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
        Set<EvidenceRef> evidenceRefs = evidenceRefs(selectedEvidenceRefs);
        Set<ActionRef> actionRefs = actionRefs(selectedActionRefs);
        try {
            RiskCaseResolutionResult result = transactionTemplate.execute(status -> {
                RiskCase riskCase = repository.findByCaseNumber(caseNumber)
                        .orElseThrow(() -> new RiskCaseException(ResultCode.RISK_CASE_NOT_FOUND));
                RiskCaseSnapshot before = riskCase.snapshot();
                if (before.currentDecisionRef() == null) {
                    throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
                }
                decisionQuery.requireRecognized(actorContext, before.currentDecisionRef());
                List<RiskCaseRepository.EffectiveEvidence> effectiveEvidence =
                        repository.findAllEffectiveEvidence(before.id());
                List<RiskCaseRepository.EffectiveAction> effectiveActions =
                        repository.findAllEffectiveActions(before.id());
                List<RiskCaseRepository.EffectiveEvidence> selectedEvidence = evidenceRefs.stream()
                        .map(reference -> effectiveEvidence.stream()
                                .filter(candidate -> candidate.evidenceRef().equals(reference))
                                .findFirst()
                                .orElseThrow(() -> new RiskCaseException(
                                        ResultCode.RISK_CASE_INVARIANT_VIOLATION)))
                        .toList();
                List<RiskCaseRepository.EffectiveAction> selectedActions = actionRefs.stream()
                        .map(reference -> effectiveActions.stream()
                                .filter(candidate -> candidate.actionRef().equals(reference))
                                .findFirst()
                                .orElseThrow(() -> new RiskCaseException(
                                        ResultCode.RISK_CASE_INVARIANT_VIOLATION)))
                        .toList();
                evidenceRefs.forEach(reference ->
                        evidenceQuery.requireRecognized(actorContext, reference));
                actionRefs.forEach(reference ->
                        actionQuery.requireRecognized(actorContext, reference));
                boolean allActionsHaveOutcomes = effectiveActions.stream()
                        .allMatch(action -> action.outcomeRef() != null);
                if (before.status() == RiskCaseStatus.ACTION_REQUIRED
                        && (selectedActions.size() != effectiveActions.size()
                        || !selectedActions.containsAll(effectiveActions))) {
                    throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
                }
                Instant occurredAt = clock.instant();
                TransitionRecord transition = riskCase.resolve(
                        allActionsHaveOutcomes, actorContext.actorRef(), resolutionSummary,
                        occurredAt, expectedVersion);
                RiskCaseSnapshot after = riskCase.snapshot();
                if (repository.updateRoot(after, expectedVersion) != 1) {
                    metrics.recordConflict("VERSION");
                    throw new RiskCaseException(ResultCode.RISK_CASE_VERSION_CONFLICT);
                }
                repository.appendTransition(transition);
                ResolutionRecord resolution = repository.appendResolution(
                        new ResolutionRecord(null, after.id(), after.currentCycle(),
                                after.version(), outcome, after.currentDecisionRef(),
                                resolutionSummary, actorContext.actorRef(), occurredAt));
                selectedEvidence.forEach(reference ->
                        repository.appendResolutionEvidence(resolution.id(), reference));
                selectedActions.forEach(reference ->
                        repository.appendResolutionAction(resolution.id(), reference));
                auditWriter.append(auditFactory.material(
                        before, after, actorContext, occurredAt, "RISK_CASE_RESOLVED",
                        "RESOLUTION", String.valueOf(after.currentCycle().value()),
                        resolutionSummary));
                return new RiskCaseResolutionResult(after, resolution);
            });
            if (result == null) {
                throw new IllegalStateException("resolution transaction returned no result");
            }
            metrics.recordSuccess(RiskCaseMetricOperation.RESOLVE);
            return result;
        } catch (RiskCaseDomainException exception) {
            throw RiskCaseErrors.translate(exception);
        } finally {
            metrics.recordDuration(RiskCaseMetricOperation.RESOLVE,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private void requireAuthorized(ActorContext actorContext) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(actorContext, RiskCaseCapabilities.RESOLVE);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(RiskCaseCapabilities.RESOLVE);
            throw exception;
        }
    }

    private CaseNumber caseNumber(String value) {
        try {
            return new CaseNumber(value);
        } catch (IllegalArgumentException exception) {
            throw new RiskCaseException(ResultCode.RISK_CASE_NOT_FOUND, exception);
        }
    }

    private ResolutionOutcome outcome(String value) {
        try {
            return ResolutionOutcome.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private Set<EvidenceRef> evidenceRefs(Set<String> values) {
        Set<EvidenceRef> refs = new LinkedHashSet<>();
        if (values == null) {
            return refs;
        }
        try {
            values.forEach(value -> refs.add(new EvidenceRef(value)));
            return refs;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private Set<ActionRef> actionRefs(Set<String> values) {
        Set<ActionRef> refs = new LinkedHashSet<>();
        if (values == null) {
            return refs;
        }
        try {
            values.forEach(value -> refs.add(new ActionRef(value)));
            return refs;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }
}
