import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { ApiError } from '../src/core/api/errors';
import { AddNoteDialog } from '../src/features/riskcase/ui/AddNoteDialog';

function renderDialog({
  submitting = false,
  onSubmit = vi.fn(async () => undefined),
}: {
  submitting?: boolean;
  onSubmit?: (content: string, expectedVersion: number) => Promise<void>;
} = {}) {
  const onVersionConflict = vi.fn(async () => undefined);
  render(
    <AddNoteDialog
      open
      expectedVersion={7}
      submitting={submitting}
      onCancel={vi.fn()}
      onSubmit={onSubmit}
      onVersionConflict={onVersionConflict}
    />,
  );
  return { onSubmit, onVersionConflict, dialog: screen.getByRole('dialog') };
}

describe('AddNoteDialog', () => {
  it('keeps an empty note client-side while the backend remains authoritative', async () => {
    const { dialog, onSubmit } = renderDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add note' }));

    expect(await within(dialog).findByText('Enter an investigation note.')).toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('renders its submitting state', () => {
    const { dialog } = renderDialog({ submitting: true });
    expect(within(dialog).getByRole('button', { name: /Add note/ })).toBeDisabled();
    expect(within(dialog).getByRole('textbox', { name: 'Investigation note' })).toBeDisabled();
  });

  it('surfaces an ordinary backend ResultCode error without closing', async () => {
    const onSubmit = vi.fn(async () => {
      throw new ApiError(
        'RISK_CASE_INVARIANT_VIOLATION',
        'Risk case invariant is violated',
        422,
      );
    });
    const { dialog } = renderDialog({ onSubmit });
    await userEvent.type(within(dialog).getByLabelText('Investigation note'), 'Objective note.');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add note' }));

    expect(await within(dialog).findByText('Risk case invariant is violated')).toBeInTheDocument();
    expect(dialog).toBeInTheDocument();
  });
});
