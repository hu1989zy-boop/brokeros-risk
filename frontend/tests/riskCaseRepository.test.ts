import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { ApiClient, type AuthSession } from '../src/core/api/apiClient';
import { ApiError, AuthenticationRequiredError, AuthorizationError } from '../src/core/api/errors';
import { HttpRiskCaseRepository } from '../src/features/riskcase/api/riskCaseRepository';
import {
  envelope,
  failureEnvelope,
  riskCaseAssociations,
  riskCaseListPage,
  riskCaseNote,
} from './fixtures/riskCases';
import { apiBaseUrl, server } from './support/server';

function authSession(token = 'test-access-token'): AuthSession {
  return {
    getAccessToken: vi.fn(() => token),
    refreshAccessToken: vi.fn(async () => 'refreshed-test-token'),
    authenticationRequired: vi.fn(),
  };
}

describe('HttpRiskCaseRepository with MSW', () => {
  it('gets and parses the authoritative association projection with bearer auth', async () => {
    let capturedAuthorization: string | null = null;
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber/associations`, ({ request }) => {
        capturedAuthorization = request.headers.get('Authorization');
        return HttpResponse.json(envelope(riskCaseAssociations));
      }),
    );
    const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, authSession()));

    await expect(repository.getAssociations('RC-2026-000001')).resolves.toEqual(
      riskCaseAssociations,
    );
    expect(capturedAuthorization).toBe('Bearer test-access-token');
  });

  it('sends bounded filters and the bearer token for the list query', async () => {
    let capturedAuthorization: string | null = null;
    let capturedUrl = '';
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases`, ({ request }) => {
        capturedAuthorization = request.headers.get('Authorization');
        capturedUrl = request.url;
        return HttpResponse.json(envelope(riskCaseListPage));
      }),
    );
    const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, authSession()));

    const page = await repository.list({ status: 'IN_REVIEW', priority: 'HIGH' }, 0, 20);

    expect(page).toEqual(riskCaseListPage);
    expect(capturedAuthorization).toBe('Bearer test-access-token');
    expect(capturedUrl).toContain('status=IN_REVIEW');
    expect(capturedUrl).toContain('priority=HIGH');
    expect(capturedUrl).toContain('size=20');
  });

  it('submits only note content and expectedVersion', async () => {
    let capturedBody: unknown;
    server.use(
      http.post(`${apiBaseUrl}/api/risk-cases/:caseNumber/notes`, async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(envelope(riskCaseNote), { status: 201 });
      }),
    );
    const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, authSession()));

    await repository.addNote({ caseNumber: 'RC-2026-000001', content: 'Checked.', expectedVersion: 7 });

    expect(capturedBody).toEqual({ content: 'Checked.', expectedVersion: 7 });
    expect(capturedBody).not.toHaveProperty('actorRef');
  });

  it('performs one silent refresh and retries one 401', async () => {
    let calls = 0;
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases`, ({ request }) => {
        calls += 1;
        if (calls === 1) {
          return HttpResponse.json(failureEnvelope('AUTHENTICATION_INVALID', 'Session expired'), {
            status: 401,
          });
        }
        expect(request.headers.get('Authorization')).toBe('Bearer refreshed-test-token');
        return HttpResponse.json(envelope(riskCaseListPage));
      }),
    );
    const auth = authSession();
    const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, auth));

    await expect(repository.list({}, 0, 20)).resolves.toEqual(riskCaseListPage);
    expect(auth.refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(calls).toBe(2);
  });

  it('maps a 403 backend envelope to a typed authorization error', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases`, () =>
        HttpResponse.json(failureEnvelope('AUTHORIZATION_DENIED', 'Authorization is denied'), {
          status: 403,
        }),
      ),
    );
    const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, authSession()));

    await expect(repository.list({}, 0, 20)).rejects.toBeInstanceOf(AuthorizationError);
  });

  it('does not treat an unknown ResultCode as success', async () => {
    server.use(
      http.post(`${apiBaseUrl}/api/risk-cases/:caseNumber/review-start`, () =>
        HttpResponse.json(failureEnvelope('FUTURE_RISK_CASE_ERROR', 'Future backend rejection')),
      ),
    );
    const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, authSession()));

    await expect(
      repository.beginReview({
        caseNumber: 'RC-2026-000001',
        reason: 'Synthetic test reason.',
        expectedVersion: 7,
      }),
    ).rejects.toMatchObject({ code: 'FUTURE_RISK_CASE_ERROR' } satisfies Partial<ApiError>);
  });

  it('stops after refresh failure and requires authentication without a retry loop', async () => {
    let calls = 0;
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases`, () => {
        calls += 1;
        return HttpResponse.json(failureEnvelope('AUTHENTICATION_INVALID', 'Session expired'), {
          status: 401,
        });
      }),
    );
    const auth = authSession();
    vi.mocked(auth.refreshAccessToken).mockResolvedValue(null);
    const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, auth));

    await expect(repository.list({}, 0, 20)).rejects.toBeInstanceOf(AuthenticationRequiredError);
    expect(auth.refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(auth.authenticationRequired).toHaveBeenCalledTimes(1);
    expect(calls).toBe(1);
  });
});
