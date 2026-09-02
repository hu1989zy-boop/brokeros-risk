import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

import {
  caseNumber,
  envelope,
  riskCaseDetail,
  riskCaseHistory,
  riskCaseListPage,
  riskCaseNote,
} from '../fixtures/riskCases';

export const apiBaseUrl = 'http://localhost:8080';

export const defaultHandlers = [
  http.get(`${apiBaseUrl}/api/risk-cases`, () => HttpResponse.json(envelope(riskCaseListPage))),
  http.get(`${apiBaseUrl}/api/risk-cases/${caseNumber}`, () =>
    HttpResponse.json(envelope(riskCaseDetail)),
  ),
  http.get(`${apiBaseUrl}/api/risk-cases/${caseNumber}/history`, () =>
    HttpResponse.json(envelope(riskCaseHistory)),
  ),
  http.post(`${apiBaseUrl}/api/risk-cases/${caseNumber}/notes`, () =>
    HttpResponse.json(envelope(riskCaseNote), { status: 201 }),
  ),
];

export const server = setupServer(...defaultHandlers);
