import { expect, type Page, test } from '@playwright/test';

const operatorUsername = process.env.E2E_OPERATOR_USERNAME ?? 'q016-operator';
const operatorPassword = process.env.E2E_OPERATOR_PASSWORD;
const caseNumber = process.env.E2E_Q017_CASE_NUMBER;
const assigneeRef =
  process.env.E2E_Q017_ASSIGNEE_REF ?? '16000000-0000-4000-8000-000000000001';

test.skip(
  !operatorPassword || !caseNumber,
  'Live Keycloak credentials and a seeded OPEN Q-017 case are required via E2E_OPERATOR_PASSWORD and E2E_Q017_CASE_NUMBER.',
);

test('seeded case: assign → begin review → priority → resolve → close', async ({ page }) => {
  await loginAndOpenCase(page, caseNumber!);
  await expect(page.getByText('Open', { exact: true })).toBeVisible();
  let version = await displayedVersion(page);

  await runAction(page, 'Assign case', {
    'Assignee reference': assigneeRef,
    Reason: 'Q-017 live assignment verification.',
  });
  version += 1;
  await expectVersionAndAudit(page, version, 'Assignment');

  await runAction(page, 'Begin review', { Reason: 'Q-017 live review verification.' });
  version += 1;
  await expect(page.getByText('In Review', { exact: true })).toBeVisible();
  await expectVersionAndAudit(page, version, 'Risk Case Review Started');

  await runAction(page, 'Change priority', {
    Priority: 'Critical',
    Reason: 'Q-017 live priority verification.',
  });
  version += 1;
  await expect(page.getByText('Critical priority', { exact: true })).toBeVisible();
  await expectVersionAndAudit(page, version, 'Priority');

  await runAction(
    page,
    'Resolve case',
    {
      'Resolution outcome': 'No Risk',
      'Resolution summary and reason': 'Q-017 live resolution verification.',
    },
    true,
  );
  version += 1;
  await expect(page.getByText('Resolved', { exact: true })).toBeVisible();
  await expectVersionAndAudit(page, version, 'Resolution');

  await runAction(
    page,
    'Close case',
    { Reason: 'Q-017 live closure verification.' },
    true,
  );
  version += 1;
  await expect(page.getByText('Closed', { exact: true })).toBeVisible();
  await expectVersionAndAudit(page, version, 'Risk Case Closed');
});

async function loginAndOpenCase(page: Page, targetCaseNumber: string) {
  await page.goto('/cases');
  await page.getByRole('button', { name: 'Sign in with Keycloak' }).click();
  await page.locator('#username').fill(operatorUsername);
  await page.locator('#password').fill(operatorPassword!);
  await page.locator('#kc-login').click();
  await expect(page.getByRole('heading', { name: 'Risk cases' })).toBeVisible();
  await expect(page.getByText(/Page \d+/)).toBeVisible();

  const caseLink = page.getByRole('button', { name: targetCaseNumber, exact: true });
  while ((await caseLink.count()) === 0) {
    const next = page.getByRole('button', { name: 'Next' });
    if (await next.isDisabled()) {
      throw new Error('E2E_Q017_CASE_NUMBER was not found in the bounded Risk Case pages.');
    }
    await next.click();
    await expect(page.getByText(/Page \d+/)).toBeVisible();
  }
  await caseLink.click();
  await expect(page.getByRole('heading', { name: targetCaseNumber })).toBeVisible();
}

async function runAction(
  page: Page,
  actionLabel: string,
  values: Record<string, string>,
  terminal = false,
) {
  await page.getByRole('button', { name: actionLabel }).click();
  const dialog = page.getByRole('dialog');
  for (const [label, value] of Object.entries(values)) {
    const control = dialog.getByLabel(label);
    if ((await control.getAttribute('role')) === 'combobox') {
      await control.click();
      await page.getByText(value, { exact: true }).last().click();
    } else {
      await control.fill(value);
    }
  }
  if (terminal) {
    await dialog.getByRole('button', { name: 'Review action' }).click();
    await expect(dialog.getByText('Confirm terminal case operation')).toBeVisible();
    await dialog
      .getByRole('button', { name: `Confirm ${actionLabel.toLowerCase()}` })
      .click();
  } else {
    await dialog.getByRole('button', { name: actionLabel }).click();
  }
  await expect(page.getByText(`${actionLabel} completed.`)).toBeVisible();
  await expect(dialog).toBeHidden();
}

async function displayedVersion(page: Page): Promise<number> {
  const text = await page.getByText(/^Version \d+$/).first().textContent();
  const version = Number(text?.replace('Version ', ''));
  if (!Number.isInteger(version)) throw new Error(`Invalid displayed version: ${text}`);
  return version;
}

async function expectVersionAndAudit(page: Page, version: number, eventType: string) {
  await expect(page.getByText(`Version ${version}`, { exact: true })).toBeVisible();
  await expect(page.getByText(`v${version} · ${eventType}`, { exact: true })).toBeVisible();
}
