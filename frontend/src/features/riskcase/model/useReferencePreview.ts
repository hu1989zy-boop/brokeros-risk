import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';

import { ApiError } from '../../../core/api/errors';
import { referencePatterns, type ReferenceKind } from '../actions/actionInputs';
import type { ReferencePreview } from '../api/referencePreview';
import { useReferencePreviewRepository } from './referencePreviewContext';

export type ReferencePreviewState =
  | { status: 'idle' }
  | { status: 'invalid-format' }
  | { status: 'loading' }
  | { status: 'valid'; preview: ReferencePreview }
  | { status: 'not-found'; message: string }
  | { status: 'forbidden'; message: string }
  | { status: 'error'; message: string };

const previewKeys = {
  byRef: (kind: ReferenceKind, reference: string) =>
    ['reference-preview', kind, reference] as const,
};

export function useReferencePreview(
  kind: ReferenceKind,
  rawReference: string | undefined,
): ReferencePreviewState {
  const repository = useReferencePreviewRepository();
  const reference = rawReference?.trim() ?? '';
  const validFormat = referencePatterns[kind].test(reference);
  const [debouncedReference, setDebouncedReference] = useState('');

  useEffect(() => {
    if (!validFormat) {
      setDebouncedReference('');
      return;
    }
    const timer = window.setTimeout(() => setDebouncedReference(reference), 300);
    return () => window.clearTimeout(timer);
  }, [reference, validFormat]);

  const query = useQuery({
    queryKey: previewKeys.byRef(kind, debouncedReference),
    queryFn: () => repository.get(kind, debouncedReference),
    enabled: validFormat && debouncedReference === reference,
    retry: false,
  });

  if (!reference) return { status: 'idle' };
  if (!validFormat) return { status: 'invalid-format' };
  if (debouncedReference !== reference || query.isPending) return { status: 'loading' };
  if (query.isSuccess && query.data.reference === reference) {
    return { status: 'valid', preview: query.data };
  }
  if (query.isSuccess) {
    return { status: 'error', message: 'The preview response did not match the requested reference.' };
  }
  if (query.error instanceof ApiError && query.error.httpStatus === 404) {
    return { status: 'not-found', message: 'The reference was not found.' };
  }
  if (query.error instanceof ApiError && query.error.httpStatus === 403) {
    return { status: 'forbidden', message: 'You are not authorized to preview this reference.' };
  }
  return { status: 'error', message: 'The reference could not be previewed.' };
}
