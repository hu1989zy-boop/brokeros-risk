package com.brokeros.risk.tradingaccount.application;

import java.util.Objects;
import java.util.Optional;

import com.brokeros.risk.security.application.AuthorizationGuard;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.application.port.TradingAccountAuthorityQueryPort;
import com.brokeros.risk.tradingaccount.domain.AuthorityLifecycle;
import com.brokeros.risk.tradingaccount.domain.EligibilityDecision;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountReferenceEligibility;

public final class TradingAccountReferenceEligibilityService {
    private final AuthorizationGuard authorizationGuard;
    private final TradingAccountAuthorityQueryPort queryPort;
    private final AuthorityEvidenceFactory evidenceFactory;

    public TradingAccountReferenceEligibilityService(
            AuthorizationGuard authorizationGuard,
            TradingAccountAuthorityQueryPort queryPort,
            AuthorityEvidenceFactory evidenceFactory) {
        this.authorizationGuard = Objects.requireNonNull(authorizationGuard);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.evidenceFactory = Objects.requireNonNull(evidenceFactory);
    }

    public TradingAccountReferenceEligibility validateForNewRiskCaseAssociation(
            ActorContext actorContext,
            TradingAccountRef tradingAccountRef) {
        authorizationGuard.requireAllowed(actorContext, TradingAccountCapabilities.READ);
        Optional<EligibilityPersistenceView> stored = queryPort.findEligibility(tradingAccountRef);
        if (stored.isEmpty()) {
            return new TradingAccountReferenceEligibility(
                    tradingAccountRef, EligibilityDecision.NOT_RECOGNIZED, null, null);
        }
        EligibilityPersistenceView view = stored.orElseThrow();
        EligibilityDecision decision = view.accountLifecycle() == AuthorityLifecycle.ACTIVE
                        && view.scopeLifecycle() == AuthorityLifecycle.ACTIVE
                ? EligibilityDecision.ELIGIBLE_FOR_NEW_ASSOCIATION
                : EligibilityDecision.RECOGNIZED_NOT_ELIGIBLE;
        return new TradingAccountReferenceEligibility(
                tradingAccountRef,
                decision,
                evidenceFactory.snapshot(view),
                evidenceFactory.provenance(view));
    }
}
