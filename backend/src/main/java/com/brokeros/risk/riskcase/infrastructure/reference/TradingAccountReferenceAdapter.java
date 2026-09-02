package com.brokeros.risk.riskcase.infrastructure.reference;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.riskcase.application.RiskCaseException;
import com.brokeros.risk.riskcase.application.port.TradingAccountReferenceQuery;
import com.brokeros.risk.security.domain.ActorContext;
import com.brokeros.risk.tradingaccount.application.TradingAccountAuthorityUnavailableException;
import com.brokeros.risk.tradingaccount.application.TradingAccountReferenceEligibilityService;
import com.brokeros.risk.tradingaccount.domain.EligibilityDecision;
import com.brokeros.risk.tradingaccount.domain.TradingAccountRef;
import org.springframework.stereotype.Component;

@Component
public class TradingAccountReferenceAdapter implements TradingAccountReferenceQuery {

    private final TradingAccountReferenceEligibilityService service;

    public TradingAccountReferenceAdapter(TradingAccountReferenceEligibilityService service) {
        this.service = service;
    }

    @Override
    public void requireEligibleForNewCase(
            ActorContext actorContext, TradingAccountRef subjectRef) {
        try {
            EligibilityDecision decision = service
                    .validateForNewRiskCaseAssociation(actorContext, subjectRef)
                    .decision();
            if (decision == EligibilityDecision.NOT_RECOGNIZED) {
                throw new RiskCaseException(ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
            }
            if (decision == EligibilityDecision.RECOGNIZED_NOT_ELIGIBLE) {
                throw new RiskCaseException(ResultCode.RISK_CASE_SUBJECT_NOT_ELIGIBLE);
            }
        } catch (TradingAccountAuthorityUnavailableException exception) {
            throw new RiskCaseException(
                    ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE, exception);
        }
    }
}
