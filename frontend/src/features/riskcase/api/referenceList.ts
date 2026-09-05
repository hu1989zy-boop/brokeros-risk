import type { ApiClient } from '../../../core/api/apiClient';
import { asArray, asInstant, asRecord, asString } from '../../../core/api/contracts';

export interface ReferenceBrowseScope {
  subjectRef?: string;
  decisionRef?: string;
  actionRef?: string;
}

interface ReferenceListItemBase {
  reference: string;
  recordedAt: string;
}

export interface EvidenceReferenceListItem extends ReferenceListItemBase {
  kind: 'evidence';
  subjectRef: string;
  status: string;
}

export interface DecisionReferenceListItem extends ReferenceListItemBase {
  kind: 'decision';
  subjectRef: string;
}

export interface ActionReferenceListItem extends ReferenceListItemBase {
  kind: 'action';
  decisionRef: string;
  status: string;
}

export interface ActionOutcomeReferenceListItem extends ReferenceListItemBase {
  kind: 'actionOutcome';
  actionRef: string;
}

export type ReferenceListItem =
  | ActionOutcomeReferenceListItem
  | ActionReferenceListItem
  | DecisionReferenceListItem
  | EvidenceReferenceListItem;

export interface ReferenceListRepository {
  listEvidence(subjectRef: string): Promise<EvidenceReferenceListItem[]>;
  listDecisions(subjectRef: string): Promise<DecisionReferenceListItem[]>;
  listActions(decisionRef: string): Promise<ActionReferenceListItem[]>;
  listOutcomes(actionRef: string): Promise<ActionOutcomeReferenceListItem[]>;
}

export class HttpReferenceListRepository implements ReferenceListRepository {
  constructor(private readonly apiClient: ApiClient) {}

  listEvidence(subjectRef: string): Promise<EvidenceReferenceListItem[]> {
    return this.apiClient.get('/api/evidence', parseEvidenceList, {
      params: { subjectRef },
    });
  }

  listDecisions(subjectRef: string): Promise<DecisionReferenceListItem[]> {
    return this.apiClient.get('/api/decisions', parseDecisionList, {
      params: { subjectRef },
    });
  }

  listActions(decisionRef: string): Promise<ActionReferenceListItem[]> {
    return this.apiClient.get('/api/actions', parseActionList, {
      params: { decisionRef },
    });
  }

  listOutcomes(actionRef: string): Promise<ActionOutcomeReferenceListItem[]> {
    return this.apiClient.get('/api/action-outcomes', parseActionOutcomeList, {
      params: { actionRef },
    });
  }
}

export function parseEvidenceList(value: unknown): EvidenceReferenceListItem[] {
  return parseItems(value, 'EvidenceReferenceList', (item, index) => {
    const record = asRecord(item, `EvidenceReferenceList.items[${index}]`);
    return {
      kind: 'evidence',
      reference: asString(record.evidenceRef, 'EvidenceReferenceList.evidenceRef'),
      subjectRef: asString(record.subjectRef, 'EvidenceReferenceList.subjectRef'),
      status: asString(record.status, 'EvidenceReferenceList.status'),
      recordedAt: asInstant(record.recordedAt, 'EvidenceReferenceList.recordedAt'),
    };
  });
}

export function parseDecisionList(value: unknown): DecisionReferenceListItem[] {
  return parseItems(value, 'DecisionReferenceList', (item, index) => {
    const record = asRecord(item, `DecisionReferenceList.items[${index}]`);
    return {
      kind: 'decision',
      reference: asString(record.decisionRef, 'DecisionReferenceList.decisionRef'),
      subjectRef: asString(record.subjectRef, 'DecisionReferenceList.subjectRef'),
      recordedAt: asInstant(record.recordedAt, 'DecisionReferenceList.recordedAt'),
    };
  });
}

export function parseActionList(value: unknown): ActionReferenceListItem[] {
  return parseItems(value, 'ActionReferenceList', (item, index) => {
    const record = asRecord(item, `ActionReferenceList.items[${index}]`);
    return {
      kind: 'action',
      reference: asString(record.actionRef, 'ActionReferenceList.actionRef'),
      decisionRef: asString(record.decisionRef, 'ActionReferenceList.decisionRef'),
      status: asString(record.status, 'ActionReferenceList.status'),
      recordedAt: asInstant(record.recordedAt, 'ActionReferenceList.recordedAt'),
    };
  });
}

export function parseActionOutcomeList(value: unknown): ActionOutcomeReferenceListItem[] {
  return parseItems(value, 'ActionOutcomeReferenceList', (item, index) => {
    const record = asRecord(item, `ActionOutcomeReferenceList.items[${index}]`);
    return {
      kind: 'actionOutcome',
      reference: asString(
        record.actionOutcomeRef,
        'ActionOutcomeReferenceList.actionOutcomeRef',
      ),
      actionRef: asString(record.actionRef, 'ActionOutcomeReferenceList.actionRef'),
      recordedAt: asInstant(record.recordedAt, 'ActionOutcomeReferenceList.recordedAt'),
    };
  });
}

function parseItems<T>(
  value: unknown,
  label: string,
  parseItem: (item: unknown, index: number) => T,
): T[] {
  const record = asRecord(value, label);
  return asArray(record.items, `${label}.items`, parseItem);
}
