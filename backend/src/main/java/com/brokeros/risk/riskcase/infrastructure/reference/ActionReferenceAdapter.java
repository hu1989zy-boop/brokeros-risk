package com.brokeros.risk.riskcase.infrastructure.reference;

import com.brokeros.risk.action.application.ActionAuthorityUnavailableException;
import com.brokeros.risk.action.application.ActionProvenanceQueryService;
import com.brokeros.risk.action.domain.ActionProvenanceOutcome;
import com.brokeros.risk.action.domain.ActionProvenanceView;
import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.riskcase.application.RiskCaseException;
import com.brokeros.risk.riskcase.application.port.ActionReferenceQuery;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.stereotype.Component;

@Component
public class ActionReferenceAdapter implements ActionReferenceQuery {

    private final ActionProvenanceQueryService service;

    public ActionReferenceAdapter(ActionProvenanceQueryService service) {
        this.service = service;
    }

    @Override
    public RecognizedAction requireRecognized(
            ActorContext actorContext, ActionRef actionRef) {
        try {
            ActionProvenanceView view = service.confirmProvenance(actorContext, actionRef);
            if (view.outcome() == ActionProvenanceOutcome.NOT_FOUND) {
                throw new RiskCaseException(ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
            }
            return new RecognizedAction(view.actionRef(), view.decisionRef());
        } catch (ActionAuthorityUnavailableException exception) {
            throw new RiskCaseException(
                    ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE, exception);
        }
    }
}
