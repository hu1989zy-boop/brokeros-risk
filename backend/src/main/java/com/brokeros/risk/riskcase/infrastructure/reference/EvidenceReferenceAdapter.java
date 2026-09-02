package com.brokeros.risk.riskcase.infrastructure.reference;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.evidence.application.EvidenceAuthorityUnavailableException;
import com.brokeros.risk.evidence.application.EvidenceProvenanceQueryService;
import com.brokeros.risk.evidence.domain.EvidenceProvenanceOutcome;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.application.RiskCaseException;
import com.brokeros.risk.riskcase.application.port.EvidenceReferenceQuery;
import com.brokeros.risk.security.domain.ActorContext;
import org.springframework.stereotype.Component;

@Component
public class EvidenceReferenceAdapter implements EvidenceReferenceQuery {

    private final EvidenceProvenanceQueryService service;

    public EvidenceReferenceAdapter(EvidenceProvenanceQueryService service) {
        this.service = service;
    }

    @Override
    public void requireRecognized(ActorContext actorContext, EvidenceRef evidenceRef) {
        try {
            if (service.confirmProvenance(actorContext, evidenceRef).outcome()
                    == EvidenceProvenanceOutcome.NOT_FOUND) {
                throw new RiskCaseException(ResultCode.RISK_CASE_REFERENCE_NOT_FOUND);
            }
        } catch (EvidenceAuthorityUnavailableException exception) {
            throw new RiskCaseException(
                    ResultCode.RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE, exception);
        }
    }
}
