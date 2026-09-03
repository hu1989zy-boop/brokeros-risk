import type {
  RiskCaseDetail,
  RiskCaseHistoryPage,
  RiskCaseListPage,
  RiskCaseNote,
  RiskCaseResolution,
} from '../../src/features/riskcase/api/riskCaseTypes';

export const caseNumber = 'RC-2026-000001';

export const riskCaseListPage: RiskCaseListPage = {
  items: [
    {
      caseNumber,
      subjectRef: 'trading-account:demo-1001',
      status: 'IN_REVIEW',
      priority: 'HIGH',
      assigneeRef: '16000000-0000-4000-8000-000000000001',
      createdAt: '2026-09-01T08:00:00Z',
      updatedAt: '2026-09-02T09:30:00Z',
      version: 7,
    },
  ],
  page: 0,
  size: 20,
  hasNext: false,
};

export const riskCaseDetail: RiskCaseDetail = {
  caseNumber,
  subjectType: 'TRADING_ACCOUNT',
  subjectRef: 'trading-account:demo-1001',
  intakeSource: 'MANUAL',
  intakeSummary: 'Review a bounded demonstration account event.',
  status: 'IN_REVIEW',
  priority: 'HIGH',
  assigneeRef: '16000000-0000-4000-8000-000000000001',
  assignedByRef: '16000000-0000-4000-8000-000000000001',
  assignedAt: '2026-09-01T08:05:00Z',
  currentDecisionRef: 'decision:demo-5001',
  currentCycleNo: 1,
  createdByRef: '16000000-0000-4000-8000-000000000001',
  createdAt: '2026-09-01T08:00:00Z',
  updatedByRef: '16000000-0000-4000-8000-000000000001',
  updatedAt: '2026-09-02T09:30:00Z',
  version: 7,
};

export const riskCaseHistory: RiskCaseHistoryPage = {
  entries: [
    {
      version: 6,
      eventType: 'NOTE',
      affectedRef: '17000000-0000-4000-8000-000000000000',
      actorRef: '16000000-0000-4000-8000-000000000001',
      occurredAt: '2026-09-02T09:00:00Z',
    },
    {
      version: 7,
      eventType: 'DECISION_ASSOCIATED',
      affectedRef: 'decision:demo-5001',
      actorRef: '16000000-0000-4000-8000-000000000001',
      occurredAt: '2026-09-02T09:30:00Z',
    },
    {
      version: 1,
      eventType: 'CASE_CREATED',
      affectedRef: null,
      actorRef: '16000000-0000-4000-8000-000000000001',
      occurredAt: '2026-09-01T08:00:00Z',
    },
  ],
  nextCursor: null,
};

export const riskCaseNote: RiskCaseNote = {
  noteRef: '17000000-0000-4000-8000-000000000001',
  supersedesNoteRef: null,
  version: 8,
  createdByRef: '16000000-0000-4000-8000-000000000001',
  createdAt: '2026-09-02T10:00:00Z',
};

export const riskCaseResolution: RiskCaseResolution = {
  riskCase: { ...riskCaseDetail, status: 'RESOLVED', version: 8 },
  cycleNo: 1,
  outcome: 'NO_RISK',
  decisionRef: 'decision:demo-5001',
  resolutionSummary: 'Resolved after bounded review.',
  resolvedByRef: '16000000-0000-4000-8000-000000000001',
  resolvedAt: '2026-09-02T10:00:00Z',
};

export function envelope<T>(data: T) {
  return {
    code: 'SUCCESS',
    message: 'Success',
    data,
    timestamp: '2026-09-02T10:00:00Z',
  };
}

export function failureEnvelope(code: string, message: string) {
  return {
    code,
    message,
    data: null,
    timestamp: '2026-09-02T10:00:00Z',
  };
}
