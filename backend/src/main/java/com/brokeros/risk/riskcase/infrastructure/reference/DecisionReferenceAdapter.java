package com.brokeros.risk.riskcase.infrastructure.reference;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.decision.application.DecisionAuthorityUnavailableException;
import com.brokeros.risk.decision.application.DecisionProvenanceQueryService;
import com.brokeros.risk.decision.domain.DecisionProvenanceOutcome;
import com.brokeros.risk.decision.domain.DecisionProvenanceView;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.riskcase.application.RiskCaseException;
import com.brokeros.risk.riskcase.application.port.DecisionReferenceQuery;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.stereotype.Component;

@Component
public class DecisionReferenceAdapter implements DecisionReferenceQuery {

    private final DecisionProvenanceQueryService service;

    public DecisionReferenceAdapter(DecisionProvenanceQueryService service) {
        this.service = service;
    }

    @Override
    public RecognizedDecision requireRecognized(
            ActorContext actorContext, DecisionRef decisionRef) {
        try {
            DecisionProvenanceView view = service.confirmProvenance(actorContext, decisionRef);
            if (view.outcome() == DecisionProvenanceOutcome.NOT_FOUND) {
                throw new RiskCaseException(ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
            }
            return new RecognizedDecision(view.decisionRef(), view.evidenceRefs());
        } catch (DecisionAuthorityUnavailableException exception) {
            throw new RiskCaseException(
                    ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE, exception);
        }
    }
}
