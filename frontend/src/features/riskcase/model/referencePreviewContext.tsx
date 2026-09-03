import { createContext, type PropsWithChildren, useContext } from 'react';

import type { ReferencePreviewRepository } from '../api/referencePreview';

const ReferencePreviewContext = createContext<ReferencePreviewRepository | null>(null);

export function ReferencePreviewRepositoryProvider({
  repository,
  children,
}: PropsWithChildren<{ repository: ReferencePreviewRepository }>) {
  return (
    <ReferencePreviewContext.Provider value={repository}>
      {children}
    </ReferencePreviewContext.Provider>
  );
}

export function useReferencePreviewRepository(): ReferencePreviewRepository {
  const repository = useContext(ReferencePreviewContext);
  if (!repository) {
    throw new Error('ReferencePreviewRepositoryProvider is missing');
  }
  return repository;
}
