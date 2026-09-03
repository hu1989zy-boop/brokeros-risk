import {
  asArray,
  asBoolean,
  asInstant,
  asInteger,
  asNullableString,
  asRecord,
  asString,
  ContractError,
} from '../../../core/api/contracts';

export const riskCaseStatuses = [
  'OPEN',
  'IN_REVIEW',
  'ACTION_REQUIRED',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
] as const;
export type RiskCaseStatus = (typeof riskCaseStatuses)[number];

export const riskCasePriorities = ['LOW', 'NORMAL', 'HIGH', 'CRITICAL'] as const;
export type RiskCasePriority = (typeof riskCasePriorities)[number];

export const riskCaseResolutionOutcomes = [
  'RISK_CONFIRMED_ACTION_COMPLETED',
  'NO_RISK',
  'FALSE_POSITIVE',
  'MONITORING_ONLY',
  'NO_ACTION_REQUIRED',
] as const;
export type RiskCaseResolutionOutcome = (typeof riskCaseResolutionOutcomes)[number];

export interface VersionedRiskCaseRequest {
  expectedVersion: number;
}

export interface ChangeRiskCaseAssignmentRequest extends VersionedRiskCaseRequest {
  assigneeRef: string;
  reason: string;
}

export interface ChangeRiskCasePriorityRequest extends VersionedRiskCaseRequest {
  priority: RiskCasePriority;
  reason: string;
}

export interface RiskCaseReasonRequest extends VersionedRiskCaseRequest {
  reason: string;
}

export type BeginRiskCaseReviewRequest = RiskCaseReasonRequest;
export type MarkRiskCaseActionRequiredRequest = RiskCaseReasonRequest;
export type ReturnRiskCaseToReviewRequest = RiskCaseReasonRequest;
export type CloseRiskCaseRequest = RiskCaseReasonRequest;

export interface ResolveRiskCaseRequest extends VersionedRiskCaseRequest {
  outcome: RiskCaseResolutionOutcome;
  resolutionSummary: string;
  evidenceRefs: string[];
  actionRefs: string[];
}

export interface CancelRiskCaseRequest extends RiskCaseReasonRequest {
  duplicateCaseNumber?: string;
}

export interface ResumeResolvedRiskCaseRequest extends RiskCaseReasonRequest {
  assigneeRef?: string;
}

export type ReopenClosedRiskCaseRequest = ResumeResolvedRiskCaseRequest;

export interface CorrectRiskCaseNoteRequest extends VersionedRiskCaseRequest {
  content: string;
}

export interface RiskCaseSummary {
  caseNumber: string;
  subjectRef: string;
  status: RiskCaseStatus;
  priority: RiskCasePriority;
  assigneeRef: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface RiskCaseListPage {
  items: RiskCaseSummary[];
  page: number;
  size: number;
  hasNext: boolean;
}

export interface RiskCaseDetail {
  caseNumber: string;
  subjectType: string;
  subjectRef: string;
  intakeSource: string;
  intakeSummary: string;
  status: RiskCaseStatus;
  priority: RiskCasePriority;
  assigneeRef: string | null;
  assignedByRef: string | null;
  assignedAt: string | null;
  currentDecisionRef: string | null;
  currentCycleNo: number;
  createdByRef: string;
  createdAt: string;
  updatedByRef: string;
  updatedAt: string;
  version: number;
}

export interface RiskCaseHistoryEntry {
  version: number;
  eventType: string;
  affectedRef: string | null;
  actorRef: string;
  occurredAt: string;
}

export interface RiskCaseHistoryPage {
  entries: RiskCaseHistoryEntry[];
  nextCursor: string | null;
}

export interface RiskCaseView {
  detail: RiskCaseDetail;
  history: RiskCaseHistoryPage;
}

export interface RiskCaseNote {
  noteRef: string;
  supersedesNoteRef: string | null;
  version: number;
  createdByRef: string;
  createdAt: string;
}

export interface RiskCaseResolution {
  riskCase: RiskCaseDetail;
  cycleNo: number;
  outcome: RiskCaseResolutionOutcome;
  decisionRef: string;
  resolutionSummary: string;
  resolvedByRef: string;
  resolvedAt: string;
}

export interface RiskCaseFilters {
  status?: RiskCaseStatus;
  priority?: RiskCasePriority;
  subjectRef?: string;
  assignee?: string;
}

function enumValue<T extends string>(value: unknown, allowed: readonly T[], label: string): T {
  const candidate = asString(value, label);
  if (!allowed.includes(candidate as T)) {
    throw new ContractError(`${label} contains an unsupported value`);
  }
  return candidate as T;
}

function optionalInstant(value: unknown, label: string): string | null {
  return value === null ? null : asInstant(value, label);
}

export function parseRiskCaseSummary(value: unknown): RiskCaseSummary {
  const record = asRecord(value, 'RiskCaseSummary');
  return {
    caseNumber: asString(record.caseNumber, 'RiskCaseSummary.caseNumber'),
    subjectRef: asString(record.subjectRef, 'RiskCaseSummary.subjectRef'),
    status: enumValue(record.status, riskCaseStatuses, 'RiskCaseSummary.status'),
    priority: enumValue(record.priority, riskCasePriorities, 'RiskCaseSummary.priority'),
    assigneeRef: asNullableString(record.assigneeRef, 'RiskCaseSummary.assigneeRef'),
    createdAt: asInstant(record.createdAt, 'RiskCaseSummary.createdAt'),
    updatedAt: asInstant(record.updatedAt, 'RiskCaseSummary.updatedAt'),
    version: asInteger(record.version, 'RiskCaseSummary.version'),
  };
}

export function parseRiskCaseListPage(value: unknown): RiskCaseListPage {
  const record = asRecord(value, 'RiskCaseListResponse');
  return {
    items: asArray(record.items, 'RiskCaseListResponse.items', parseRiskCaseSummary),
    page: asInteger(record.page, 'RiskCaseListResponse.page'),
    size: asInteger(record.size, 'RiskCaseListResponse.size'),
    hasNext: asBoolean(record.hasNext, 'RiskCaseListResponse.hasNext'),
  };
}

export function parseRiskCaseDetail(value: unknown): RiskCaseDetail {
  const record = asRecord(value, 'RiskCaseDetail');
  return {
    caseNumber: asString(record.caseNumber, 'RiskCaseDetail.caseNumber'),
    subjectType: asString(record.subjectType, 'RiskCaseDetail.subjectType'),
    subjectRef: asString(record.subjectRef, 'RiskCaseDetail.subjectRef'),
    intakeSource: asString(record.intakeSource, 'RiskCaseDetail.intakeSource'),
    intakeSummary: asString(record.intakeSummary, 'RiskCaseDetail.intakeSummary'),
    status: enumValue(record.status, riskCaseStatuses, 'RiskCaseDetail.status'),
    priority: enumValue(record.priority, riskCasePriorities, 'RiskCaseDetail.priority'),
    assigneeRef: asNullableString(record.assigneeRef, 'RiskCaseDetail.assigneeRef'),
    assignedByRef: asNullableString(record.assignedByRef, 'RiskCaseDetail.assignedByRef'),
    assignedAt: optionalInstant(record.assignedAt, 'RiskCaseDetail.assignedAt'),
    currentDecisionRef: asNullableString(
      record.currentDecisionRef,
      'RiskCaseDetail.currentDecisionRef',
    ),
    currentCycleNo: asInteger(record.currentCycleNo, 'RiskCaseDetail.currentCycleNo'),
    createdByRef: asString(record.createdByRef, 'RiskCaseDetail.createdByRef'),
    createdAt: asInstant(record.createdAt, 'RiskCaseDetail.createdAt'),
    updatedByRef: asString(record.updatedByRef, 'RiskCaseDetail.updatedByRef'),
    updatedAt: asInstant(record.updatedAt, 'RiskCaseDetail.updatedAt'),
    version: asInteger(record.version, 'RiskCaseDetail.version'),
  };
}

export function parseRiskCaseHistoryPage(value: unknown): RiskCaseHistoryPage {
  const record = asRecord(value, 'RiskCaseHistoryPage');
  return {
    entries: asArray(record.entries, 'RiskCaseHistoryPage.entries', (entry) => {
      const item = asRecord(entry, 'RiskCaseHistoryEntry');
      return {
        version: asInteger(item.version, 'RiskCaseHistoryEntry.version'),
        eventType: asString(item.eventType, 'RiskCaseHistoryEntry.eventType'),
        affectedRef: asNullableString(item.affectedRef, 'RiskCaseHistoryEntry.affectedRef'),
        actorRef: asString(item.actorRef, 'RiskCaseHistoryEntry.actorRef'),
        occurredAt: asInstant(item.occurredAt, 'RiskCaseHistoryEntry.occurredAt'),
      };
    }),
    nextCursor: asNullableString(record.nextCursor, 'RiskCaseHistoryPage.nextCursor'),
  };
}

export function parseRiskCaseNote(value: unknown): RiskCaseNote {
  const record = asRecord(value, 'RiskCaseNote');
  return {
    noteRef: asString(record.noteRef, 'RiskCaseNote.noteRef'),
    supersedesNoteRef: asNullableString(record.supersedesNoteRef, 'RiskCaseNote.supersedesNoteRef'),
    version: asInteger(record.version, 'RiskCaseNote.version'),
    createdByRef: asString(record.createdByRef, 'RiskCaseNote.createdByRef'),
    createdAt: asInstant(record.createdAt, 'RiskCaseNote.createdAt'),
  };
}

export function parseRiskCaseResolution(value: unknown): RiskCaseResolution {
  const record = asRecord(value, 'RiskCaseResolution');
  return {
    riskCase: parseRiskCaseDetail(record.riskCase),
    cycleNo: asInteger(record.cycleNo, 'RiskCaseResolution.cycleNo'),
    outcome: enumValue(
      record.outcome,
      riskCaseResolutionOutcomes,
      'RiskCaseResolution.outcome',
    ),
    decisionRef: asString(record.decisionRef, 'RiskCaseResolution.decisionRef'),
    resolutionSummary: asString(
      record.resolutionSummary,
      'RiskCaseResolution.resolutionSummary',
    ),
    resolvedByRef: asString(record.resolvedByRef, 'RiskCaseResolution.resolvedByRef'),
    resolvedAt: asInstant(record.resolvedAt, 'RiskCaseResolution.resolvedAt'),
  };
}
