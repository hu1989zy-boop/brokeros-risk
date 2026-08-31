package com.brokeros.risk.decision.application.port;

import com.brokeros.risk.decision.application.AuthorizedMutationContext;
import com.brokeros.risk.decision.application.CompletedDecisionOperation;
import com.brokeros.risk.decision.application.RecordDecisionSpec;

public interface DecisionMutationPort {

    CompletedDecisionOperation record(
            RecordDecisionSpec spec,
            AuthorizedMutationContext context);
}
