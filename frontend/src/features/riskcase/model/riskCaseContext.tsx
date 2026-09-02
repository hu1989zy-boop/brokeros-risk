import { createContext, type PropsWithChildren, useContext } from 'react';

import type { RiskCaseRepository } from '../api/riskCaseRepository';

const RepositoryContext = createContext<RiskCaseRepository | null>(null);

export function RiskCaseRepositoryProvider({
  repository,
  children,
}: PropsWithChildren<{ repository: RiskCaseRepository }>) {
  return <RepositoryContext.Provider value={repository}>{children}</RepositoryContext.Provider>;
}

export function useRiskCaseRepository(): RiskCaseRepository {
  const repository = useContext(RepositoryContext);
  if (!repository) {
    throw new Error('RiskCaseRepositoryProvider is missing');
  }
  return repository;
}
