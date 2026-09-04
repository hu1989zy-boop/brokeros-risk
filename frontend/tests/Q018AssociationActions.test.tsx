import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { ApiClient, type AuthSession } from '../src/core/api/apiClient';
import {
  associationActionIds,
  descriptorFor,
  type CaseActionDescriptor,
  type CaseActionId,
} from '../src/features/riskcase/actions/actionDescriptors';
import type {
  CaseActionFieldName,
  CaseActionFieldOption,
  CaseActionValues,
} from '../src/features/riskcase/actions/actionInputs';
import { useCaseAction } from '../src/features/riskcase/actions/useCaseAction';
import { HttpReferencePreviewRepository } from '../src/features/riskcase/api/referencePreview';
import { HttpRiskCaseRepository } from '../src/features/riskcase/api/riskCaseRepository';
import { ReferencePreviewRepositoryProvider } from '../src/features/riskcase/model/referencePreviewContext';
import { RiskCaseRepositoryProvider } from '../src/features/riskcase/model/riskCaseContext';
import {
  useRiskCaseAssociations,
  useRiskCaseDetail,
} from '../src/features/riskcase/model/riskCaseQueries';
import { CaseActionDialog } from '../src/features/riskcase/ui/CaseActionDialog';
import {
  caseNumber,
  envelope,
  failureEnvelope,
  riskCaseAssociations,
  riskCaseDetail,
} from './fixtures/riskCases';
import { apiBaseUrl, server } from './support/server';

const uuid = '18000000-0000-4000-8000-000000000001';
const evidenceRef = `ev-${uuid}`;
const decisionRef = `dec-${uuid}`;
const actionRef = `act-${uuid}`;
const outcomeRef = `aoc-${uuid}`;
const associationEventRef = '18000000-0000-4000-8000-000000000002';

interface AssociationScenario {
  id: Extract<CaseActionId, (typeof associationActionIds)[number]>;
  values: CaseActionValues;
  expectedBody: Record<string, unknown>;
  preview?: { kind: 'evidence' | 'decision' | 'action' | 'actionOutcome'; ref: string };
}

const scenarios: AssociationScenario[] = [
  {
    id: 'associateEvidence',
    values: { evidenceRef, source: 'operator-review', reason: 'Attach reviewed evidence.' },
    expectedBody: {
      evidenceRef,
      source: 'operator-review',
      reason: 'Attach reviewed evidence.',
      expectedVersion: 7,
    },
    preview: { kind: 'evidence', ref: evidenceRef },
  },
  {
    id: 'changeEvidenceDisposition',
    values: {
      associationEventRef,
      disposition: 'SUPERSEDED',
      replacementEvidenceRef: evidenceRef,
      source: 'operator-review',
      reason: 'Replace stale evidence.',
    },
    expectedBody: {
      disposition: 'SUPERSEDED',
      replacementEvidenceRef: evidenceRef,
      source: 'operator-review',
      reason: 'Replace stale evidence.',
      expectedVersion: 7,
    },
    preview: { kind: 'evidence', ref: evidenceRef },
  },
  {
    id: 'associateDecision',
    values: { decisionRef, reason: 'Attach reviewed decision.' },
    expectedBody: { decisionRef, reason: 'Attach reviewed decision.', expectedVersion: 7 },
    preview: { kind: 'decision', ref: decisionRef },
  },
  {
    id: 'selectDecision',
    values: { decisionRef, reason: 'Select current decision.' },
    expectedBody: { decisionRef, reason: 'Select current decision.', expectedVersion: 7 },
  },
  {
    id: 'associateAction',
    values: { actionRef, reason: 'Attach reviewed action.' },
    expectedBody: { actionRef, reason: 'Attach reviewed action.', expectedVersion: 7 },
    preview: { kind: 'action', ref: actionRef },
  },
  {
    id: 'referenceActionOutcome',
    values: { actionRef, outcomeRef, reason: 'Reference verified outcome.' },
    expectedBody: { outcomeRef, reason: 'Reference verified outcome.', expectedVersion: 7 },
    preview: { kind: 'actionOutcome', ref: outcomeRef },
  },
];

const onCaseOptions: Partial<Record<CaseActionFieldName, CaseActionFieldOption[]>> = {
  associationEventRef: [{ label: associationEventRef, value: associationEventRef }],
  decisionRef: [{ label: `${decisionRef} (current)`, value: decisionRef }],
  actionRef: [{ label: actionRef, value: actionRef }],
};

function authSession(): AuthSession {
  return {
    getAccessToken: vi.fn(() => 'synthetic-test-token'),
    refreshAccessToken: vi.fn(async () => null),
    authenticationRequired: vi.fn(),
  };
}

function renderAction(scenario: AssociationScenario, onSuccess = vi.fn()) {
  const client = new ApiClient(apiBaseUrl, authSession());
  const riskCases = new HttpRiskCaseRepository(client);
  const previews = new HttpReferencePreviewRepository(client);
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  function Providers({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <RiskCaseRepositoryProvider repository={riskCases}>
          <ReferencePreviewRepositoryProvider repository={previews}>
            {children}
          </ReferencePreviewRepositoryProvider>
        </RiskCaseRepositoryProvider>
      </QueryClientProvider>
    );
  }
  render(<ActionHarness scenario={scenario} onSuccess={onSuccess} />, { wrapper: Providers });
  return { onSuccess };
}

function ActionHarness({
  scenario,
  onSuccess,
}: {
  scenario: AssociationScenario;
  onSuccess: () => void;
}) {
  const query = useRiskCaseDetail(caseNumber);
  const associations = useRiskCaseAssociations(caseNumber);
  if (query.isPending || associations.isPending) return <div>Loading association action</div>;
  if (query.isError || associations.isError) return <div>Failed to load association action</div>;
  return (
    <div>
      <span>Version {query.data.detail.version}</span>
      <span>Projection version {associations.data.version}</span>
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
  const action = useCaseAction(descriptor, { caseNumber, expectedVersion });
  return (
    <CaseActionDialog
      descriptor={descriptor}
      expectedVersion={expectedVersion}
      submitting={action.isPending}
      onCaseOptions={onCaseOptions}
      onCancel={() => undefined}
      onSubmit={async (values) => {
        await action.run(values);
        onSuccess();
      }}
    />
  );
}

async function fillDialog(scenario: AssociationScenario) {
  const descriptor = descriptorFor(scenario.id);
  const dialog = await screen.findByRole('dialog');
  const user = userEvent.setup();
  for (const field of descriptor.fields) {
    const value = scenario.values[field.name];
    if (!value) continue;
    if (field.kind === 'select' || field.kind === 'on-case-select') {
      await user.click(within(dialog).getByLabelText(field.label));
      const configuredOptions =
        field.kind === 'on-case-select' ? onCaseOptions[field.name]! : field.options!;
      const option = configuredOptions.find((candidate) => candidate.value === value)!;
      await user.click(
        await screen.findByText(option.label, { selector: '.ant-select-item-option-content' }),
      );
    } else {
      fireEvent.change(within(dialog).getByLabelText(field.label), { target: { value } });
    }
  }
  if (scenario.preview) {
    await within(dialog).findByText('Confirmed reference preview');
  }
  return dialog;
}

function actionUrl(scenario: AssociationScenario) {
  return `${apiBaseUrl}${descriptorFor(scenario.id).path({ caseNumber }, scenario.values)}`;
}

function usePreviewSuccess(scenario: AssociationScenario) {
  if (!scenario.preview) return;
  const { kind, ref } = scenario.preview;
  const resource = {
    evidence: 'evidence',
    decision: 'decisions',
    action: 'actions',
    actionOutcome: 'action-outcomes',
  }[kind];
  server.use(
    http.get(`${apiBaseUrl}/api/${resource}/${ref}`, () =>
      HttpResponse.json(envelope(previewData(kind, ref))),
    ),
  );
}

function previewData(
  kind: NonNullable<AssociationScenario['preview']>['kind'],
  ref: string,
) {
  const common = { source: 'MANUAL', recordedAt: '2026-09-03T00:00:00Z' };
  if (kind === 'evidence') {
    return { evidenceRef: ref, subjectRef: 'subject:synthetic', status: 'ACTIVE', ...common };
  }
  if (kind === 'decision') {
    return { decisionRef: ref, subjectRef: 'subject:synthetic', ...common };
  }
  if (kind === 'action') {
    return { actionRef: ref, decisionRef, status: 'RECORDED', ...common };
  }
  return { actionOutcomeRef: ref, actionRef, ...common };
}

function successData(id: AssociationScenario['id'], version = 8) {
  if (id === 'associateEvidence' || id === 'changeEvidenceDisposition') {
    return {
      associationEventRef,
      eventType: id === 'associateEvidence' ? 'ATTACHED' : 'SUPERSEDED',
      evidenceRef,
      replacementEvidenceRef: id === 'associateEvidence' ? null : evidenceRef,
      version,
      actorRef: '16000000-0000-4000-8000-000000000001',
      occurredAt: '2026-09-03T00:00:00Z',
    };
  }
  if (id === 'associateDecision') return { decisionRef, version };
  if (id === 'selectDecision') return { ...riskCaseDetail, currentDecisionRef: decisionRef, version };
  return {
    eventType: id === 'associateAction' ? 'ACTION_ASSOCIATED' : 'OUTCOME_REFERENCED',
    actionRef,
    decisionRef,
    outcomeRef: id === 'associateAction' ? null : outcomeRef,
    version,
    actorRef: '16000000-0000-4000-8000-000000000001',
    occurredAt: '2026-09-03T00:00:00Z',
  };
}

describe('Q-018 association action registry and runner', () => {
  it('registers exactly the six approved Group C operations', () => {
    expect(scenarios.map(({ id }) => id)).toEqual(associationActionIds);
  });

  it.each(scenarios)('$id sends the exact versioned body and no actor identity', async (scenario) => {
    usePreviewSuccess(scenario);
    let capturedBody: unknown;
    server.use(
      http.post(actionUrl(scenario), async ({ request }) => {
        capturedBody = await request.json();
        return HttpResponse.json(envelope(successData(scenario.id)), { status: 201 });
      }),
    );
    const { onSuccess } = renderAction(scenario);
    const dialog = await fillDialog(scenario);
    await userEvent.click(within(dialog).getByRole('button', { name: descriptorFor(scenario.id).label }));

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(capturedBody).toEqual(scenario.expectedBody);
    expect(capturedBody).not.toHaveProperty('actorRef');
  });

  it('refetches the authoritative projection after an association write', async () => {
    const scenario = scenarios.find(({ id }) => id === 'associateDecision')!;
    let projectionCalls = 0;
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber/associations`, () => {
        projectionCalls += 1;
        return HttpResponse.json(envelope({
          ...riskCaseAssociations,
          version: projectionCalls === 1 ? 7 : 8,
        }));
      }),
      http.get(`${apiBaseUrl}/api/decisions/${decisionRef}`, () =>
        HttpResponse.json(envelope(previewData('decision', decisionRef))),
      ),
      http.post(actionUrl(scenario), () =>
        HttpResponse.json(envelope(successData(scenario.id)), { status: 201 }),
      ),
    );
    const { onSuccess } = renderAction(scenario);
    const dialog = await fillDialog(scenario);
    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Associate decision' }),
    );

    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.getByText('Projection version 8')).toBeInTheDocument());
    expect(projectionCalls).toBeGreaterThanOrEqual(2);
  });

  it.each(scenarios)('$id disables submission while its request is pending', async (scenario) => {
    usePreviewSuccess(scenario);
    server.use(
      http.post(actionUrl(scenario), async () => {
        await delay(150);
        return HttpResponse.json(envelope(successData(scenario.id)), { status: 201 });
      }),
    );
    const { onSuccess } = renderAction(scenario);
    const dialog = await fillDialog(scenario);
    await userEvent.click(within(dialog).getByRole('button', { name: descriptorFor(scenario.id).label }));
    const submit = within(dialog)
      .getAllByRole('button')
      .find((button) => button.classList.contains('ant-btn-primary'))!;
    await waitFor(() => expect(submit).toBeDisabled());
    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
  });

  it.each(scenarios)('$id blocks invalid or missing required input', async (scenario) => {
    let postCalls = 0;
    server.use(
      http.post(actionUrl(scenario), () => {
        postCalls += 1;
        return HttpResponse.json(envelope(successData(scenario.id)), { status: 201 });
      }),
    );
    renderAction(scenario);
    const dialog = await screen.findByRole('dialog');
    const descriptor = descriptorFor(scenario.id);
    const submit = within(dialog).getByRole('button', { name: descriptor.label });
    if (descriptor.fields.some((field) => field.kind === 'reference' && field.required)) {
      expect(submit).toBeDisabled();
    } else {
      await waitFor(() => expect(submit).toBeEnabled());
      await userEvent.click(submit);
      const required = descriptor.fields.find((field) => field.required)!;
      expect(
        await within(dialog).findByText(`Enter ${required.label.toLowerCase()}.`),
      ).toBeInTheDocument();
    }
    expect(postCalls).toBe(0);
  });

  it.each(scenarios)('$id surfaces an ordinary backend ResultCode', async (scenario) => {
    usePreviewSuccess(scenario);
    server.use(
      http.post(actionUrl(scenario), () =>
        HttpResponse.json(
          failureEnvelope('RISK_CASE_INVARIANT_VIOLATION', 'Rejected by backend'),
          { status: 422 },
        ),
      ),
    );
    renderAction(scenario);
    const dialog = await fillDialog(scenario);
    await userEvent.click(within(dialog).getByRole('button', { name: descriptorFor(scenario.id).label }));
    expect(
      await within(dialog).findByText('The operation does not satisfy the current case requirements.'),
    ).toBeInTheDocument();
  });

  it.each(scenarios)('$id surfaces a typed 403', async (scenario) => {
    usePreviewSuccess(scenario);
    server.use(
      http.post(actionUrl(scenario), () =>
        HttpResponse.json(failureEnvelope('AUTHORIZATION_DENIED', 'Forbidden'), { status: 403 }),
      ),
    );
    renderAction(scenario);
    const dialog = await fillDialog(scenario);
    await userEvent.click(within(dialog).getByRole('button', { name: descriptorFor(scenario.id).label }));
    expect(
      await within(dialog).findByText('You are not authorized to perform this case operation.'),
    ).toBeInTheDocument();
  });

  it.each(scenarios)('$id reloads on conflict and preserves input', async (scenario) => {
    usePreviewSuccess(scenario);
    let detailCalls = 0;
    let projectionCalls = 0;
    let postCalls = 0;
    const submittedVersions: number[] = [];
    server.use(
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber`, () => {
        detailCalls += 1;
        return HttpResponse.json(
          envelope(detailCalls === 1 ? riskCaseDetail : { ...riskCaseDetail, version: 8 }),
        );
      }),
      http.get(`${apiBaseUrl}/api/risk-cases/:caseNumber/associations`, () => {
        projectionCalls += 1;
        return HttpResponse.json(envelope(riskCaseAssociations));
      }),
      http.post(actionUrl(scenario), async ({ request }) => {
        postCalls += 1;
        const body = (await request.json()) as { expectedVersion: number };
        submittedVersions.push(body.expectedVersion);
        if (postCalls === 1) {
          return HttpResponse.json(
            failureEnvelope('RISK_CASE_VERSION_CONFLICT', 'Concurrent update'),
            { status: 409 },
          );
        }
        return HttpResponse.json(envelope(successData(scenario.id, 9)), { status: 201 });
      }),
    );
    const { onSuccess } = renderAction(scenario);
    const dialog = await fillDialog(scenario);
    await userEvent.click(within(dialog).getByRole('button', { name: descriptorFor(scenario.id).label }));

    expect(
      await within(dialog).findByText(/latest version was reloaded; review your preserved input/),
    ).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText('Version 8')).toBeInTheDocument());
    await waitFor(() => expect(projectionCalls).toBeGreaterThanOrEqual(2));
    expect(within(dialog).getByLabelText('Reason')).toHaveValue(scenario.values.reason);
    await userEvent.click(within(dialog).getByRole('button', { name: descriptorFor(scenario.id).label }));
    await waitFor(() => expect(onSuccess).toHaveBeenCalledTimes(1));
    expect(submittedVersions).toEqual([7, 8]);
  });
});
