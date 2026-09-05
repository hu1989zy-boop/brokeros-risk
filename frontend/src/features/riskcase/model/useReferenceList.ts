import { useQuery } from '@tanstack/react-query';

import { useReferenceListRepository } from './referenceListContext';

export const referenceListKeys = {
  evidence: (subjectRef: string) => ['reference-list', 'evidence', subjectRef] as const,
  decisions: (subjectRef: string) => ['reference-list', 'decision', subjectRef] as const,
  actions: (decisionRef: string) => ['reference-list', 'action', decisionRef] as const,
  outcomes: (actionRef: string) => ['reference-list', 'actionOutcome', actionRef] as const,
};

export function useEvidenceList(subjectRef: string | undefined) {
  const repository = useReferenceListRepository();
  return useQuery({
    queryKey: referenceListKeys.evidence(subjectRef ?? ''),
    queryFn: () => repository.listEvidence(subjectRef!),
    enabled: Boolean(subjectRef),
    retry: false,
  });
}

export function useDecisionList(subjectRef: string | undefined) {
  const repository = useReferenceListRepository();
  return useQuery({
    queryKey: referenceListKeys.decisions(subjectRef ?? ''),
    queryFn: () => repository.listDecisions(subjectRef!),
    enabled: Boolean(subjectRef),
    retry: false,
  });
}

export function useActionList(decisionRef: string | undefined) {
  const repository = useReferenceListRepository();
  return useQuery({
    queryKey: referenceListKeys.actions(decisionRef ?? ''),
    queryFn: () => repository.listActions(decisionRef!),
    enabled: Boolean(decisionRef),
    retry: false,
  });
}

export function useOutcomeList(actionRef: string | undefined) {
  const repository = useReferenceListRepository();
  return useQuery({
    queryKey: referenceListKeys.outcomes(actionRef ?? ''),
    queryFn: () => repository.listOutcomes(actionRef!),
    enabled: Boolean(actionRef),
    retry: false,
  });
}
