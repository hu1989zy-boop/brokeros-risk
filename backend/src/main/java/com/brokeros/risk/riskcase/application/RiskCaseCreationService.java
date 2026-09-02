package com.brokeros.risk.riskcase.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.riskcase.application.port.DecisionReferenceQuery;
import com.brokeros.risk.riskcase.application.port.RiskCaseConflictKind;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCasePersistenceConflictException;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.application.port.TradingAccountReferenceQuery;
import com.brokeros.risk.riskcase.domain.CaseIntakeSource;
import com.brokeros.risk.riskcase.domain.CaseNumberGenerator;
import com.brokeros.risk.riskcase.domain.DecisionAssociation;
import com.brokeros.risk.riskcase.domain.DecisionSelectionRecord;
import com.brokeros.risk.riskcase.domain.ResolutionCycleNumber;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCasePriority;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.riskcase.domain.RiskCaseStatus;
import com.brokeros.risk.riskcase.domain.RiskCaseText;
import com.brokeros.risk.riskcase.domain.RiskCaseTransitionOperation;
import com.brokeros.risk.riskcase.domain.TradingAccountSubjectRef;
import com.brokeros.risk.riskcase.domain.TransitionRecord;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class RiskCaseCreationService {

    private static final int MAX_CASE_NUMBER_ATTEMPTS = 3;

    private final AuthorizationGuard authorizationGuard;
    private final TradingAccountReferenceQuery subjectQuery;
    private final DecisionReferenceQuery decisionQuery;
    private final RiskCaseRepository repository;
    private final AuditRecordWriter auditWriter;
    private final CaseNumberGenerator caseNumberGenerator;
    private final RiskCaseFingerprintFactory fingerprintFactory;
    private final RiskCaseAuditFactory auditFactory;
    private final RiskCaseMetricsPort metrics;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public RiskCaseCreationService(
            AuthorizationGuard authorizationGuard,
            TradingAccountReferenceQuery subjectQuery,
            DecisionReferenceQuery decisionQuery,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            CaseNumberGenerator caseNumberGenerator,
            RiskCaseFingerprintFactory fingerprintFactory,
            RiskCaseAuditFactory auditFactory,
            RiskCaseMetricsPort metrics,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.subjectQuery = Objects.requireNonNull(subjectQuery);
        this.decisionQuery = Objects.requireNonNull(decisionQuery);
        this.repository = Objects.requireNonNull(repository);
        this.auditWriter = Objects.requireNonNull(auditWriter);
        this.caseNumberGenerator = Objects.requireNonNull(caseNumberGenerator);
        this.fingerprintFactory = Objects.requireNonNull(fingerprintFactory);
        this.auditFactory = Objects.requireNonNull(auditFactory);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public RiskCaseSnapshot create(ActorContext actorContext, CreateRiskCaseCommand command) {
        long started = System.nanoTime();
        try {
            requireAuthorized(actorContext);
            Objects.requireNonNull(command, "command must not be null");
            byte[] keyHash = fingerprintFactory.idempotencyKeyHash(command.idempotencyKey());
            byte[] requestHash = fingerprintFactory.requestHash(command);
            RiskCaseCreationRecord replay = repository
                    .findByCreationKey(actorContext.actorRef(), keyHash)
                    .orElse(null);
            if (replay != null) {
                return replay(replay, requestHash);
            }
            ParsedCreate parsed = parse(command);
            for (int attempt = 1; attempt <= MAX_CASE_NUMBER_ATTEMPTS; attempt++) {
                try {
                    RiskCaseSnapshot created = transactionTemplate.execute(status ->
                            createTransaction(actorContext, parsed, keyHash, requestHash));
                    if (created == null) {
                        throw new IllegalStateException("creation transaction returned no result");
                    }
                    metrics.recordSuccess(RiskCaseMetricOperation.CREATE);
                    return created;
                } catch (RiskCasePersistenceConflictException exception) {
                    if (exception.kind() == RiskCaseConflictKind.CASE_NUMBER
                            && attempt < MAX_CASE_NUMBER_ATTEMPTS) {
                        continue;
                    }
                    if (exception.kind() == RiskCaseConflictKind.CREATION_KEY) {
                        RiskCaseCreationRecord concurrentReplay = repository
                                .findByCreationKey(actorContext.actorRef(), keyHash)
                                .orElseThrow(() -> exception);
                        return replay(concurrentReplay, requestHash);
                    }
                    if (exception.kind() == RiskCaseConflictKind.PRIMARY_DECISION) {
                        metrics.recordConflict("PRIMARY_DECISION");
                        throw new RiskCaseException(
                                ResultCode.RISK_CASE_PRIMARY_DECISION_CONFLICT, exception);
                    }
                    throw exception;
                }
            }
            throw new IllegalStateException("case number retry bound exhausted");
        } finally {
            metrics.recordDuration(RiskCaseMetricOperation.CREATE,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private RiskCaseSnapshot createTransaction(
            ActorContext actorContext,
            ParsedCreate parsed,
            byte[] keyHash,
            byte[] requestHash) {
        subjectQuery.requireEligibleForNewCase(actorContext, parsed.subjectRef());
        if (parsed.decisionRef() != null) {
            decisionQuery.requireRecognized(actorContext, parsed.decisionRef());
        }
        Instant occurredAt = clock.instant();
        TradingAccountSubjectRef subject = new TradingAccountSubjectRef(parsed.subjectRef());
        RiskCase riskCase = parsed.intakeSource() == CaseIntakeSource.MANUAL
                ? RiskCase.openManual(caseNumberGenerator.generate(), subject,
                        parsed.summary(), parsed.priority(), actorContext.actorRef(), occurredAt)
                : RiskCase.openDecisionDriven(caseNumberGenerator.generate(), subject,
                        parsed.summary(), parsed.priority(), parsed.decisionRef(),
                        actorContext.actorRef(), occurredAt);
        repository.insertRoot(riskCase, keyHash, requestHash);
        RiskCaseSnapshot created = riskCase.snapshot();
        repository.appendTransition(new TransitionRecord(
                created.id(), 1, new ResolutionCycleNumber(1),
                RiskCaseTransitionOperation.CREATE, null, RiskCaseStatus.OPEN,
                parsed.summary(), actorContext.actorRef(), occurredAt));
        if (parsed.decisionRef() != null) {
            repository.appendDecisionAssociation(new DecisionAssociation(
                    null, created.id(), 1, parsed.decisionRef(), actorContext.actorRef(),
                    "decision-driven intake", occurredAt));
            repository.appendDecisionSelection(new DecisionSelectionRecord(
                    created.id(), 1, null, parsed.decisionRef(), actorContext.actorRef(),
                    "decision-driven intake", occurredAt));
        }
        auditWriter.append(auditFactory.material(null, created, actorContext, occurredAt,
                "RISK_CASE_CREATED", "TRADING_ACCOUNT", parsed.subjectRef().value(),
                parsed.summary()));
        return created;
    }

    private ParsedCreate parse(CreateRiskCaseCommand command) {
        try {
            if (!"TRADING_ACCOUNT".equals(command.subjectType())) {
                throw new IllegalArgumentException("subjectType must be TRADING_ACCOUNT");
            }
            CaseIntakeSource source = CaseIntakeSource.valueOf(command.intakeSource());
            TradingAccountRef subject = new TradingAccountRef(command.subjectRef());
            String summary = RiskCaseText.require(command.intakeSummary(), 1000, "intakeSummary");
            RiskCasePriority priority = RiskCasePriority.valueOf(command.priority());
            DecisionRef decision = command.decisionRef() == null
                    ? null
                    : new DecisionRef(command.decisionRef());
            if (source == CaseIntakeSource.MANUAL && decision != null) {
                throw new IllegalArgumentException("manual intake must omit decisionRef");
            }
            if (source == CaseIntakeSource.DECISION_DRIVEN && decision == null) {
                throw new IllegalArgumentException("decision-driven intake requires decisionRef");
            }
            return new ParsedCreate(source, subject, summary, priority, decision);
        } catch (IllegalArgumentException exception) {
            throw RiskCaseErrors.invalid(exception);
        }
    }

    private RiskCaseSnapshot replay(RiskCaseCreationRecord record, byte[] requestHash) {
        if (!record.matches(requestHash)) {
            metrics.recordConflict("IDEMPOTENCY");
            throw new RiskCaseException(ResultCode.RISK_CASE_IDEMPOTENCY_CONFLICT);
        }
        return record.riskCase().snapshot();
    }

    private void requireAuthorized(ActorContext actorContext) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(actorContext, RiskCaseCapabilities.CREATE);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(RiskCaseCapabilities.CREATE);
            throw exception;
        }
    }

    private record ParsedCreate(
            CaseIntakeSource intakeSource,
            TradingAccountRef subjectRef,
            String summary,
            RiskCasePriority priority,
            DecisionRef decisionRef) {
    }
}
