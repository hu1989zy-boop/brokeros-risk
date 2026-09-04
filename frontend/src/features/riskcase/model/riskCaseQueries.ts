import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { AddNoteCommand } from '../api/riskCaseRepository';
import type { RiskCaseFilters } from '../api/riskCaseTypes';
import { useRiskCaseRepository } from './riskCaseContext';

export const riskCaseKeys = {
  all: ['risk-cases'] as const,
  list: (filters: RiskCaseFilters, page: number, size: number) =>
    [...riskCaseKeys.all, 'list', filters, page, size] as const,
  detail: (caseNumber: string) => [...riskCaseKeys.all, 'detail', caseNumber] as const,
  associations: (caseNumber: string) =>
    [...riskCaseKeys.all, 'associations', caseNumber] as const,
};

export function useRiskCaseList(filters: RiskCaseFilters, page: number, size: number) {
  const repository = useRiskCaseRepository();
  return useQuery({
    queryKey: riskCaseKeys.list(filters, page, size),
    queryFn: () => repository.list(filters, page, size),
    retry: false,
  });
}

export function useRiskCaseDetail(caseNumber: string) {
  const repository = useRiskCaseRepository();
  return useQuery({
    queryKey: riskCaseKeys.detail(caseNumber),
    queryFn: () => repository.get(caseNumber),
    retry: false,
  });
}

export function useRiskCaseAssociations(caseNumber: string) {
  const repository = useRiskCaseRepository();
  return useQuery({
    queryKey: riskCaseKeys.associations(caseNumber),
    queryFn: () => repository.getAssociations(caseNumber),
    retry: false,
  });
}

export function useAddRiskCaseNote() {
  const repository = useRiskCaseRepository();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (command: AddNoteCommand) => repository.addNote(command),
    onSuccess: async (_note, command) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: riskCaseKeys.detail(command.caseNumber) }),
        queryClient.invalidateQueries({ queryKey: riskCaseKeys.all }),
      ]);
    },
  });
}
