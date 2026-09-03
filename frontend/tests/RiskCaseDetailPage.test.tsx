import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { ApiClient, type AuthSession } from '../src/core/api/apiClient';
import { HttpRiskCaseRepository } from '../src/features/riskcase/api/riskCaseRepository';
import { RiskCaseRepositoryProvider } from '../src/features/riskcase/model/riskCaseContext';
import { RiskCaseDetailPage } from '../src/features/riskcase/ui/RiskCaseDetailPage';
import {
  caseNumber,
  envelope,
  failureEnvelope,
  riskCaseDetail,
  riskCaseHistory,
} from './fixtures/riskCases';
import { apiBaseUrl, server } from './support/server';

function renderPage() {
  const auth: AuthSession = {
    getAccessToken: () => 'test-token',
    refreshAccessToken: async () => null,
    authenticationRequired: vi.fn(),
  };
  const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, auth));
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  function Providers({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <RiskCaseRepositoryProvider repository={repository}>{children}</RiskCaseRepositoryProvider>
      </QueryClientProvider>
    );
  }
  return render(
    <MemoryRouter initialEntries={[`/cases/${caseNumber}`]}>
      <Routes>
        <Route path="/cases/:caseNumber" element={<RiskCaseDetailPage />} />
      </Routes>
    </MemoryRouter>,
    { wrapper: Providers },
  );
}

async function openNoteDialog() {
  await screen.findByRole('heading', { name: caseNumber });
  await userEvent.click(screen.getByRole('button', { name: 'Add note' }));
  return screen.getByRole('dialog');
}

describe('RiskCaseDetailPage', () => {
  it('renders its loading state', () => {
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber`, async () => {
        await delay('infinite');
        return HttpResponse.json(envelope(riskCaseDetail));
      }),
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber/history`, async () => {
        await delay('infinite');
        return HttpResponse.json(envelope(riskCaseHistory));
      }),
    );
    renderPage();
    expect(screen.getByText('Loading risk case detail')).toBeInTheDocument();
  });

  it('renders its error state', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber`, () =>
        HttpResponse.json(failureEnvelope('RISK_CASE_NOT_FOUND', 'Risk case was not found'), {
          status: 404,
        }),
      ),
    );
    renderPage();
    expect(await screen.findByText('Risk case was not found')).toBeInTheDocument();
  });

  it('renders detail, history, and association references', async () => {
    renderPage();
    expect(await screen.findByRole('heading', { name: caseNumber })).toBeInTheDocument();
    expect(screen.getByText('Review a bounded demonstration account event.')).toBeInTheDocument();
    expect(screen.getAllByText(/decision:demo-5001/).length).toBeGreaterThan(0);
    expect(screen.getByText('v7 · Decision Associated')).toBeInTheDocument();
    expect(screen.getAllByText('17000000-0000-4000-8000-000000000000').length).toBeGreaterThan(0);
    const associationCard = screen
      .getByText('Association references in history')
      .closest<HTMLElement>('.ant-card')!;
    expect(within(associationCard).queryByText(/Note:/)).not.toBeInTheDocument();
  });

  it('binds note correction to the selected note reference', async () => {
    let capturedBody: unknown;
    let capturedNoteRef: string | undefined;
    server.use(
      http.post(
        `${apiBaseUrl}/api/risk-cases/:caseNumber/notes/:noteRef/corrections`,
        async ({ params, request }) => {
          capturedNoteRef = params.noteRef as string;
          capturedBody = await request.json();
          return HttpResponse.json(
            envelope({
              noteRef: '17000000-0000-4000-8000-000000000001',
              supersedesNoteRef: params.noteRef,
              version: 8,
              createdByRef: '16000000-0000-4000-8000-000000000001',
              createdAt: '2026-09-02T10:00:00Z',
            }),
          );
        },
      ),
    );
    renderPage();
    await screen.findByRole('heading', { name: caseNumber });

    await userEvent.click(screen.getByRole('button', { name: 'Correct' }));
    const dialog = screen.getByRole('dialog');
    await userEvent.type(
      within(dialog).getByLabelText('Corrected note'),
      'Corrected after evidence review.',
    );
    await userEvent.click(within(dialog).getByRole('button', { name: 'Correct note' }));

    expect(await screen.findByText(/was corrected/)).toBeInTheDocument();
    expect(capturedNoteRef).toBe('17000000-0000-4000-8000-000000000000');
    expect(capturedBody).toEqual({
      content: 'Corrected after evidence review.',
      expectedVersion: 7,
    });
  });

  it('renders empty history and association states', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber/history`, () =>
        HttpResponse.json(envelope({ entries: [], nextCursor: null })),
      ),
    );
    renderPage();
    expect(await screen.findByText('No association reference events recorded.')).toBeInTheDocument();
    expect(screen.getByText('No history entries recorded.')).toBeInTheDocument();
  });

  it('submits a note successfully with the displayed expected version', async () => {
    let requestBody: unknown;
    server.use(
      http.post(`${apiBaseUrl}/api/risk-cases/:caseNumber/notes`, async ({ request }) => {
        requestBody = await request.json();
        return HttpResponse.json(
          envelope({
            noteRef: '17000000-0000-4000-8000-000000000001',
            supersedesNoteRef: null,
            version: 8,
            createdByRef: '16000000-0000-4000-8000-000000000001',
            createdAt: '2026-09-02T10:00:00Z',
          }),
          { status: 201 },
        );
      }),
    );
    renderPage();
    const dialog = await openNoteDialog();
    await userEvent.type(within(dialog).getByLabelText('Investigation note'), 'Objective review note.');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add note' }));

    expect(await screen.findByText(/was added/)).toBeInTheDocument();
    expect(requestBody).toEqual({ content: 'Objective review note.', expectedVersion: 7 });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('surfaces a version conflict, reloads, and keeps the note for review', async () => {
    let detailCalls = 0;
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber`, () => {
        detailCalls += 1;
        return HttpResponse.json(
          envelope(detailCalls === 1 ? riskCaseDetail : { ...riskCaseDetail, version: 8 }),
        );
      }),
      http.post(`${apiBaseUrl}/api/risk-cases/:caseNumber/notes`, () =>
        HttpResponse.json(
          failureEnvelope('RISK_CASE_VERSION_CONFLICT', 'Risk case version conflicts with current state'),
          { status: 409 },
        ),
      ),
    );
    renderPage();
    const dialog = await openNoteDialog();
    const textarea = within(dialog).getByLabelText('Investigation note');
    fireEvent.change(textarea, { target: { value: 'Keep this note during conflict handling.' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add note' }));

    expect(await within(dialog).findByText(/changed while you were editing/)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('Version 8')).toBeInTheDocument());
    expect(textarea).toHaveValue('Keep this note during conflict handling.');
    expect(detailCalls).toBeGreaterThanOrEqual(2);
  });
});
