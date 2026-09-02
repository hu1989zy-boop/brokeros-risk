package com.brokeros.risk.riskcase.application.port;

import java.util.Set;

import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorContext;

public interface DecisionReferenceQuery {

    RecognizedDecision requireRecognized(ActorContext actorContext, DecisionRef decisionRef);

    record RecognizedDecision(DecisionRef decisionRef, Set<EvidenceRef> evidenceRefs) {
    }
}
