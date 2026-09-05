import { createContext, type PropsWithChildren, useContext } from 'react';

import type { ReferenceListRepository } from '../api/referenceList';

const ReferenceListContext = createContext<ReferenceListRepository | null>(null);

export function ReferenceListRepositoryProvider({
  repository,
  children,
}: PropsWithChildren<{ repository: ReferenceListRepository }>) {
  return (
    <ReferenceListContext.Provider value={repository}>
      {children}
    </ReferenceListContext.Provider>
  );
}

export function useReferenceListRepository(): ReferenceListRepository {
  const repository = useContext(ReferenceListContext);
  if (!repository) {
    throw new Error('ReferenceListRepositoryProvider is missing');
  }
  return repository;
}
