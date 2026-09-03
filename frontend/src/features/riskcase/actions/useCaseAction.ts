import { useMutation, useQueryClient } from '@tanstack/react-query';

import { ApiError } from '../../../core/api/errors';
import { riskCaseKeys } from '../model/riskCaseQueries';
import { useRiskCaseRepository } from '../model/riskCaseContext';
import type { CaseActionDescriptor, CaseActionExecutionContext } from './actionDescriptors';
import type { CaseActionValues } from './actionInputs';

export function useCaseAction(
  descriptor: CaseActionDescriptor,
  context: CaseActionExecutionContext,
) {
  const repository = useRiskCaseRepository();
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationFn: (values: CaseActionValues) => descriptor.execute(repository, context, values),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: riskCaseKeys.all });
    },
    onError: async (error) => {
      if (error instanceof ApiError && error.code === 'RISK_CASE_VERSION_CONFLICT') {
        await queryClient.refetchQueries({
          queryKey: riskCaseKeys.detail(context.caseNumber),
          type: 'active',
        });
      }
    },
  });

  return {
    run: mutation.mutateAsync,
    isPending: mutation.isPending,
    error: mutation.error,
    reset: mutation.reset,
  };
}
