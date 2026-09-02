package com.brokeros.risk.riskcase.application.port;

import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public interface TradingAccountReferenceQuery {

    void requireEligibleForNewCase(ActorContext actorContext, TradingAccountRef subjectRef);
}
