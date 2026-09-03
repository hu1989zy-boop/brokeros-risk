import type { CaseActionFieldName, CaseActionFieldOption } from '../actions/actionInputs';
import type { RiskCaseHistoryEntry, RiskCaseView } from '../api/riskCaseTypes';

const evidenceEvents = new Set(['ATTACHED', 'SUPERSEDED', 'INVALIDATED']);
const associationEvents = new Set([
  ...evidenceEvents,
  'WITHDRAWN',
  'DECISION_ASSOCIATED',
  'DECISION_SELECTED',
  'ACTION_ASSOCIATED',
  'OUTCOME_REFERENCED',
]);

export interface EvidenceAssociationProjection {
  evidenceRef: string;
  latestEventType: string;
  version: number;
}

export interface ActionAssociationProjection {
  actionRef: string;
  outcomeRecorded: boolean;
  active: boolean;
}

export interface AssociationProjection {
  evidence: EvidenceAssociationProjection[];
  decisions: string[];
  actions: ActionAssociationProjection[];
}

export function projectAssociations(view: RiskCaseView): AssociationProjection {
  const evidence = new Map<string, EvidenceAssociationProjection>();
  const decisions = new Set<string>();
  const actions = new Map<string, ActionAssociationProjection>();

  for (const entry of view.history.entries) {
    if (!entry.affectedRef) continue;
    if (entry.eventType === 'WITHDRAWN' && actions.has(entry.affectedRef)) {
      const current = actions.get(entry.affectedRef)!;
      actions.set(entry.affectedRef, { ...current, active: false });
      continue;
    }
    if (evidenceEvents.has(entry.eventType)) {
      evidence.set(entry.affectedRef, {
        evidenceRef: entry.affectedRef,
        latestEventType: entry.eventType,
        version: entry.version,
      });
    }
    if (entry.eventType === 'DECISION_ASSOCIATED') {
      decisions.add(entry.affectedRef);
    }
    if (entry.eventType === 'ACTION_ASSOCIATED') {
      actions.set(entry.affectedRef, {
        actionRef: entry.affectedRef,
        outcomeRecorded: false,
        active: true,
      });
    }
    if (entry.eventType === 'OUTCOME_REFERENCED') {
      const current = actions.get(entry.affectedRef);
      actions.set(entry.affectedRef, {
        actionRef: entry.affectedRef,
        outcomeRecorded: true,
        active: current?.active ?? true,
      });
    }
    if (entry.eventType === 'WITHDRAWN') {
      evidence.set(entry.affectedRef, {
        evidenceRef: entry.affectedRef,
        latestEventType: entry.eventType,
        version: entry.version,
      });
    }
  }
  if (view.detail.currentDecisionRef) decisions.add(view.detail.currentDecisionRef);
  return {
    evidence: [...evidence.values()],
    decisions: [...decisions],
    actions: [...actions.values()],
  };
}

export function onCaseReferenceOptions(
  view: RiskCaseView,
): Partial<Record<CaseActionFieldName, CaseActionFieldOption[]>> {
  const projection = projectAssociations(view);
  return {
    decisionRef: projection.decisions.map((reference) => ({
      label:
        reference === view.detail.currentDecisionRef
          ? `${reference} (current)`
          : reference,
      value: reference,
    })),
    actionRef: projection.actions
      .filter((action) => action.active)
      .map((action) => ({ label: action.actionRef, value: action.actionRef })),
  };
}

export function associationHistoryEntries(entries: RiskCaseHistoryEntry[]) {
  return entries.filter((entry) =>
    entry.affectedRef ? associationEvents.has(entry.eventType) : false,
  );
}
