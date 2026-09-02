package com.brokeros.risk.riskcase.infrastructure.reference;

import com.brokeros.risk.actionoutcome.application.ActionOutcomeAuthorityUnavailableException;
import com.brokeros.risk.actionoutcome.application.ActionOutcomeProvenanceQueryService;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeProvenanceOutcome;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeProvenanceView;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.riskcase.application.RiskCaseException;
import com.brokeros.risk.riskcase.application.port.ActionOutcomeReferenceQuery;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.stereotype.Component;

@Component
public class ActionOutcomeReferenceAdapter implements ActionOutcomeReferenceQuery {

    private final ActionOutcomeProvenanceQueryService service;

    public ActionOutcomeReferenceAdapter(ActionOutcomeProvenanceQueryService service) {
        this.service = service;
    }

    @Override
    public RecognizedActionOutcome requireRecognized(
            ActorContext actorContext, ActionOutcomeRef actionOutcomeRef) {
        try {
            ActionOutcomeProvenanceView view =
                    service.confirmProvenance(actorContext, actionOutcomeRef);
            if (view.outcome() == ActionOutcomeProvenanceOutcome.NOT_FOUND) {
                throw new RiskCaseException(ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
            }
            return new RecognizedActionOutcome(
                    view.actionOutcomeRef(), view.actionRef());
        } catch (ActionOutcomeAuthorityUnavailableException exception) {
            throw new RiskCaseException(
                    ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE, exception);
        }
    }
}
