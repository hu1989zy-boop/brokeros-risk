package com.brokeros.risk.actionoutcome.application.port;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeReferenceSummary;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeOperationId;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.actionoutcome.domain.CompletedActionOutcomeOperation;

public interface ActionOutcomeQueryPort {

    Optional<CompletedActionOutcomeOperation> findOperation(
            ActionOutcomeOperationId id);

    Optional<ActionOutcomeRecord> findByRef(ActionOutcomeRef ref);

    List<ActionOutcomeReferenceSummary> findSummariesByAction(
            ActionRef actionRef,
            int limit);
}
