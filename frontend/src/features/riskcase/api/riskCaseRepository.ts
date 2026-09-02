import type { ApiClient } from '../../../core/api/apiClient';
import {
  parseRiskCaseDetail,
  parseRiskCaseHistoryPage,
  parseRiskCaseListPage,
  parseRiskCaseNote,
  type RiskCaseFilters,
  type RiskCaseListPage,
  type RiskCaseNote,
  type RiskCaseView,
} from './riskCaseTypes';

export interface AddNoteCommand {
  caseNumber: string;
  content: string;
  expectedVersion: number;
}

export interface RiskCaseRepository {
  list(filters: RiskCaseFilters, page: number, size: number): Promise<RiskCaseListPage>;
  get(caseNumber: string): Promise<RiskCaseView>;
  addNote(command: AddNoteCommand): Promise<RiskCaseNote>;
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
}
