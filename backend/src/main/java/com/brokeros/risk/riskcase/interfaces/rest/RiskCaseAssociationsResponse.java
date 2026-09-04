package com.brokeros.risk.riskcase.interfaces.rest;

import java.time.Instant;
import java.util.List;

import com.brokeros.risk.riskcase.application.RiskCaseAssociations;

public record RiskCaseAssociationsResponse(
        String caseNumber,
        long version,
        List<EvidenceAssociation> evidenceAssociations,
        List<DecisionAssociation> decisions,
        List<ActionAssociation> actions) {

    public static RiskCaseAssociationsResponse from(RiskCaseAssociations associations) {
        return new RiskCaseAssociationsResponse(
                associations.caseNumber().value(), associations.version(),
                associations.evidenceAssociations().stream()
                        .map(evidence -> new EvidenceAssociation(
                                evidence.eventRef().value(), evidence.evidenceRef().value(),
                                evidence.disposition().name(), evidence.source(),
                                evidence.replacementEvidenceRef() == null
                                        ? null
                                        : evidence.replacementEvidenceRef().value(),
                                evidence.occurredAt()))
                        .toList(),
                associations.decisions().stream()
                        .map(decision -> new DecisionAssociation(
                                decision.decisionRef().value(), decision.current()))
                        .toList(),
                associations.actions().stream()
                        .map(action -> new ActionAssociation(
                                action.actionRef().value(),
                                action.outcomeRefs().stream().map(ref -> ref.value()).toList()))
                        .toList());
    }

    public record EvidenceAssociation(
            String eventRef,
            String evidenceRef,
            String disposition,
            String source,
            String replacementEvidenceRef,
            Instant occurredAt) {
    }

    public record DecisionAssociation(String decisionRef, boolean current) {
    }

    public record ActionAssociation(String actionRef, List<String> outcomeRefs) {
    }
}
