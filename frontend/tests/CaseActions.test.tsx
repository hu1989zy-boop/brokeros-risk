import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { ApiClient, type AuthSession } from '../src/core/api/apiClient';
import {
  caseActionIds,
  descriptorFor,
  type CaseActionDescriptor,
  type CaseActionId,
} from '../src/features/riskcase/actions/actionDescriptors';
import type { CaseActionValues } from '../src/features/riskcase/actions/actionInputs';
import { useCaseAction } from '../src/features/riskcase/actions/useCaseAction';
import { HttpRiskCaseRepository } from '../src/features/riskcase/api/riskCaseRepository';
import { RiskCaseRepositoryProvider } from '../src/features/riskcase/model/riskCaseContext';
import { useRiskCaseDetail } from '../src/features/riskcase/model/riskCaseQueries';
import { CaseActionDialog } from '../src/features/riskcase/ui/CaseActionDialog';
import {
  caseNumber,
  envelope,
  failureEnvelope,
  riskCaseDetail,
  riskCaseHistory,
  riskCaseNote,
  riskCaseResolution,
} from './fixtures/riskCases';
import { apiBaseUrl, server } from './support/server';

const actorRef = '16000000-0000-4000-8000-000000000001';
const originalNoteRef = '17000000-0000-4000-8000-000000000000';

interface ActionScenario {
  id: CaseActionId;
  values: CaseActionValues;
  expectedBody: Record<string, unknown>;
  preservedLabel: string;
  preservedValue: string;
}

const scenarios: ActionScenario[] = [
  scenario('assign', { assigneeRef: actorRef, reason: 'Assign for review.' }),
  scenario('changePriority', { priority: 'CRITICAL', reason: 'Escalate priority.' }),
  scenario('beginReview', { reason: 'Review has started.' }),
  scenario('markActionRequired', { reason: 'Remediation is required.' }),
  scenario('returnToReview', { reason: 'Return after remediation.' }),
  scenario('resolve', {
    outcome: 'NO_RISK',
    resolutionSummary: 'No risk after bounded review.',
    evidenceRefs: 'ev-18000000-0000-4000-8000-000000000001',
    actionRefs: 'act-19000000-0000-4000-8000-000000000001',
  }),
  scenario('close', { reason: 'Close completed review.' }),
  scenario('cancel', {
    reason: 'Duplicate investigation.',
    duplicateCaseNumber: 'RC-20000000-0000-4000-8000-000000000001',
  }),
  scenario('resume', { reason: 'New information received.', assigneeRef: actorRef }),
  scenario('reopen', { reason: 'Closed case needs review.', assigneeRef: actorRef }),
  scenario('correctNote', { content: 'Complete corrected investigation note.' }),
];

function scenario(id: CaseActionId, values: CaseActionValues): ActionScenario {
  const descriptor = descriptorFor(id);
  const preservedField = descriptor.fields.find(
    (field) => field.kind === 'text' || field.kind === 'textarea',
  )!;
  const bodyValues: Record<string, unknown> = {};
  for (const [name, value] of Object.entries(values)) {
    if (name === 'evidenceRefs' || name === 'actionRefs') {
      bodyValues[name] = value ? [value] : [];
    } else {
      bodyValues[name] = value;
    }
  }
  if (id === 'resolve') {
    bodyValues.evidenceRefs ??= [];
    bodyValues.actionRefs ??= [];
  }
  return {
    id,
    values,
    expectedBody: { ...bodyValues, expectedVersion: 7 },
    preservedLabel: preservedField.label,
    preservedValue: values[preservedField.name]!,
  };
}

function authSession(): AuthSession {
  return {
    getAccessToken: vi.fn(() => 'test-token'),
    refreshAccessToken: vi.fn(async () => null),
    authenticationRequired: vi.fn(),
  };
}

function renderAction(scenarioUnderTest: ActionScenario, onSuccess = vi.fn()) {
  const repository = new HttpRiskCaseRepository(new ApiClient(apiBaseUrl, authSession()));
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  function Providers({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <RiskCaseRepositoryProvider repository={repository}>{children}</RiskCaseRepositoryProvider>
      </QueryClientProvider>
    );
  }
  render(<ActionHarness scenario={scenarioUnderTest} onSuccess={onSuccess} />, {
    wrapper: Providers,
  });
  return { onSuccess, queryClient };
}

function ActionHarness({
  scenario,
  onSuccess,
}: {
  scenario: ActionScenario;
  onSuccess: () => void;
}) {
  const query = useRiskCaseDetail(caseNumber);
  if (query.isPending) return <div>Loading action</div>;
  if (query.isError) return <div>Failed to load action</div>;
  return (
    <div>
      <span>Version {query.data.detail.version}</span>
      <ActionFlow
        descriptor={descriptorFor(scenario.id)}
        expectedVersion={query.data.detail.version}
        onSuccess={onSuccess}
      />
    </div>
  );
}

function ActionFlow({
  descriptor,
  expectedVersion,
  onSuccess,
}: {
  descriptor: CaseActionDescriptor;
  expectedVersion: number;
  onSuccess: () => void;
}) {
  const action = useCaseAction(descriptor, {
    caseNumber,
    expectedVersion,
    noteRef: descriptor.id === 'correctNote' ? originalNoteRef : undefined,
  });
  return (
    <CaseActionDialog
      descriptor={descriptor}
      expectedVersion={expectedVersion}
      submitting={action.isPending}
      onCancel={() => undefined}
      onSubmit={async (values) => {
        await action.run(values);
        onSuccess();
      }}
    />
  );
}

async function fillAndSubmit(actionScenario: ActionScenario) {
  const descriptor = descriptorFor(actionScenario.id);
  const dialog = await screen.findByRole('dialog');
  const user = userEvent.setup();
  for (const field of descriptor.fields) {
    const value = actionScenario.values[field.name];
    if (!value) continue;
    if (field.kind === 'select') {
      await user.click(within(dialog).getByLabelText(field.label));
      const option = field.options!.find((candidate) => candidate.value === value)!;
      await user.click(
        await screen.findByText(option.label, { selector: '.ant-select-item-option-content' }),
      );
    } else {
      fireEvent.change(within(dialog).getByLabelText(field.label), { target: { value } });
    }
  }
  await submitCurrentValues(dialog, descriptor);
  return dialog;
}

async function submitCurrentValues(dialog: HTMLElement, descriptor: CaseActionDescriptor) {
  const user = userEvent.setup();
  await user.click(
    within(dialog).getByRole('button', {
      name: descriptor.terminal ? 'Review action' : descriptor.label,
    }),
  );
  if (descriptor.terminal) {
    await user.click(
      await within(dialog).findByRole('button', {
        name: `Confirm ${descriptor.label.toLowerCase()}`,
      }),
    );
  }
}

function actionUrl(actionScenario: ActionScenario) {
  return `${apiBaseUrl}${descriptorFor(actionScenario.id).path({
    caseNumber,
    noteRef: actionScenario.id === 'correctNote' ? originalNoteRef : undefined,
  })}`;
}

function successData(id: CaseActionId) {
  if (id === 'resolve') return riskCaseResolution;
  if (id === 'correctNote') return { ...riskCaseNote, supersedesNoteRef: originalNoteRef };
  return { ...riskCaseDetail, version: 8 };
}

describe('Q-017 shared case action flow', () => {
  it('registers exactly the 11 approved V1 operations', () => {
    expect(scenarios.map(({ id }) => id)).toEqual(caseActionIds);
  });

  it.each(scenarios)('$id sends the exact versioned request and handles success', async (actionScenario) => {
    let capturedBody: unknown;
    server.use(
      http.post(actionUrl(actionScenario), async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(envelope(successData(actionScenario.id)));
      }),
    );
    const { onSuccess } = renderAction(actionScenario);

    await fillAndSubmit(actionScenario);

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(capturedBody).toEqual(actionScenario.expectedBody);
    expect(capturedBody).not.toHaveProperty('actorRef');
  });

  it.each(scenarios)('$id exposes pending state while the request is in flight', async (actionScenario) => {
    server.use(
      http.post(actionUrl(actionScenario), async () => {
        await delay(150);
        return HttpResponse.json(envelope(successData(actionScenario.id)));
      }),
    );
    const { onSuccess } = renderAction(actionScenario);

    const dialog = await fillAndSubmit(actionScenario);
    const submitButton = within(dialog)
      .getAllByRole('button')
      .find((button) => button.classList.contains('ant-btn-primary'))!;
    await waitFor(() => expect(submitButton).toBeDisabled());
    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
  });

  it.each(scenarios)('$id validates required input before sending', async (actionScenario) => {
    let postCalls = 0;
    server.use(
      http.post(actionUrl(actionScenario), () => {
        postCalls += 1;
        return HttpResponse.json(envelope(successData(actionScenario.id)));
      }),
    );
    renderAction(actionScenario);
    const descriptor = descriptorFor(actionScenario.id);
    const dialog = await screen.findByRole('dialog');

    await userEvent.click(
      within(dialog).getByRole('button', {
        name: descriptor.terminal ? 'Review action' : descriptor.label,
      }),
    );

    const requiredField = descriptor.fields.find((field) => field.required)!;
    expect(
      await within(dialog).findByText(`Enter ${requiredField.label.toLowerCase()}.`),
    ).toBeInTheDocument();
    expect(postCalls).toBe(0);
  });

  it.each(scenarios)('$id shows the backend lifecycle ResultCode', async (actionScenario) => {
    server.use(
      http.post(actionUrl(actionScenario), () =>
        HttpResponse.json(
          failureEnvelope('RISK_CASE_INVALID_TRANSITION', 'Backend transition rejected'),
          { status: 409 },
        ),
      ),
    );
    renderAction(actionScenario);

    const dialog = await fillAndSubmit(actionScenario);

    expect(
      await within(dialog).findByText(
        'The case is no longer in a state that allows this operation.',
      ),
    ).toBeInTheDocument();
  });

  it.each(scenarios)('$id shows the typed 403 authorization fallback', async (actionScenario) => {
    server.use(
      http.post(actionUrl(actionScenario), () =>
        HttpResponse.json(failureEnvelope('AUTHORIZATION_DENIED', 'Forbidden by policy'), {
          status: 403,
        }),
      ),
    );
    renderAction(actionScenario);

    const dialog = await fillAndSubmit(actionScenario);

    expect(
      await within(dialog).findByText(
        'You are not authorized to perform this case operation.',
      ),
    ).toBeInTheDocument();
  });

  it.each(scenarios)(
    '$id reloads on version conflict and preserves operator input',
    async (actionScenario) => {
      let detailCalls = 0;
      let postCalls = 0;
      const submittedVersions: number[] = [];
      server.use(
        http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber`, () => {
          detailCalls += 1;
          return HttpResponse.json(
            envelope(detailCalls === 1 ? riskCaseDetail : { ...riskCaseDetail, version: 8 }),
          );
        }),
        http.post(actionUrl(actionScenario), async ({ request }) => {
          postCalls += 1;
          const body = (await request.json()) as { expectedVersion: number };
          submittedVersions.push(body.expectedVersion);
          if (postCalls === 1) {
            return HttpResponse.json(
              failureEnvelope('RISK_CASE_VERSION_CONFLICT', 'Concurrent update'),
              { status: 409 },
            );
          }
          return HttpResponse.json(envelope(successData(actionScenario.id)));
        }),
      );
      const { onSuccess } = renderAction(actionScenario);

      const dialog = await fillAndSubmit(actionScenario);

      expect(
        await within(dialog).findByText(/latest version was reloaded; review your preserved input/),
      ).toBeInTheDocument();
      await waitFor(() => expect(screen.getByText('Version 8')).toBeInTheDocument());
      expect(within(dialog).getByLabelText(actionScenario.preservedLabel)).toHaveValue(
        actionScenario.preservedValue,
      );
      expect(detailCalls).toBeGreaterThanOrEqual(2);
      await submitCurrentValues(dialog, descriptorFor(actionScenario.id));
      await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
      expect(submittedVersions).toEqual([7, 8]);
    },
  );
});
