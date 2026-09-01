package com.brokeros.risk.action.application.port;

import java.util.Optional;

import com.brokeros.risk.action.domain.ActionOperationId;
import com.brokeros.risk.action.domain.ActionRecord;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.action.domain.CompletedActionOperation;

public interface ActionQueryPort {

    Optional<CompletedActionOperation> findOperation(ActionOperationId id);

    Optional<ActionRecord> findByRef(ActionRef ref);
}
