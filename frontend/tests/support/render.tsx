import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderOptions } from '@testing-library/react';
import type { PropsWithChildren, ReactElement } from 'react';

import type { RiskCaseRepository } from '../../src/features/riskcase/api/riskCaseRepository';
import { RiskCaseRepositoryProvider } from '../../src/features/riskcase/model/riskCaseContext';

export function renderWithRepository(
  ui: ReactElement,
  repository: RiskCaseRepository,
  options?: Omit<RenderOptions, 'wrapper'>,
) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: Infinity },
      mutations: { retry: false },
    },
  });
  function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <RiskCaseRepositoryProvider repository={repository}>{children}</RiskCaseRepositoryProvider>
      </QueryClientProvider>
    );
  }
  return { ...render(ui, { wrapper: Wrapper, ...options }), queryClient };
}
