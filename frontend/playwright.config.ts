import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: process.env.E2E_CONSOLE_URL ?? 'http://localhost:4173',
    trace: 'off',
    screenshot: 'off',
    video: 'off',
  },
  reporter: [['list'], ['html', { open: 'never' }]],
});
