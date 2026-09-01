package com.brokeros.risk.action.application.port;

import com.brokeros.risk.action.application.AuthorizedMutationContext;
import com.brokeros.risk.action.application.RecordActionSpec;
import com.brokeros.risk.action.domain.CompletedActionOperation;

public interface ActionMutationPort {

    CompletedActionOperation record(
            RecordActionSpec spec,
            AuthorizedMutationContext context);
}
