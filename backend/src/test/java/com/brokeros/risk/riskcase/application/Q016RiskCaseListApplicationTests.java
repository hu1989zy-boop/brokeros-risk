package com.brokeros.risk.riskcase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.riskcase.application.port.RiskCaseMetricsPort;
import com.brokeros.risk.riskcase.application.port.RiskCaseRepository;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.RiskCasePriority;
import com.brokeros.risk.riskcase.domain.RiskCaseStatus;
import com.brokeros.risk.security.application.AuthorizationDeniedException;
import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.security.domain.ActorRef;
import com.brokeros.risk.security.domain.ActorType;
import com.brokeros.risk.security.domain.AuthenticationMethod;
import com.brokeros.risk.security.domain.ExternalPrincipalKey;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;

class Q016RiskCaseListApplicationTests {

    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final ActorRef ACTOR =
            new ActorRef("16000000-0000-4000-8000-000000000001");

    private AuthorizationGuard authorizationGuard;
    private RiskCaseRepository repository;
    private RiskCaseMetricsPort metrics;
    private ActorContext actorContext;

    @BeforeEach
    void setUp() {
        authorizationGuard = mock(AuthorizationGuard.class);
        repository = mock(RiskCaseRepository.class);
        metrics = mock(RiskCaseMetricsPort.class);
        actorContext = actorContext();
    }

    @Test
    void authorizationPrecedesBoundedFilteredRepositoryQuery() {
        when(repository.findSummaries(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(IntStream.rangeClosed(1, 101)
                        .mapToObj(this::summary)
                        .toList());
        RiskCaseQueryService service = service();

        RiskCasePage<RiskCaseSummary> result = service.listCases(
                actorContext, "OPEN", "HIGH", subject(1), ACTOR.value(), 2, 250);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(100);
        assertThat(result.items()).hasSize(100);
        assertThat(result.hasNext()).isTrue();

        ArgumentCaptor<RiskCaseListQuery> query =
                ArgumentCaptor.forClass(RiskCaseListQuery.class);
        InOrder order = inOrder(authorizationGuard, repository);
        order.verify(authorizationGuard).requireAllowed(actorContext, RiskCaseCapabilities.READ);
        order.verify(repository).findSummaries(query.capture(),
                org.mockito.ArgumentMatchers.eq(101),
                org.mockito.ArgumentMatchers.eq(200L));
        assertThat(query.getValue()).isEqualTo(new RiskCaseListQuery(
                RiskCaseStatus.OPEN, RiskCasePriority.HIGH,
                new TradingAccountRef(subject(1)), ACTOR));
    }

    @Test
    void authorizationDenialPreventsListRepositoryAccess() {
        doThrow(new AuthorizationDeniedException()).when(authorizationGuard)
                .requireAllowed(actorContext, RiskCaseCapabilities.READ);

        assertThatThrownBy(() -> service().listCases(
                actorContext, null, null, null, null, 0, 20))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(repository, never()).findSummaries(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void invalidPagingNeverReachesRepository() {
        assertThatThrownBy(() -> service().listCases(
                actorContext, null, null, null, null, -1, 20))
                .isInstanceOf(RiskCaseException.class);
        assertThatThrownBy(() -> service().listCases(
                actorContext, null, null, null, null, 0, 0))
                .isInstanceOf(RiskCaseException.class);

        verify(repository, never()).findSummaries(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    private RiskCaseQueryService service() {
        return new RiskCaseQueryService(
                authorizationGuard, repository, mock(AuditRecordWriter.class),
                new RiskCaseAuditFactory(), metrics,
                Clock.fixed(NOW, ZoneOffset.UTC), mock(PlatformTransactionManager.class));
    }

    private RiskCaseSummary summary(int value) {
        return new RiskCaseSummary(
                new CaseNumber("RC-16000000-0000-4000-8000-"
                        + String.format("%012d", value)),
                new TradingAccountRef(subject(value)), RiskCaseStatus.OPEN,
                RiskCasePriority.HIGH, ACTOR, NOW, NOW, 1);
    }

    private ActorContext actorContext() {
        return new ActorContext(
                ACTOR, ActorType.HUMAN,
                new ExternalPrincipalKey(
                        "urn:brokeros:risk:q016-test", "operator", ActorType.HUMAN),
                AuthenticationMethod.TRUSTED_IN_PROCESS, NOW, null,
                UUID.fromString("16000000-0000-4000-8000-000000000099"),
                "q016-request", "16000000000000000000000000000001");
    }

    private String subject(int value) {
        return "ta-26000000-0000-4000-8000-" + String.format("%012d", value);
    }
}
