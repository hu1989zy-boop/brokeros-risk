package com.brokeros.risk.riskcase.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class RiskCaseQueryService {

    private final AuthorizationGuard authorizationGuard;
    private final RiskCaseRepository repository;
    private final AuditRecordWriter auditWriter;
    private final RiskCaseAuditFactory auditFactory;
    private final RiskCaseMetricsPort metrics;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public RiskCaseQueryService(
            AuthorizationGuard authorizationGuard,
            RiskCaseRepository repository,
            AuditRecordWriter auditWriter,
            RiskCaseAuditFactory auditFactory,
            RiskCaseMetricsPort metrics,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.repository = Objects.requireNonNull(repository);
        this.auditWriter = Objects.requireNonNull(auditWriter);
        this.auditFactory = Objects.requireNonNull(auditFactory);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public RiskCaseSnapshot detail(ActorContext actorContext, String rawCaseNumber) {
        long started = System.nanoTime();
        requireAuthorized(actorContext);
        try {
            RiskCaseSnapshot snapshot = transactionTemplate.execute(status -> {
                RiskCase riskCase = repository.findByCaseNumber(caseNumber(rawCaseNumber))
                        .orElseThrow(() -> new RiskCaseException(ResultCode.RISK_CASE_NOT_FOUND));
                RiskCaseSnapshot result = riskCase.snapshot();
                Instant occurredAt = clock.instant();
                auditWriter.append(auditFactory.read(
                        result, actorContext, occurredAt, "RISK_CASE_VIEWED"));
                return result;
            });
            if (snapshot == null) {
                throw new IllegalStateException("detail query returned no result");
            }
            metrics.recordSuccess(RiskCaseMetricOperation.READ);
            return snapshot;
        } finally {
            metrics.recordDuration(RiskCaseMetricOperation.READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    public RiskCaseHistoryPage history(
            ActorContext actorContext, String rawCaseNumber, String cursor, int limit) {
        long started = System.nanoTime();
        requireAuthorized(actorContext);
        if (limit < 1 || limit > 100) {
            throw new RiskCaseException(ResultCode.RISK_CASE_INVARIANT_VIOLATION);
        }
        RiskCaseHistoryCursor parsedCursor = decode(cursor);
        try {
            RiskCaseHistoryPage page = transactionTemplate.execute(status -> {
                RiskCase riskCase = repository.findByCaseNumber(caseNumber(rawCaseNumber))
                        .orElseThrow(() -> new RiskCaseException(ResultCode.RISK_CASE_NOT_FOUND));
                RiskCaseSnapshot snapshot = riskCase.snapshot();
                List<RiskCaseHistoryEntry> fetched = new ArrayList<>(
                        repository.findHistory(snapshot.id(), parsedCursor, limit + 1));
                String next = null;
                if (fetched.size() > limit) {
                    fetched.remove(fetched.size() - 1);
                    RiskCaseHistoryEntry last = fetched.get(fetched.size() - 1);
                    next = encode(new RiskCaseHistoryCursor(
                            last.caseVersion(), last.eventRank(), last.rowId()));
                }
                auditWriter.append(auditFactory.read(snapshot, actorContext, clock.instant(),
                        "RISK_CASE_HISTORY_VIEWED"));
                return new RiskCaseHistoryPage(fetched, next);
            });
            if (page == null) {
                throw new IllegalStateException("history query returned no result");
            }
            metrics.recordSuccess(RiskCaseMetricOperation.HISTORY_READ);
            return page;
        } finally {
            metrics.recordDuration(RiskCaseMetricOperation.HISTORY_READ,
                    Duration.ofNanos(System.nanoTime() - started));
        }
    }

    private void requireAuthorized(ActorContext actorContext) {
        Objects.requireNonNull(actorContext, "actorContext must not be null");
        try {
            authorizationGuard.requireAllowed(actorContext, RiskCaseCapabilities.READ);
        } catch (AuthorizationDeniedException exception) {
            metrics.recordAuthorizationDenied(RiskCaseCapabilities.READ);
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

    private RiskCaseHistoryCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new RiskCaseHistoryCursor(0, 0, 0);
        }
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII);
            String[] parts = decoded.split(":", -1);
            if (parts.length != 3) {
                throw new IllegalArgumentException("cursor shape is invalid");
            }
            return new RiskCaseHistoryCursor(
                    Long.parseLong(parts[0]), Integer.parseInt(parts[1]),
                    Long.parseLong(parts[2]));
        } catch (IllegalArgumentException exception) {
            throw new RiskCaseException(
                    ResultCode.RISK_CASE_INVARIANT_VIOLATION, exception);
        }
    }

    private String encode(RiskCaseHistoryCursor cursor) {
        String raw = cursor.caseVersion() + ":" + cursor.eventRank() + ":" + cursor.rowId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.US_ASCII));
    }
}
