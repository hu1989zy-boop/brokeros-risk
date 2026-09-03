import type { ResultCode } from '../../../core/api/contracts';
import type {
  RiskCaseRepository,
} from '../api/riskCaseRepository';
import {
  riskCaseStatuses,
  type RiskCaseDetail,
  type RiskCaseNote,
  type RiskCasePriority,
  type RiskCaseResolution,
  type RiskCaseResolutionOutcome,
  type RiskCaseStatus,
} from '../api/riskCaseTypes';
import {
  actionRefPattern,
  canonicalUuidV4Pattern,
  caseNumberPattern,
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

export type CaseActionId = (typeof caseActionIds)[number];
export type CaseActionResult = RiskCaseDetail | RiskCaseResolution | RiskCaseNote;

export interface CaseActionExecutionContext {
  caseNumber: string;
  expectedVersion: number;
  noteRef?: string;
}

export interface CaseActionDescriptor {
  id: CaseActionId;
  label: string;
  method: 'POST';
  path: (context: Pick<CaseActionExecutionContext, 'caseNumber' | 'noteRef'>) => string;
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
};

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
];

export const caseActionDescriptors = Object.freeze(descriptors);
export const actionBarDescriptors = caseActionDescriptors.filter(
  (descriptor) => descriptor.id !== 'correctNote',
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
