import type { ApiClient } from '../../../core/api/apiClient';
import {
  parseRiskCaseDetail,
  parseRiskCaseHistoryPage,
  parseRiskCaseListPage,
  parseRiskCaseNote,
  parseRiskCaseResolution,
  type BeginRiskCaseReviewRequest,
  type CancelRiskCaseRequest,
  type ChangeRiskCaseAssignmentRequest,
  type ChangeRiskCasePriorityRequest,
  type CloseRiskCaseRequest,
  type CorrectRiskCaseNoteRequest,
  type MarkRiskCaseActionRequiredRequest,
  type ReopenClosedRiskCaseRequest,
  type ResolveRiskCaseRequest,
  type ResumeResolvedRiskCaseRequest,
  type ReturnRiskCaseToReviewRequest,
  type RiskCaseDetail,
  type RiskCaseFilters,
  type RiskCaseListPage,
  type RiskCaseNote,
  type RiskCaseResolution,
  type RiskCaseView,
} from './riskCaseTypes';

export interface AddNoteCommand {
  caseNumber: string;
  content: string;
  expectedVersion: number;
}

interface RiskCaseCommand {
  caseNumber: string;
}

export type ChangeRiskCaseAssignmentCommand = RiskCaseCommand & ChangeRiskCaseAssignmentRequest;
export type ChangeRiskCasePriorityCommand = RiskCaseCommand & ChangeRiskCasePriorityRequest;
export type BeginRiskCaseReviewCommand = RiskCaseCommand & BeginRiskCaseReviewRequest;
export type MarkRiskCaseActionRequiredCommand = RiskCaseCommand & MarkRiskCaseActionRequiredRequest;
export type ReturnRiskCaseToReviewCommand = RiskCaseCommand & ReturnRiskCaseToReviewRequest;
export type ResolveRiskCaseCommand = RiskCaseCommand & ResolveRiskCaseRequest;
export type CloseRiskCaseCommand = RiskCaseCommand & CloseRiskCaseRequest;
export type CancelRiskCaseCommand = RiskCaseCommand & CancelRiskCaseRequest;
export type ResumeResolvedRiskCaseCommand = RiskCaseCommand & ResumeResolvedRiskCaseRequest;
export type ReopenClosedRiskCaseCommand = RiskCaseCommand & ReopenClosedRiskCaseRequest;
export type CorrectRiskCaseNoteCommand = RiskCaseCommand &
  CorrectRiskCaseNoteRequest & { noteRef: string };

export interface RiskCaseRepository {
  list(filters: RiskCaseFilters, page: number, size: number): Promise<RiskCaseListPage>;
  get(caseNumber: string): Promise<RiskCaseView>;
  addNote(command: AddNoteCommand): Promise<RiskCaseNote>;
  assign(command: ChangeRiskCaseAssignmentCommand): Promise<RiskCaseDetail>;
  changePriority(command: ChangeRiskCasePriorityCommand): Promise<RiskCaseDetail>;
  beginReview(command: BeginRiskCaseReviewCommand): Promise<RiskCaseDetail>;
  markActionRequired(command: MarkRiskCaseActionRequiredCommand): Promise<RiskCaseDetail>;
  returnToReview(command: ReturnRiskCaseToReviewCommand): Promise<RiskCaseDetail>;
  resolve(command: ResolveRiskCaseCommand): Promise<RiskCaseResolution>;
  close(command: CloseRiskCaseCommand): Promise<RiskCaseDetail>;
  cancel(command: CancelRiskCaseCommand): Promise<RiskCaseDetail>;
  resume(command: ResumeResolvedRiskCaseCommand): Promise<RiskCaseDetail>;
  reopen(command: ReopenClosedRiskCaseCommand): Promise<RiskCaseDetail>;
  correctNote(command: CorrectRiskCaseNoteCommand): Promise<RiskCaseNote>;
}

export class HttpRiskCaseRepository implements RiskCaseRepository {
  constructor(private readonly apiClient: ApiClient) {}

  list(filters: RiskCaseFilters, page: number, size: number): Promise<RiskCaseListPage> {
    return this.apiClient.get('/api/risk-cases', parseRiskCaseListPage, {
      params: {
        ...filters,
        page,
        size,
      },
    });
  }

  async get(caseNumber: string): Promise<RiskCaseView> {
    const pathCaseNumber = encodeURIComponent(caseNumber);
    const [detail, history] = await Promise.all([
      this.apiClient.get(`/api/risk-cases/${pathCaseNumber}`, parseRiskCaseDetail),
      this.apiClient.get(
        `/api/risk-cases/${pathCaseNumber}/history`,
        parseRiskCaseHistoryPage,
        { params: { limit: 100 } },
      ),
    ]);
    return { detail, history };
  }

  addNote(command: AddNoteCommand): Promise<RiskCaseNote> {
    return this.apiClient.post(
      `/api/risk-cases/${encodeURIComponent(command.caseNumber)}/notes`,
      {
        content: command.content,
        expectedVersion: command.expectedVersion,
      },
      parseRiskCaseNote,
    );
  }

  assign(command: ChangeRiskCaseAssignmentCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'assignments', {
      assigneeRef: command.assigneeRef,
      reason: command.reason,
      expectedVersion: command.expectedVersion,
    });
  }

  changePriority(command: ChangeRiskCasePriorityCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'priority-changes', {
      priority: command.priority,
      reason: command.reason,
      expectedVersion: command.expectedVersion,
    });
  }

  beginReview(command: BeginRiskCaseReviewCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'review-start', reasonBody(command));
  }

  markActionRequired(command: MarkRiskCaseActionRequiredCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'action-required', reasonBody(command));
  }

  returnToReview(command: ReturnRiskCaseToReviewCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'review-return', reasonBody(command));
  }

  resolve(command: ResolveRiskCaseCommand): Promise<RiskCaseResolution> {
    return this.apiClient.post(
      `${casePath(command.caseNumber)}/resolutions`,
      {
        outcome: command.outcome,
        resolutionSummary: command.resolutionSummary,
        evidenceRefs: command.evidenceRefs,
        actionRefs: command.actionRefs,
        expectedVersion: command.expectedVersion,
      },
      parseRiskCaseResolution,
    );
  }

  close(command: CloseRiskCaseCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'closure', reasonBody(command));
  }

  cancel(command: CancelRiskCaseCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'cancellation', {
      reason: command.reason,
      ...(command.duplicateCaseNumber
        ? { duplicateCaseNumber: command.duplicateCaseNumber }
        : {}),
      expectedVersion: command.expectedVersion,
    });
  }

  resume(command: ResumeResolvedRiskCaseCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'resume', optionalAssigneeBody(command));
  }

  reopen(command: ReopenClosedRiskCaseCommand): Promise<RiskCaseDetail> {
    return this.postDetail(command, 'reopen', optionalAssigneeBody(command));
  }

  correctNote(command: CorrectRiskCaseNoteCommand): Promise<RiskCaseNote> {
    return this.apiClient.post(
      `${casePath(command.caseNumber)}/notes/${encodeURIComponent(command.noteRef)}/corrections`,
      { content: command.content, expectedVersion: command.expectedVersion },
      parseRiskCaseNote,
    );
  }

  private postDetail(
    command: RiskCaseCommand,
    suffix: string,
    body: unknown,
  ): Promise<RiskCaseDetail> {
    return this.apiClient.post(`${casePath(command.caseNumber)}/${suffix}`, body, parseRiskCaseDetail);
  }
}

function casePath(caseNumber: string): string {
  return `/api/risk-cases/${encodeURIComponent(caseNumber)}`;
}

function reasonBody(command: { reason: string; expectedVersion: number }) {
  return { reason: command.reason, expectedVersion: command.expectedVersion };
}

function optionalAssigneeBody(command: {
  reason: string;
  assigneeRef?: string;
  expectedVersion: number;
}) {
  return {
    reason: command.reason,
    ...(command.assigneeRef ? { assigneeRef: command.assigneeRef } : {}),
    expectedVersion: command.expectedVersion,
  };
}
