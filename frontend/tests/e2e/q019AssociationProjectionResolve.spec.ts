import { expect, type Locator, type Page, test } from '@playwright/test';

const operatorUsername = process.env.E2E_OPERATOR_USERNAME ?? 'q016-operator';
const operatorPassword = process.env.E2E_OPERATOR_PASSWORD;
const caseNumber = process.env.E2E_Q019_CASE_NUMBER;
const decisionRef = process.env.E2E_Q019_DECISION_REF;
const actionRef = process.env.E2E_Q019_ACTION_REF;

test.skip(
  !operatorPassword || !caseNumber || !decisionRef || !actionRef,
  'Live credentials plus a seeded IN_REVIEW case and real decision/action refs are required.',
);

test('projection-backed association flow reaches resolve and close end to end', async ({
  page,
}) => {
  await loginAndOpenCase(page, caseNumber!);
  const panel = page.getByTestId('associations-panel');
  let version = await displayedVersion(page);

  await runPreviewAction(page, 'Associate decision', 'Decision reference', decisionRef!, {
    Reason: 'Q-019 live decision association verification.',
  });
  version += 1;
  await expectVersion(page, version);
  await expect(panel.getByText(decisionRef!, { exact: true })).toBeVisible();

  await runSelectAction(page, 'Select current decision', 'Associated decision', decisionRef!, {
    Reason: 'Q-019 live current-decision verification.',
  });
  version += 1;
  await expectVersion(page, version);
  await expect(panel.getByText('Current', { exact: true })).toBeVisible();

  await runPreviewAction(page, 'Associate action', 'Action reference', actionRef!, {
    Reason: 'Q-019 live action association verification.',
  });
  version += 1;
  await expectVersion(page, version);
  await expect(panel.getByText(actionRef!, { exact: true })).toBeVisible();

  await runTerminalAction(page, 'Resolve case', {
    'Resolution outcome': 'No Risk',
    'Resolution summary and reason': 'Q-019 projection-backed live resolution.',
    'Action references (optional)': actionRef!,
  });
  version += 1;
  await expectVersion(page, version);
  await expect(page.getByText('Resolved', { exact: true })).toBeVisible();

  await runTerminalAction(page, 'Close case', {
    Reason: 'Q-019 live closure after resolution.',
  });
  version += 1;
  await expectVersion(page, version);
  await expect(page.getByText('Closed', { exact: true })).toBeVisible();
});

async function loginAndOpenCase(page: Page, targetCaseNumber: string) {
  await page.goto('/cases');
  await page.getByRole('button', { name: 'Sign in with Keycloak' }).click();
  await page.locator('#username').fill(operatorUsername);
  await page.locator('#password').fill(operatorPassword!);
  await page.locator('#kc-login').click();
  await expect(page.getByRole('heading', { name: 'Risk cases' })).toBeVisible();

  const caseLink = page.getByRole('button', { name: targetCaseNumber, exact: true });
  while ((await caseLink.count()) === 0) {
    const next = page.getByRole('button', { name: 'Next' });
    if (await next.isDisabled()) {
      throw new Error('E2E_Q019_CASE_NUMBER was not found in the bounded Risk Case pages.');
    }
    await next.click();
  }
  await caseLink.click();
  await expect(page.getByRole('heading', { name: targetCaseNumber })).toBeVisible();
}

async function runPreviewAction(
  page: Page,
  actionLabel: string,
  referenceLabel: string,
  reference: string,
  values: Record<string, string>,
) {
  await page.getByRole('button', { name: actionLabel }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel(referenceLabel).fill(reference);
  await expect(dialog.getByText('Confirmed reference preview')).toBeVisible();
  await fillValues(dialog, page, values);
  await dialog.getByRole('button', { name: actionLabel }).click();
  await expect(page.getByText(`${actionLabel} completed.`)).toBeVisible();
}

async function runSelectAction(
  page: Page,
  actionLabel: string,
  selectLabel: string,
  reference: string,
  values: Record<string, string>,
) {
  await page.getByRole('button', { name: actionLabel }).click();
  const dialog = page.getByRole('dialog');
  await dialog.getByLabel(selectLabel).click();
  await page.getByText(new RegExp(`^${escapeRegExp(reference)}`)).last().click();
  await fillValues(dialog, page, values);
  await dialog.getByRole('button', { name: actionLabel }).click();
  await expect(page.getByText(`${actionLabel} completed.`)).toBeVisible();
}

async function runTerminalAction(
  page: Page,
  actionLabel: string,
  values: Record<string, string>,
) {
  await page.getByRole('button', { name: actionLabel }).click();
  const dialog = page.getByRole('dialog');
  await fillValues(dialog, page, values);
  await dialog.getByRole('button', { name: 'Review action' }).click();
  await expect(dialog.getByText('Confirm terminal case operation')).toBeVisible();
  await dialog
    .getByRole('button', { name: `Confirm ${actionLabel.toLowerCase()}` })
    .click();
  await expect(page.getByText(`${actionLabel} completed.`)).toBeVisible();
  await expect(dialog).toBeHidden();
}

async function fillValues(
  dialog: Locator,
  page: Page,
  values: Record<string, string>,
) {
  for (const [label, value] of Object.entries(values)) {
    const control = dialog.getByLabel(label);
    if ((await control.getAttribute('role')) === 'combobox') {
      await control.click();
      await page.getByText(value, { exact: true }).last().click();
    } else {
      await control.fill(value);
    }
  }
}

async function displayedVersion(page: Page): Promise<number> {
  const text = await page.getByText(/^Version \d+$/).first().textContent();
  const version = Number(text?.replace('Version ', ''));
  if (!Number.isInteger(version)) throw new Error(`Invalid displayed version: ${text}`);
  return version;
}

async function expectVersion(page: Page, version: number) {
  await expect(page.getByText(`Version ${version}`, { exact: true })).toBeVisible();
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
