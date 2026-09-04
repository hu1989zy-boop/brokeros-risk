package com.brokeros.risk.riskcase.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEventRef;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEventType;

public record RiskCaseAssociations(
        CaseNumber caseNumber,
        long version,
        List<EvidenceAssociation> evidenceAssociations,
        List<DecisionAssociation> decisions,
        List<ActionAssociation> actions) {

    public RiskCaseAssociations {
        Objects.requireNonNull(caseNumber, "caseNumber must not be null");
        evidenceAssociations = List.copyOf(evidenceAssociations);
        decisions = List.copyOf(decisions);
        actions = List.copyOf(actions);
    }

    public record EvidenceAssociation(
            EvidenceAssociationEventRef eventRef,
            EvidenceRef evidenceRef,
            EvidenceAssociationEventType disposition,
            String source,
            EvidenceRef replacementEvidenceRef,
            Instant occurredAt) {

        public EvidenceAssociation {
            Objects.requireNonNull(eventRef, "eventRef must not be null");
            Objects.requireNonNull(evidenceRef, "evidenceRef must not be null");
            Objects.requireNonNull(disposition, "disposition must not be null");
            Objects.requireNonNull(source, "source must not be null");
            Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        }
    }

    public record DecisionAssociation(DecisionRef decisionRef, boolean current) {

        public DecisionAssociation {
            Objects.requireNonNull(decisionRef, "decisionRef must not be null");
        }
    }

    public record ActionAssociation(ActionRef actionRef, List<ActionOutcomeRef> outcomeRefs) {

        public ActionAssociation {
            Objects.requireNonNull(actionRef, "actionRef must not be null");
            outcomeRefs = List.copyOf(outcomeRefs);
        }
    }
}
