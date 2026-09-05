import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { ApiClient, type AuthSession } from '../src/core/api/apiClient';
import type { ReferenceKind } from '../src/features/riskcase/actions/actionInputs';
import {
  HttpReferenceListRepository,
  type ReferenceBrowseScope,
} from '../src/features/riskcase/api/referenceList';
import { HttpReferencePreviewRepository } from '../src/features/riskcase/api/referencePreview';
import { ReferenceListRepositoryProvider } from '../src/features/riskcase/model/referenceListContext';
import { ReferencePreviewRepositoryProvider } from '../src/features/riskcase/model/referencePreviewContext';
import { ReferenceInput } from '../src/features/riskcase/ui/ReferenceInput';
import { envelope, failureEnvelope } from './fixtures/riskCases';
import { apiBaseUrl, server } from './support/server';

const evidenceRef = 'ev-18000000-0000-4000-8000-000000000001';

function renderInput(
  kind: ReferenceKind = 'evidence',
  browseScope?: ReferenceBrowseScope,
) {
  const auth: AuthSession = {
    getAccessToken: () => 'synthetic-test-token',
    refreshAccessToken: async () => null,
    authenticationRequired: vi.fn(),
  };
  const client = new ApiClient(apiBaseUrl, auth);
  const repository = new HttpReferencePreviewRepository(client);
  const referenceLists = new HttpReferenceListRepository(client);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const confirmation = vi.fn();

  function Harness() {
    const [value, setValue] = useState('');
    return (
      <QueryClientProvider client={queryClient}>
        <ReferenceListRepositoryProvider repository={referenceLists}>
          <ReferencePreviewRepositoryProvider repository={repository}>
            <label htmlFor="reference-under-test">Reference</label>
            <ReferenceInput
              id="reference-under-test"
              kind={kind}
              value={value}
              required
              browseScope={browseScope}
              onChange={setValue}
              onConfirmationChange={confirmation}
            />
          </ReferencePreviewRepositoryProvider>
        </ReferenceListRepositoryProvider>
      </QueryClientProvider>
    );
  }
  render(<Harness />);
  return { confirmation };
}

describe('Q-018 ReferenceInput', () => {
  it('validates format, fetches a bounded preview, and confirms only the matching ref', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/evidence/${evidenceRef}`, () =>
        HttpResponse.json(
          envelope({
            evidenceRef,
            subjectRef: 'subject:synthetic',
            source: 'MANUAL',
            status: 'ACTIVE',
            recordedAt: '2026-09-03T00:00:00Z',
          }),
        ),
      ),
    );
    const { confirmation } = renderInput();

    await userEvent.type(screen.getByLabelText('Reference'), evidenceRef);

    expect(await screen.findByText('Confirmed reference preview')).toBeInTheDocument();
    expect(screen.getByText('subject:synthetic')).toBeInTheDocument();
    await waitFor(() => expect(confirmation).toHaveBeenLastCalledWith(true));
  });

  it('blocks invalid format without calling the backend', async () => {
    let calls = 0;
    server.use(
      http.get(`${apiBaseUrl}/api/evidence/:reference`, () => {
        calls += 1;
        return HttpResponse.json(envelope({}));
      }),
    );
    const { confirmation } = renderInput();

    await userEvent.type(screen.getByLabelText('Reference'), 'ev-not-a-uuid');

    expect(await screen.findByText('Enter ev-<UUIDv4>.')).toBeInTheDocument();
    await new Promise((resolve) => window.setTimeout(resolve, 350));
    expect(calls).toBe(0);
    expect(confirmation).toHaveBeenLastCalledWith(false);
  });

  it('maps preview 404 to not-found and keeps the ref unconfirmed', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/evidence/${evidenceRef}`, () =>
        HttpResponse.json(failureEnvelope('EVIDENCE_NOT_FOUND', 'Not found'), { status: 404 }),
      ),
    );
    const { confirmation } = renderInput();

    await userEvent.type(screen.getByLabelText('Reference'), evidenceRef);

    expect(await screen.findByText('The reference was not found.')).toBeInTheDocument();
    expect(confirmation).toHaveBeenLastCalledWith(false);
  });

  it('maps preview 403 to a typed authorization state and blocks confirmation', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/evidence/${evidenceRef}`, () =>
        HttpResponse.json(failureEnvelope('AUTHORIZATION_DENIED', 'Forbidden'), { status: 403 }),
      ),
    );
    const { confirmation } = renderInput();

    await userEvent.type(screen.getByLabelText('Reference'), evidenceRef);

    expect(
      await screen.findByText('You are not authorized to preview this reference.'),
    ).toBeInTheDocument();
    expect(confirmation).toHaveBeenLastCalledWith(false);
  });
});

describe('Q-020 ReferenceInput browse mode', () => {
  it('browses within the supplied subject scope and selects into the existing preview flow', async () => {
    const subjectRef = 'ta-18000000-0000-4000-8000-000000000002';
    let requestedScope: string | null = null;
    server.use(
      http.get(`${apiBaseUrl}/api/evidence`, ({ request }) => {
        requestedScope = new URL(request.url).searchParams.get('subjectRef');
        return HttpResponse.json(
          envelope({
            items: [
              {
                evidenceRef,
                subjectRef,
                status: 'ACTIVE',
                recordedAt: '2026-09-05T00:00:00Z',
              },
            ],
          }),
        );
      }),
      http.get(`${apiBaseUrl}/api/evidence/${evidenceRef}`, () =>
        HttpResponse.json(
          envelope({
            evidenceRef,
            subjectRef,
            source: 'MANUAL',
            status: 'ACTIVE',
            recordedAt: '2026-09-05T00:00:00Z',
          }),
        ),
      ),
    );
    const { confirmation } = renderInput('evidence', { subjectRef });

    await userEvent.click(screen.getByLabelText('Reference'));
    await userEvent.click(
      await screen.findByText((content, element) =>
        Boolean(element?.classList.contains('ant-select-item-option-content')) &&
        content.includes(evidenceRef)),
    );

    expect(requestedScope).toBe(subjectRef);
    expect(await screen.findByText('Confirmed reference preview')).toBeInTheDocument();
    await waitFor(() => expect(confirmation).toHaveBeenLastCalledWith(true));
  });

  it('retains manual entry when a browse scope is available', async () => {
    const subjectRef = 'ta-18000000-0000-4000-8000-000000000002';
    server.use(
      http.get(`${apiBaseUrl}/api/evidence`, () =>
        HttpResponse.json(envelope({ items: [] })),
      ),
      http.get(`${apiBaseUrl}/api/evidence/${evidenceRef}`, () =>
        HttpResponse.json(
          envelope({
            evidenceRef,
            subjectRef,
            source: 'MANUAL',
            status: 'ACTIVE',
            recordedAt: '2026-09-05T00:00:00Z',
          }),
        ),
      ),
    );
    renderInput('evidence', { subjectRef });

    await userEvent.click(screen.getByText('Enter manually'));
    await userEvent.type(screen.getByLabelText('Reference'), evidenceRef);

    expect(await screen.findByText('Confirmed reference preview')).toBeInTheDocument();
  });
});
