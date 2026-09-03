import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { CaseActionsBar } from '../src/features/riskcase/ui/CaseActionsBar';
import type { RiskCaseStatus } from '../src/features/riskcase/api/riskCaseTypes';

const expectedByStatus: Record<RiskCaseStatus, string[]> = {
  OPEN: ['Assign case', 'Change priority', 'Begin review', 'Cancel case'],
  IN_REVIEW: [
    'Assign case',
    'Change priority',
    'Mark action required',
    'Resolve case',
    'Cancel case',
  ],
  ACTION_REQUIRED: [
    'Assign case',
    'Change priority',
    'Return to review',
    'Resolve case',
    'Cancel case',
  ],
  RESOLVED: ['Close case', 'Resume resolved case'],
  CLOSED: ['Reopen closed case'],
  CANCELLED: [],
};

describe('CaseActionsBar status availability', () => {
  it.each(Object.entries(expectedByStatus))('%s renders only its lifecycle-valid actions', (status, labels) => {
    render(
      <CaseActionsBar
        status={status as RiskCaseStatus}
        onSelect={() => undefined}
      />,
    );
    const actionBar = screen.getByLabelText('Available case actions');
    expect(within(actionBar).queryAllByRole('button').map((button) => button.textContent)).toEqual(
      labels,
    );
  });

  it('passes the selected declarative descriptor to the shared action flow', async () => {
    const onSelect = vi.fn();
    render(<CaseActionsBar status="OPEN" onSelect={onSelect} />);

    await userEvent.click(screen.getByRole('button', { name: 'Begin review' }));

    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 'beginReview' }));
  });
});
