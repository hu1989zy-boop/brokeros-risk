package com.brokeros.risk.riskcase.application.port;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.security.domain.ActorContext;

public interface ActionReferenceQuery {

    RecognizedAction requireRecognized(ActorContext actorContext, ActionRef actionRef);

    record RecognizedAction(ActionRef actionRef, DecisionRef decisionRef) {
    }
}
