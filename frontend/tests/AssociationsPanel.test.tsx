import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { descriptorFor } from '../src/features/riskcase/actions/actionDescriptors';
import { onCaseReferenceOptions } from '../src/features/riskcase/model/associationProjection';
import { AssociationsPanel } from '../src/features/riskcase/ui/AssociationsPanel';
import { riskCaseAssociations } from './fixtures/riskCases';

describe('Q-019 authoritative association projection and panel', () => {
  it('builds every on-case picker from the backend projection', () => {
    const options = onCaseReferenceOptions(riskCaseAssociations);

    expect(options.associationEventRef).toEqual([
      {
        label:
          '18000000-0000-4000-8000-000000000002 — ev-18000000-0000-4000-8000-000000000001 (ATTACHED)',
        value: '18000000-0000-4000-8000-000000000002',
      },
    ]);
    expect(options.decisionRef).toEqual([
      {
        label: 'dec-18000000-0000-4000-8000-000000000001 (current)',
        value: 'dec-18000000-0000-4000-8000-000000000001',
      },
    ]);
    expect(options.actionRef).toEqual([
      {
        label: 'act-18000000-0000-4000-8000-000000000001',
        value: 'act-18000000-0000-4000-8000-000000000001',
      },
    ]);
  });

  it('renders evidence event, disposition, current decision, and exact outcome refs', () => {
    render(<AssociationsPanel associations={riskCaseAssociations} />);
    const panel = screen.getByTestId('associations-panel');

    expect(
      within(panel).getByText('ev-18000000-0000-4000-8000-000000000001'),
    ).toBeInTheDocument();
    expect(
      within(panel).getByText(/Attached · operator-review.*event 18000000/),
    ).toBeInTheDocument();
    expect(within(panel).getByText('Current')).toBeInTheDocument();
    expect(
      within(panel).getByText('Outcomes: aoc-18000000-0000-4000-8000-000000000001'),
    ).toBeInTheDocument();
    expect(within(panel).queryByText(/reconstructed only from/i)).not.toBeInTheDocument();
  });

  it('hosts all six Group C operations and passes the selected descriptor upward', async () => {
    const onSelectAction = vi.fn();
    render(
      <AssociationsPanel
        associations={riskCaseAssociations}
        onSelectAction={onSelectAction}
      />,
    );
    const actions = screen.getByLabelText('Association actions');

    expect(within(actions).getAllByRole('button')).toHaveLength(6);
    await userEvent.click(within(actions).getByRole('button', { name: 'Associate decision' }));

    expect(onSelectAction).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'associateDecision' }),
    );
  });

  it('uses an on-case picker for the evidence disposition target', () => {
    const target = descriptorFor('changeEvidenceDisposition').fields.find(
      (field) => field.name === 'associationEventRef',
    );

    expect(target).toMatchObject({ kind: 'on-case-select', required: true });
  });
});
