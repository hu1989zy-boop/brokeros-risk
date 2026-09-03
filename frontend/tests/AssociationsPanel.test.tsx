import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import {
  onCaseReferenceOptions,
  projectAssociations,
} from '../src/features/riskcase/model/associationProjection';
import { AssociationsPanel } from '../src/features/riskcase/ui/AssociationsPanel';
import { riskCaseDetail } from './fixtures/riskCases';

const view = {
  detail: { ...riskCaseDetail, currentDecisionRef: 'dec-18000000-0000-4000-8000-000000000001' },
  history: {
    entries: [
      entry(2, 'ATTACHED', 'ev-18000000-0000-4000-8000-000000000001'),
      entry(3, 'DECISION_ASSOCIATED', 'dec-18000000-0000-4000-8000-000000000001'),
      entry(4, 'ACTION_ASSOCIATED', 'act-18000000-0000-4000-8000-000000000001'),
      entry(5, 'OUTCOME_REFERENCED', 'act-18000000-0000-4000-8000-000000000001'),
    ],
    nextCursor: null,
  },
};

function entry(version: number, eventType: string, affectedRef: string) {
  return {
    version,
    eventType,
    affectedRef,
    actorRef: '16000000-0000-4000-8000-000000000001',
    occurredAt: '2026-09-03T00:00:00Z',
  };
}

describe('Q-018 association projection and panel', () => {
  it('builds decision and action on-case picker options from detail/history', () => {
    const options = onCaseReferenceOptions(view);

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

  it('renders bounded association evidence without claiming unavailable outcome refs', () => {
    render(<AssociationsPanel view={view} />);
    const panel = screen.getByTestId('associations-panel');

    expect(within(panel).getByText('Bounded association view')).toBeInTheDocument();
    expect(within(panel).getByText('ev-18000000-0000-4000-8000-000000000001')).toBeInTheDocument();
    expect(within(panel).getByText('Current')).toBeInTheDocument();
    expect(
      within(panel).getByText('Outcome reference recorded (outcome ref is not exposed by history)'),
    ).toBeInTheDocument();
  });

  it('hosts all six Group C operations and passes the selected descriptor upward', async () => {
    const onSelectAction = vi.fn();
    render(<AssociationsPanel view={view} onSelectAction={onSelectAction} />);
    const actions = screen.getByLabelText('Association actions');

    expect(within(actions).getAllByRole('button')).toHaveLength(6);
    await userEvent.click(within(actions).getByRole('button', { name: 'Associate decision' }));

    expect(onSelectAction).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'associateDecision' }),
    );
  });

  it('marks an action withdrawn when the latest visible action event withdraws it', () => {
    const projection = projectAssociations({
      ...view,
      history: {
        ...view.history,
        entries: [
          ...view.history.entries,
          entry(6, 'WITHDRAWN', 'act-18000000-0000-4000-8000-000000000001'),
        ],
      },
    });

    expect(projection.actions[0]).toMatchObject({ active: false, outcomeRecorded: true });
  });
});
