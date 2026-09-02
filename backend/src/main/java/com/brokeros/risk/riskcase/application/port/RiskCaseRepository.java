package com.brokeros.risk.riskcase.application.port;

import java.util.List;
import java.util.Optional;

import com.brokeros.risk.action.domain.ActionRef;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRef;
import com.brokeros.risk.decision.domain.DecisionRef;
import com.brokeros.risk.evidence.domain.EvidenceRef;
import com.brokeros.risk.riskcase.application.RiskCaseCreationRecord;
import com.brokeros.risk.riskcase.application.RiskCaseHistoryCursor;
import com.brokeros.risk.riskcase.application.RiskCaseHistoryEntry;
import com.brokeros.risk.riskcase.application.RiskCaseListQuery;
import com.brokeros.risk.riskcase.application.RiskCaseSummary;
import com.brokeros.risk.riskcase.domain.ActionAssociationEvent;
import com.brokeros.risk.riskcase.domain.AssignmentChangeRecord;
import com.brokeros.risk.riskcase.domain.CaseNumber;
import com.brokeros.risk.riskcase.domain.DecisionAssociation;
import com.brokeros.risk.riskcase.domain.DecisionSelectionRecord;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEvent;
import com.brokeros.risk.riskcase.domain.EvidenceAssociationEventRef;
import com.brokeros.risk.riskcase.domain.InvestigationNote;
import com.brokeros.risk.riskcase.domain.InvestigationNoteRef;
import com.brokeros.risk.riskcase.domain.PriorityChangeRecord;
import com.brokeros.risk.riskcase.domain.ResolutionRecord;
import com.brokeros.risk.riskcase.domain.RiskCase;
import com.brokeros.risk.riskcase.domain.RiskCaseId;
import com.brokeros.risk.riskcase.domain.RiskCaseSnapshot;
import com.brokeros.risk.riskcase.domain.TransitionRecord;
import com.brokeros.risk.security.domain.ActorRef;

public interface RiskCaseRepository {

    Optional<RiskCase> findByCaseNumber(CaseNumber caseNumber);

    Optional<RiskCaseCreationRecord> findByCreationKey(
            ActorRef actorRef, byte[] idempotencyKeyHash);

    Optional<RiskCase> findByPrimaryDecision(DecisionRef decisionRef);

    RiskCase insertRoot(RiskCase riskCase, byte[] keyHash, byte[] requestHash);

    int updateRoot(RiskCaseSnapshot snapshot, long expectedVersion);

    void appendTransition(TransitionRecord record);

    void appendAssignment(AssignmentChangeRecord record);

    void appendPriority(PriorityChangeRecord record);

    EvidenceAssociationEvent appendEvidence(EvidenceAssociationEvent event);

    Optional<EvidenceAssociationEvent> findEvidenceEvent(
            RiskCaseId caseId, EvidenceAssociationEventRef eventRef);

    boolean evidenceEventHasDisposition(long eventId);

    Optional<EffectiveEvidence> findEffectiveEvidence(
            RiskCaseId caseId, EvidenceRef evidenceRef);

    List<EffectiveEvidence> findAllEffectiveEvidence(RiskCaseId caseId);

    void appendDecisionAssociation(DecisionAssociation association);

    void appendDecisionSelection(DecisionSelectionRecord record);

    boolean isDecisionAssociated(RiskCaseId caseId, DecisionRef decisionRef);

    ActionAssociationEvent appendAction(ActionAssociationEvent event);

    Optional<EffectiveAction> findEffectiveAction(RiskCaseId caseId, ActionRef actionRef);

    List<EffectiveAction> findAllEffectiveActions(RiskCaseId caseId);

    boolean hasActionForDecision(RiskCaseId caseId, DecisionRef decisionRef);

    InvestigationNote appendNote(InvestigationNote note);

    Optional<InvestigationNote> findNote(RiskCaseId caseId, InvestigationNoteRef noteRef);

    boolean noteHasCorrection(long noteId);

    ResolutionRecord appendResolution(ResolutionRecord resolution);

    void appendResolutionEvidence(long resolutionId, EffectiveEvidence evidence);

    void appendResolutionAction(long resolutionId, EffectiveAction action);

    List<RiskCaseHistoryEntry> findHistory(
            RiskCaseId caseId, RiskCaseHistoryCursor cursor, int limit);

    List<RiskCaseSummary> findSummaries(
            RiskCaseListQuery query, int limit, long offset);

    record EffectiveEvidence(long eventId, EvidenceRef evidenceRef) {
    }

    record EffectiveAction(
            long eventId,
            ActionRef actionRef,
            DecisionRef decisionRef,
            ActionOutcomeRef outcomeRef) {
    }
}
