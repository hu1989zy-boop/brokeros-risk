package com.brokeros.risk.actionoutcome.application.port;

import com.brokeros.risk.actionoutcome.application.AuthorizedMutationContext;
import com.brokeros.risk.actionoutcome.application.RecordActionOutcomeSpec;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;

public interface ActionOutcomeMutationPort {

    CompletedActionOutcomeOperation record(
            RecordActionOutcomeSpec spec,
            AuthorizedMutationContext context);
}
