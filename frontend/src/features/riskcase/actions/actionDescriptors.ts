import type { ResultCode } from '../../../core/api/contracts';
import type { RiskCaseRepository } from '../api/riskCaseRepository';
import {
  riskCaseStatuses,
  type RiskCaseDetail,
  type EvidenceDisposition,
  type RiskCaseActionAssociation,
  type RiskCaseDecisionAssociation,
  type RiskCaseEvidenceAssociation,
  type RiskCaseNote,
  type RiskCasePriority,
  type RiskCaseResolution,
  type RiskCaseResolutionOutcome,
  type RiskCaseStatus,
} from '../api/riskCaseTypes';
import {
  actionRefPattern,
  actionOutcomeRefPattern,
  associationActionRefPattern,
  canonicalUuidV4Pattern,
  caseNumberPattern,
  decisionRefPattern,
  evidenceRefPattern,
  priorityOptions,
  resolutionOutcomeOptions,
  splitReferenceList,
  type CaseActionFieldSpec,
  type CaseActionValues,
} from './actionInputs';

export const caseActionIds = [
  'assign',
  'changePriority',
  'beginReview',
  'markActionRequired',
  'returnToReview',
  'resolve',
  'close',
  'cancel',
  'resume',
  'reopen',
  'correctNote',
] as const;

export const associationActionIds = [
  'associateEvidence',
  'changeEvidenceDisposition',
  'associateDecision',
  'selectDecision',
  'associateAction',
  'referenceActionOutcome',
] as const;

export type CaseActionId =
  | (typeof associationActionIds)[number]
  | (typeof caseActionIds)[number];
export type CaseActionResult =
  | RiskCaseActionAssociation
  | RiskCaseDecisionAssociation
  | RiskCaseDetail
  | RiskCaseEvidenceAssociation
  | RiskCaseNote
  | RiskCaseResolution;

export interface CaseActionExecutionContext {
  caseNumber: string;
  expectedVersion: number;
  noteRef?: string;
}

export interface CaseActionDescriptor {
  id: CaseActionId;
  label: string;
  method: 'POST';
  path: (
    context: Pick<CaseActionExecutionContext, 'caseNumber' | 'noteRef'>,
    values?: CaseActionValues,
  ) => string;
  fields: CaseActionFieldSpec[];
  allowedFrom: readonly RiskCaseStatus[];
  terminal: boolean;
  confirmation?: string;
  messages?: Partial<Record<ResultCode, string>>;
  execute: (
    repository: RiskCaseRepository,
    context: CaseActionExecutionContext,
    values: CaseActionValues,
  ) => Promise<CaseActionResult>;
}

const reasonField: CaseActionFieldSpec = {
  name: 'reason',
  label: 'Reason',
  kind: 'textarea',
  required: true,
  maxLength: 1000,
  placeholder: 'Explain why this operation is required',
};

const assigneeField = (required: boolean): CaseActionFieldSpec => ({
  name: 'assigneeRef',
  label: required ? 'Assignee reference' : 'Assignee reference (optional)',
  kind: 'text',
  required,
  maxLength: 128,
  pattern: canonicalUuidV4Pattern,
  patternMessage: 'Enter a canonical lowercase UUIDv4 actor reference.',
});

const commonMessages: CaseActionDescriptor['messages'] = {
  AUTHORIZATION_DENIED: 'You are not authorized to perform this case operation.',
  RISK_CASE_INVALID_TRANSITION: 'The case is no longer in a state that allows this operation.',
  RISK_CASE_INVARIANT_VIOLATION: 'The operation does not satisfy the current case requirements.',
  RISK_CASE_REFERENCE_NOT_FOUND: 'The selected reference is not available on this case.',
  RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE: 'The reference provider is temporarily unavailable.',
};

const associationStatuses = riskCaseStatuses;

const sourceField: CaseActionFieldSpec = {
  name: 'source',
  label: 'Association source',
  kind: 'text',
  required: true,
  maxLength: 64,
  placeholder: 'Operator-provided provenance label',
};

function referenceField(
  name: 'actionRef' | 'decisionRef' | 'evidenceRef' | 'outcomeRef' | 'replacementEvidenceRef',
  label: string,
  referenceKind: NonNullable<CaseActionFieldSpec['referenceKind']>,
  required: boolean,
  pattern: RegExp,
): CaseActionFieldSpec {
  return {
    name,
    label,
    kind: 'reference',
    required,
    maxLength: 128,
    referenceKind,
    pattern,
    patternMessage: 'Enter the approved canonical reference format.',
  };
}

const descriptors: CaseActionDescriptor[] = [
  {
    id: 'assign',
    label: 'Assign case',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/assignments`,
    fields: [assigneeField(true), reasonField],
    allowedFrom: ['OPEN', 'IN_REVIEW', 'ACTION_REQUIRED'],
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.assign({
        caseNumber: context.caseNumber,
        assigneeRef: required(values, 'assigneeRef'),
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
  {
    id: 'changePriority',
    label: 'Change priority',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/priority-changes`,
    fields: [
      {
        name: 'priority',
        label: 'Priority',
        kind: 'select',
        required: true,
        options: priorityOptions,
      },
      reasonField,
    ],
    allowedFrom: ['OPEN', 'IN_REVIEW', 'ACTION_REQUIRED'],
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.changePriority({
        caseNumber: context.caseNumber,
        priority: required(values, 'priority') as RiskCasePriority,
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
  reasonAction('beginReview', 'Begin review', 'review-start', ['OPEN']),
  reasonAction(
    'markActionRequired',
    'Mark action required',
    'action-required',
    ['IN_REVIEW'],
  ),
  reasonAction(
    'returnToReview',
    'Return to review',
    'review-return',
    ['ACTION_REQUIRED'],
  ),
  {
    id: 'resolve',
    label: 'Resolve case',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/resolutions`,
    fields: [
      {
        name: 'outcome',
        label: 'Resolution outcome',
        kind: 'select',
        required: true,
        options: resolutionOutcomeOptions,
      },
      {
        name: 'resolutionSummary',
        label: 'Resolution summary and reason',
        kind: 'textarea',
        required: true,
        maxLength: 2000,
        placeholder: 'Explain the outcome and why the case is being resolved',
      },
      {
        name: 'evidenceRefs',
        label: 'Evidence references (optional)',
        kind: 'reference-list',
        required: false,
        referencePattern: evidenceRefPattern,
        placeholder: 'One ev-<UUIDv4> reference per line',
      },
      {
        name: 'actionRefs',
        label: 'Action references (optional)',
        kind: 'reference-list',
        required: false,
        referencePattern: actionRefPattern,
        placeholder: 'One act-<UUIDv4> reference per line',
      },
    ],
    allowedFrom: ['IN_REVIEW', 'ACTION_REQUIRED'],
    terminal: true,
    confirmation: 'This will resolve the case and record the selected outcome.',
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.resolve({
        caseNumber: context.caseNumber,
        outcome: required(values, 'outcome') as RiskCaseResolutionOutcome,
        resolutionSummary: required(values, 'resolutionSummary'),
        evidenceRefs: splitReferenceList(values.evidenceRefs),
        actionRefs: splitReferenceList(values.actionRefs),
        expectedVersion: context.expectedVersion,
      }),
  },
  terminalReasonAction('close', 'Close case', 'closure', ['RESOLVED']),
  {
    id: 'cancel',
    label: 'Cancel case',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/cancellation`,
    fields: [
      reasonField,
      {
        name: 'duplicateCaseNumber',
        label: 'Duplicate case number (optional)',
        kind: 'text',
        required: false,
        maxLength: 39,
        pattern: caseNumberPattern,
        patternMessage: 'Enter a canonical RC-<UUIDv4> case number.',
      },
    ],
    allowedFrom: ['OPEN', 'IN_REVIEW', 'ACTION_REQUIRED'],
    terminal: true,
    confirmation: 'This will cancel the case.',
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.cancel({
        caseNumber: context.caseNumber,
        reason: required(values, 'reason'),
        ...(optional(values, 'duplicateCaseNumber')
          ? { duplicateCaseNumber: optional(values, 'duplicateCaseNumber') }
          : {}),
        expectedVersion: context.expectedVersion,
      }),
  },
  reopenAction('resume', 'Resume resolved case', 'resume', ['RESOLVED']),
  reopenAction('reopen', 'Reopen closed case', 'reopen', ['CLOSED']),
  {
    id: 'correctNote',
    label: 'Correct note',
    method: 'POST',
    path: ({ caseNumber, noteRef }) =>
      `${root(caseNumber)}/notes/${encodeURIComponent(boundNoteRef(noteRef))}/corrections`,
    fields: [
      {
        name: 'content',
        label: 'Corrected note',
        kind: 'textarea',
        required: true,
        maxLength: 4000,
        placeholder: 'Record the complete corrected investigation note',
      },
    ],
    allowedFrom: riskCaseStatuses,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.correctNote({
        caseNumber: context.caseNumber,
        noteRef: boundNoteRef(context.noteRef),
        content: required(values, 'content'),
        expectedVersion: context.expectedVersion,
      }),
  },
  {
    id: 'associateEvidence',
    label: 'Associate evidence',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/evidence-associations`,
    fields: [
      referenceField('evidenceRef', 'Evidence reference', 'evidence', true, evidenceRefPattern),
      sourceField,
      reasonField,
    ],
    allowedFrom: associationStatuses,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.associateEvidence({
        caseNumber: context.caseNumber,
        evidenceRef: required(values, 'evidenceRef'),
        source: required(values, 'source'),
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
  {
    id: 'changeEvidenceDisposition',
    label: 'Change evidence disposition',
    method: 'POST',
    path: ({ caseNumber }, values) =>
      `${root(caseNumber)}/evidence-associations/${encodeURIComponent(required(values ?? {}, 'associationEventRef'))}/dispositions`,
    fields: [
      {
        name: 'associationEventRef',
        label: 'Association event reference',
        kind: 'text',
        required: true,
        maxLength: 36,
        pattern: canonicalUuidV4Pattern,
        patternMessage: 'Enter the canonical lowercase UUIDv4 association event reference.',
        placeholder: 'History does not expose this event ID; paste its UUIDv4 reference',
        help:
          'Risk Case detail/history does not expose this event ID, and no approved preview endpoint exists. Confirm the UUID from an authoritative source before submitting.',
      },
      {
        name: 'disposition',
        label: 'Disposition',
        kind: 'select',
        required: true,
        options: ['SUPERSEDED', 'INVALIDATED', 'WITHDRAWN'].map((value) => ({
          label: titleCase(value),
          value,
        })),
      },
      referenceField(
        'replacementEvidenceRef',
        'Replacement evidence reference (optional)',
        'evidence',
        false,
        evidenceRefPattern,
      ),
      sourceField,
      reasonField,
    ],
    allowedFrom: associationStatuses,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.changeEvidenceDisposition({
        caseNumber: context.caseNumber,
        associationEventRef: required(values, 'associationEventRef'),
        disposition: required(values, 'disposition') as EvidenceDisposition,
        ...(optional(values, 'replacementEvidenceRef')
          ? { replacementEvidenceRef: optional(values, 'replacementEvidenceRef') }
          : {}),
        source: required(values, 'source'),
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
  {
    id: 'associateDecision',
    label: 'Associate decision',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/decision-associations`,
    fields: [
      referenceField('decisionRef', 'Decision reference', 'decision', true, decisionRefPattern),
      reasonField,
    ],
    allowedFrom: associationStatuses,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.associateDecision({
        caseNumber: context.caseNumber,
        decisionRef: required(values, 'decisionRef'),
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
  {
    id: 'selectDecision',
    label: 'Select current decision',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/decision-selection`,
    fields: [
      {
        name: 'decisionRef',
        label: 'Associated decision',
        kind: 'on-case-select',
        required: true,
      },
      reasonField,
    ],
    allowedFrom: associationStatuses,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.selectDecision({
        caseNumber: context.caseNumber,
        decisionRef: required(values, 'decisionRef'),
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
  {
    id: 'associateAction',
    label: 'Associate action',
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/action-associations`,
    fields: [
      referenceField(
        'actionRef',
        'Action reference',
        'action',
        true,
        associationActionRefPattern,
      ),
      reasonField,
    ],
    allowedFrom: associationStatuses,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.associateAction({
        caseNumber: context.caseNumber,
        actionRef: required(values, 'actionRef'),
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
  {
    id: 'referenceActionOutcome',
    label: 'Reference action outcome',
    method: 'POST',
    path: ({ caseNumber }, values) =>
      `${root(caseNumber)}/action-associations/${encodeURIComponent(required(values ?? {}, 'actionRef'))}/outcomes`,
    fields: [
      {
        name: 'actionRef',
        label: 'Associated action',
        kind: 'on-case-select',
        required: true,
      },
      referenceField(
        'outcomeRef',
        'Action outcome reference',
        'actionOutcome',
        true,
        actionOutcomeRefPattern,
      ),
      reasonField,
    ],
    allowedFrom: associationStatuses,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.referenceActionOutcome({
        caseNumber: context.caseNumber,
        actionRef: required(values, 'actionRef'),
        outcomeRef: required(values, 'outcomeRef'),
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  },
];

export const caseActionDescriptors = Object.freeze(descriptors);
export const actionBarDescriptors = caseActionDescriptors.filter(
  (descriptor) =>
    descriptor.id !== 'correctNote' &&
    !associationActionIds.includes(
      descriptor.id as (typeof associationActionIds)[number],
    ),
);
export const associationActionDescriptors = caseActionDescriptors.filter(
  (descriptor) =>
    associationActionIds.includes(
      descriptor.id as (typeof associationActionIds)[number],
    ),
);

export function actionsForStatus(status: RiskCaseStatus): CaseActionDescriptor[] {
  return actionBarDescriptors.filter((descriptor) => descriptor.allowedFrom.includes(status));
}

export function descriptorFor(id: CaseActionId): CaseActionDescriptor {
  const descriptor = caseActionDescriptors.find((candidate) => candidate.id === id);
  if (!descriptor) throw new Error(`Unknown case action: ${id}`);
  return descriptor;
}

function reasonAction(
  id: 'beginReview' | 'markActionRequired' | 'returnToReview',
  label: string,
  suffix: string,
  allowedFrom: readonly RiskCaseStatus[],
): CaseActionDescriptor {
  return {
    id,
    label,
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/${suffix}`,
    fields: [reasonField],
    allowedFrom,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository[id]({
        caseNumber: context.caseNumber,
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  };
}

function terminalReasonAction(
  id: 'close',
  label: string,
  suffix: string,
  allowedFrom: readonly RiskCaseStatus[],
): CaseActionDescriptor {
  return {
    id,
    label,
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/${suffix}`,
    fields: [reasonField],
    allowedFrom,
    terminal: true,
    confirmation: `This will ${label.toLowerCase()}.`,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository.close({
        caseNumber: context.caseNumber,
        reason: required(values, 'reason'),
        expectedVersion: context.expectedVersion,
      }),
  };
}

function reopenAction(
  id: 'resume' | 'reopen',
  label: string,
  suffix: string,
  allowedFrom: readonly RiskCaseStatus[],
): CaseActionDescriptor {
  return {
    id,
    label,
    method: 'POST',
    path: ({ caseNumber }) => `${root(caseNumber)}/${suffix}`,
    fields: [reasonField, assigneeField(false)],
    allowedFrom,
    terminal: false,
    messages: commonMessages,
    execute: (repository, context, values) =>
      repository[id]({
        caseNumber: context.caseNumber,
        reason: required(values, 'reason'),
        ...(optional(values, 'assigneeRef')
          ? { assigneeRef: optional(values, 'assigneeRef') }
          : {}),
        expectedVersion: context.expectedVersion,
      }),
  };
}

function root(caseNumber: string): string {
  return `/api/risk-cases/${encodeURIComponent(caseNumber)}`;
}

function required(values: CaseActionValues, name: keyof CaseActionValues): string {
  const value = values[name]?.trim();
  if (!value) throw new Error(`${String(name)} is required`);
  return value;
}

function optional(values: CaseActionValues, name: keyof CaseActionValues): string | undefined {
  return values[name]?.trim() || undefined;
}

function boundNoteRef(noteRef: string | undefined): string {
  if (!noteRef) throw new Error('A note reference is required to correct a note');
  return noteRef;
}

function titleCase(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}
