package com.brokeros.risk.riskcase.application.port;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.security.domain.ActorContext;

public interface ActionOutcomeReferenceQuery {

    RecognizedActionOutcome requireRecognized(
            ActorContext actorContext, ActionOutcomeRef actionOutcomeRef);

    record RecognizedActionOutcome(
            ActionOutcomeRef actionOutcomeRef,
            ActionRef actionRef) {
    }
}
