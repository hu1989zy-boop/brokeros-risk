package com.brokeros.risk.decision.application.port;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.DecisionReferenceSummary;
import com.brokeros.risk.decision.domain.DecisionOperationId;
import com.brokeros.risk.decision.domain.DecisionRecord;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;

public interface DecisionQueryPort {

    Optional<CompletedDecisionOperation> findOperation(DecisionOperationId id);

    Optional<DecisionRecord> findByRef(DecisionRef ref);

    List<DecisionReferenceSummary> findSummariesBySubject(
            TradingAccountRef subjectRef,
            int limit);
}
