import type { ApiClient } from '../../../core/api/apiClient';
import { asInstant, asRecord, asString } from '../../../core/api/contracts';
import type { ReferenceKind } from '../actions/actionInputs';

interface PreviewBase {
  kind: ReferenceKind;
  reference: string;
  source: string;
  recordedAt: string;
}

export interface EvidencePreview extends PreviewBase {
  kind: 'evidence';
  subjectRef: string;
  status: string;
}

export interface DecisionPreview extends PreviewBase {
  kind: 'decision';
  subjectRef: string;
}

export interface ActionPreview extends PreviewBase {
  kind: 'action';
  decisionRef: string;
  status: string;
}

export interface ActionOutcomePreview extends PreviewBase {
  kind: 'actionOutcome';
  actionRef: string;
}

export type ReferencePreview =
  | ActionOutcomePreview
  | ActionPreview
  | DecisionPreview
  | EvidencePreview;

export interface ReferencePreviewRepository {
  get(kind: ReferenceKind, reference: string): Promise<ReferencePreview>;
}

export class HttpReferencePreviewRepository implements ReferencePreviewRepository {
  constructor(private readonly apiClient: ApiClient) {}

  get(kind: ReferenceKind, reference: string): Promise<ReferencePreview> {
    const encoded = encodeURIComponent(reference);
    switch (kind) {
      case 'evidence':
        return this.apiClient.get(`/api/evidence/${encoded}`, parseEvidencePreview);
      case 'decision':
        return this.apiClient.get(`/api/decisions/${encoded}`, parseDecisionPreview);
      case 'action':
        return this.apiClient.get(`/api/actions/${encoded}`, parseActionPreview);
      case 'actionOutcome':
        return this.apiClient.get(
          `/api/action-outcomes/${encoded}`,
          parseActionOutcomePreview,
        );
    }
  }
}

export function parseEvidencePreview(value: unknown): EvidencePreview {
  const record = asRecord(value, 'EvidenceDetail');
  return {
    kind: 'evidence',
    reference: asString(record.evidenceRef, 'EvidenceDetail.evidenceRef'),
    subjectRef: asString(record.subjectRef, 'EvidenceDetail.subjectRef'),
    source: asString(record.source, 'EvidenceDetail.source'),
    status: asString(record.status, 'EvidenceDetail.status'),
    recordedAt: asInstant(record.recordedAt, 'EvidenceDetail.recordedAt'),
  };
}

export function parseDecisionPreview(value: unknown): DecisionPreview {
  const record = asRecord(value, 'DecisionDetail');
  return {
    kind: 'decision',
    reference: asString(record.decisionRef, 'DecisionDetail.decisionRef'),
    subjectRef: asString(record.subjectRef, 'DecisionDetail.subjectRef'),
    source: asString(record.source, 'DecisionDetail.source'),
    recordedAt: asInstant(record.recordedAt, 'DecisionDetail.recordedAt'),
  };
}

export function parseActionPreview(value: unknown): ActionPreview {
  const record = asRecord(value, 'ActionDetail');
  return {
    kind: 'action',
    reference: asString(record.actionRef, 'ActionDetail.actionRef'),
    decisionRef: asString(record.decisionRef, 'ActionDetail.decisionRef'),
    source: asString(record.source, 'ActionDetail.source'),
    status: asString(record.status, 'ActionDetail.status'),
    recordedAt: asInstant(record.recordedAt, 'ActionDetail.recordedAt'),
  };
}

export function parseActionOutcomePreview(value: unknown): ActionOutcomePreview {
  const record = asRecord(value, 'ActionOutcomeDetail');
  return {
    kind: 'actionOutcome',
    reference: asString(record.actionOutcomeRef, 'ActionOutcomeDetail.actionOutcomeRef'),
    actionRef: asString(record.actionRef, 'ActionOutcomeDetail.actionRef'),
    source: asString(record.source, 'ActionOutcomeDetail.source'),
    recordedAt: asInstant(record.recordedAt, 'ActionOutcomeDetail.recordedAt'),
  };
}
