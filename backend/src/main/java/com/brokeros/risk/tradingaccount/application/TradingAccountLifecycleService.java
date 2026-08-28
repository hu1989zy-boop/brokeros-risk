package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMutationPort;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityMetricsPort;

public final class TradingAccountLifecycleService {
    private final AuthorizedMutationFactory mutationFactory;
    private final TradingAccountAuthorityMutationPort mutationPort;
    private final TradingAccountAuthorityMetricsPort metrics;

    public TradingAccountLifecycleService(
            AuthorizedMutationFactory mutationFactory,
            TradingAccountAuthorityMutationPort mutationPort,
            TradingAccountAuthorityMetricsPort metrics) {
        this.mutationFactory = Objects.requireNonNull(mutationFactory);
        this.mutationPort = Objects.requireNonNull(mutationPort);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public LifecycleChangeResult change(ActorContext actorContext, AuthorityOperationRequest request) {
        AuthorizedMutationContext context = mutationFactory.authorize(
                actorContext, TradingAccountCapabilities.CHANGE_LIFECYCLE, request);
        long started = System.nanoTime();
        try {
            LifecycleChangeResult result = mutationPort.changeAccountLifecycle(
                    new ChangeAccountLifecycleSpec(request), context);
            metrics.recordOperation(request.operationType(), result.outcome());
            return result;
        } finally {
            metrics.recordDuration(request.operationType(), java.time.Duration.ofNanos(System.nanoTime() - started));
        }
    }
}
