import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { ApiClient, type AuthSession } from '../src/core/api/apiClient';
import { HttpReferenceListRepository } from '../src/features/riskcase/api/referenceList';
import { envelope } from './fixtures/riskCases';
import { apiBaseUrl, server } from './support/server';

const subjectRef = 'ta-20000000-0000-4000-8000-000000000001';
const decisionRef = 'dec-20000000-0000-4000-8000-000000000002';
const actionRef = 'act-20000000-0000-4000-8000-000000000003';

function repository() {
  const auth: AuthSession = {
    getAccessToken: vi.fn(() => 'synthetic-test-token'),
    refreshAccessToken: vi.fn(async () => null),
    authenticationRequired: vi.fn(),
  };
  return new HttpReferenceListRepository(new ApiClient(apiBaseUrl, auth));
}

describe('Q-020 ReferenceListRepository', () => {
  it('uses the exact scoped endpoint and parses each content-free list shape', async () => {
    const seen: Record<string, string | null> = {};
    server.use(
      http.get(`${apiBaseUrl}/api/evidence`, ({ request }) => {
        seen.evidence = new URL(request.url).searchParams.get('subjectRef');
        return HttpResponse.json(envelope({
          items: [{
            evidenceRef: 'ev-20000000-0000-4000-8000-000000000004',
            subjectRef,
            status: 'ACTIVE',
            recordedAt: '2026-09-05T01:00:00Z',
          }],
        }));
      }),
      http.get(`${apiBaseUrl}/api/decisions`, ({ request }) => {
        seen.decision = new URL(request.url).searchParams.get('subjectRef');
        return HttpResponse.json(envelope({
          items: [{
            decisionRef,
            subjectRef,
            recordedAt: '2026-09-05T01:01:00Z',
          }],
        }));
      }),
      http.get(`${apiBaseUrl}/api/actions`, ({ request }) => {
        seen.action = new URL(request.url).searchParams.get('decisionRef');
        return HttpResponse.json(envelope({
          items: [{
            actionRef,
            decisionRef,
            status: 'PROPOSED',
            recordedAt: '2026-09-05T01:02:00Z',
          }],
        }));
      }),
      http.get(`${apiBaseUrl}/api/action-outcomes`, ({ request }) => {
        seen.outcome = new URL(request.url).searchParams.get('actionRef');
        return HttpResponse.json(envelope({
          items: [{
            actionOutcomeRef: 'aoc-20000000-0000-4000-8000-000000000005',
            actionRef,
            recordedAt: '2026-09-05T01:03:00Z',
          }],
        }));
      }),
    );
    const lists = repository();

    const [evidence, decisions, actions, outcomes] = await Promise.all([
      lists.listEvidence(subjectRef),
      lists.listDecisions(subjectRef),
      lists.listActions(decisionRef),
      lists.listOutcomes(actionRef),
    ]);

    expect(seen).toEqual({
      evidence: subjectRef,
      decision: subjectRef,
      action: decisionRef,
      outcome: actionRef,
    });
    expect(evidence[0]).toEqual(expect.objectContaining({ kind: 'evidence', subjectRef }));
    expect(decisions[0]).toEqual(expect.objectContaining({ kind: 'decision', subjectRef }));
    expect(actions[0]).toEqual(expect.objectContaining({ kind: 'action', decisionRef }));
    expect(outcomes[0]).toEqual(expect.objectContaining({ kind: 'actionOutcome', actionRef }));
  });

  it('parses an empty bounded response', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/evidence`, () =>
        HttpResponse.json(envelope({ items: [] })),
      ),
    );

    await expect(repository().listEvidence(subjectRef)).resolves.toEqual([]);
  });
});
