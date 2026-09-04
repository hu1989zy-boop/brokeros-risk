import { describe, expect, it } from 'vitest';

import { parseApiResponse, type ResultCode } from '../src/core/api/contracts';
import {
  parseRiskCaseDetail,
  parseRiskCaseAssociations,
  parseRiskCaseHistoryPage,
  parseRiskCaseListPage,
  parseRiskCaseNote,
} from '../src/features/riskcase/api/riskCaseTypes';
import {
  envelope,
  riskCaseDetail,
  riskCaseAssociations,
  riskCaseHistory,
  riskCaseListPage,
  riskCaseNote,
} from './fixtures/riskCases';

describe('documented backend contract', () => {
  it('parses the ApiResponse and bounded list envelope', () => {
    const parsed = parseApiResponse(envelope(riskCaseListPage), parseRiskCaseListPage);
    expect(parsed.code satisfies string).toBe('SUCCESS');
    expect(parsed.data).toEqual(riskCaseListPage);
    expect(parsed.data?.size).toBe(20);
  });

  it('parses detail, history, and add-note response DTOs', () => {
    expect(parseApiResponse(envelope(riskCaseDetail), parseRiskCaseDetail).data).toEqual(
      riskCaseDetail,
    );
    expect(parseApiResponse(envelope(riskCaseHistory), parseRiskCaseHistoryPage).data).toEqual(
      riskCaseHistory,
    );
    expect(parseApiResponse(envelope(riskCaseNote), parseRiskCaseNote).data).toEqual(riskCaseNote);
    expect(
      parseApiResponse(envelope(riskCaseAssociations), parseRiskCaseAssociations).data,
    ).toEqual(riskCaseAssociations);
  });

  it('keeps consumed ResultCodes as a compile-time string union', () => {
    const versionConflict: ResultCode = 'RISK_CASE_VERSION_CONFLICT';
    expect(versionConflict).toBe('RISK_CASE_VERSION_CONFLICT');
  });

  it('rejects contract drift instead of silently rendering it', () => {
    const drifted = envelope({ ...riskCaseListPage, items: [{ ...riskCaseListPage.items[0], status: 'NEW' }] });
    expect(() => parseApiResponse(drifted, parseRiskCaseListPage)).toThrow(
      'RiskCaseSummary.status contains an unsupported value',
    );
  });
});
