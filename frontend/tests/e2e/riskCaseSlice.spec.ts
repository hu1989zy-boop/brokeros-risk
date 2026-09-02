import { expect, test } from '@playwright/test';

const operatorUsername = process.env.E2E_OPERATOR_USERNAME ?? 'q016-operator';
const operatorPassword = process.env.E2E_OPERATOR_PASSWORD;
const caseNumber = process.env.E2E_CASE_NUMBER;

test.skip(
  !operatorPassword || !caseNumber,
  'Live Keycloak credentials and an existing Risk Case are required via E2E_OPERATOR_PASSWORD and E2E_CASE_NUMBER.',
);

test('login → list → detail → add investigation note', async ({ page }) => {
  await page.goto('/cases');
  await page.getByRole('button', { name: 'Sign in with Keycloak' }).click();

  await page.locator('#username').fill(operatorUsername);
  await page.locator('#password').fill(operatorPassword!);
  await page.locator('#kc-login').click();

  await expect(page.getByRole('heading', { name: 'Risk cases' })).toBeVisible();
  // Wait for the first page of results to finish loading before searching/paginating,
  // otherwise the search races the async list query (Next is disabled while loading,
  // which would falsely report the case as not found).
  await expect(page.getByText(/Page \d+/)).toBeVisible();

  const caseLink = page.getByRole('button', { name: caseNumber!, exact: true });
  while ((await caseLink.count()) === 0) {
    const next = page.getByRole('button', { name: 'Next' });
    if (await next.isDisabled()) {
      throw new Error('E2E_CASE_NUMBER was not found in the bounded Risk Case pages.');
    }
    await next.click();
    await expect(page.getByText(/Page \d+/)).toBeVisible();
  }
  await caseLink.click();

  await expect(page.getByRole('heading', { name: caseNumber! })).toBeVisible();
  await expect(page.getByText('History timeline')).toBeVisible();
  await page.getByRole('button', { name: 'Add note' }).click();

  const dialog = page.getByRole('dialog');
  const note = `Q-016 Playwright verification ${new Date().toISOString()}`;
  await dialog.getByLabel('Investigation note').fill(note);
  await dialog.getByRole('button', { name: 'Add note' }).click();

  await expect(page.getByText(/Investigation note .* was added\./)).toBeVisible();
  await expect(dialog).toBeHidden();
});
