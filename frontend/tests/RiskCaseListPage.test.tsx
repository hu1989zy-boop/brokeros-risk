import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { delay, http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { ApiClient, type AuthSession } from '../src/core/api/apiClient';
import { HttpRiskCaseRepository } from '../src/features/riskcase/api/riskCaseRepository';
import { RiskCaseRepositoryProvider } from '../src/features/riskcase/model/riskCaseContext';
import { RiskCaseListPage } from '../src/features/riskcase/ui/RiskCaseListPage';
import { envelope, failureEnvelope, riskCaseListPage } from './fixtures/riskCases';
import { apiBaseUrl, server } from './support/server';

function renderPage() {
  const auth: AuthSession = {
    getAccessToken: () => 'test-token',
    refreshAccessToken: async () => null,
    authenticationRequired: vi.fn(),
  };
  const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, auth));
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  function Providers({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <RiskCaseRepositoryProvider repository={repository}>{children}</RiskCaseRepositoryProvider>
      </QueryClientProvider>
    );
  }
  return render(
    <MemoryRouter initialEntries={['/cases']}>
      <Routes>
        <Route path="/cases" element={<RiskCaseListPage />} />
        <Route path="/cases/:caseNumber" element={<div>Opened case</div>} />
      </Routes>
    </MemoryRouter>,
    { wrapper: Providers },
  );
}

describe('RiskCaseListPage', () => {
  it('renders its loading state', () => {
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases`, async () => {
        await delay('infinite');
        return HttpResponse.json(envelope(riskCaseListPage));
      }),
    );
    renderPage();
    expect(screen.getByText('Loading risk cases')).toBeInTheDocument();
  });

  it('renders its empty state', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases`, () =>
        HttpResponse.json(envelope({ ...riskCaseListPage, items: [] })),
      ),
    );
    renderPage();
    expect(await screen.findByText('No risk cases match these filters.')).toBeInTheDocument();
  });

  it('renders a typed backend error and retry action', async () => {
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases`, () =>
        HttpResponse.json(failureEnvelope('SECURITY_DEPENDENCY_UNAVAILABLE', 'Security is unavailable'), {
          status: 503,
        }),
      ),
    );
    renderPage();
    expect(await screen.findByText('Security is unavailable')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('renders the TanStack table success state and opens a case', async () => {
    renderPage();
    expect(await screen.findByTestId('risk-case-table')).toBeInTheDocument();
    expect(screen.getByText('trading-account:demo-1001')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'RC-2026-000001' }));
    expect(await screen.findByText('Opened case')).toBeInTheDocument();
  });
});
