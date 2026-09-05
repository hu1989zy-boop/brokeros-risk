import type { CaseActionFieldName, CaseActionFieldOption } from '../actions/actionInputs';
import type { ReferenceBrowseScope } from '../api/referenceList';
import type { RiskCaseAssociations, RiskCaseHistoryEntry } from '../api/riskCaseTypes';

const associationEvents = new Set([
  'ATTACHED',
  'SUPERSEDED',
  'INVALIDATED',
  'WITHDRAWN',
  'DECISION_ASSOCIATED',
  'DECISION_SELECTED',
  'ACTION_ASSOCIATED',
  'OUTCOME_REFERENCED',
]);

export function onCaseReferenceOptions(
  associations: RiskCaseAssociations,
): Partial<Record<CaseActionFieldName, CaseActionFieldOption[]>> {
  return {
    associationEventRef: associations.evidenceAssociations.map((evidence) => ({
      label: `${evidence.eventRef} — ${evidence.evidenceRef} (${evidence.disposition})`,
      value: evidence.eventRef,
    })),
    decisionRef: associations.decisions.map((decision) => ({
      label: decision.current
        ? `${decision.decisionRef} (current)`
        : decision.decisionRef,
      value: decision.decisionRef,
    })),
    actionRef: associations.actions.map((action) => ({
      label: action.actionRef,
      value: action.actionRef,
    })),
  };
}

export function referenceBrowseScope(
  subjectRef: string,
  associations: RiskCaseAssociations,
): ReferenceBrowseScope {
  const decisionRef =
    associations.decisions.find((decision) => decision.current)?.decisionRef ??
    associations.decisions[0]?.decisionRef;
  return {
    subjectRef,
    ...(decisionRef ? { decisionRef } : {}),
    ...(associations.actions[0]?.actionRef
      ? { actionRef: associations.actions[0].actionRef }
      : {}),
  };
}

export function associationHistoryEntries(entries: RiskCaseHistoryEntry[]) {
  return entries.filter((entry) =>
    entry.affectedRef ? associationEvents.has(entry.eventType) : false,
  );
}
