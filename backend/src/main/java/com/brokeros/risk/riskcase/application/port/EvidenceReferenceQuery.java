package com.brokeros.risk.riskcase.application.port;

import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.security.domain.ActorContext;

public interface EvidenceReferenceQuery {

    void requireRecognized(ActorContext actorContext, EvidenceRef evidenceRef);
}
