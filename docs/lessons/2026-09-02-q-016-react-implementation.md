# Q-016 React Implementation Lessons Learned

## Scope

Q-016 replaced the accepted Flutter client under `frontend/` with the
Product-Owner-authorized React 18 + strict TypeScript SPA from ADR-018. The
delivered backend Risk Case endpoints, Q-009 security boundary, Flyway
migrations, Keycloak realm, CORS configuration, and Compose topology were
consumed unchanged.

## What worked

- Hand-written runtime parsers kept the TypeScript DTOs tied to the real
  `ApiResponse`, list, detail, history, and add-note response records rather
  than treating compile-time types as runtime validation.
- An injected repository plus TanStack Query allowed MSW to exercise the same
  bearer/error/contract path used by the components for loading, empty, error,
  success, mutation, and version-conflict states.
- Aligning Vite to port 4173 reused the already-delivered exact Keycloak
  redirect URI and dev backend CORS origin, so no backend or dev-IdP
  configuration change was needed.
- Updating the existing `run-risk-console-dev.sh` entry point from Flutter to
  `npm ci` + Vite preserved the repository's one-command local workflow.
- An in-memory OIDC user store kept the signed-in user/access token out of
  browser storage while session storage retained only redirect protocol state.

## Problems encountered

- The first 401 retry test found that the ordinary axios request interceptor
  overwrote the explicitly refreshed Authorization header with the stale token
  on replay. The fix makes the interceptor attach a token only when the request
  does not already carry the header; the regression test proves exactly one
  refresh and one replay.
- Ant Design's modal exit animation remains mounted briefly in jsdom. Mounting
  the dialog only while open made component lifecycle deterministic without
  weakening the success assertion or changing production behavior.
- The expanded empty-form test found that `Form.validateFields()` rejects on
  validation failure. Catching that structured validation result prevents an
  unhandled Promise while leaving the field-level message visible; submission
  controls and input are also explicitly disabled while a mutation is pending.
- Vitest initially could not create Vite's temporary cache under the execution
  sandbox. Running the same authorized command with the required filesystem
  permission succeeded; this was an environment restriction, not a code defect.
- The root `.env` and an existing Risk Case number were not available, so the
  live Keycloak → backend Playwright slice was delivered but not executed.
  Chromium download and a real headless launch succeeded, keeping browser
  availability distinct from missing live-test inputs.

## Security and data-boundary result

TanStack Query caches bounded but potentially sensitive Risk Case data. The
cache must be cleared whenever the authenticated subject changes and when a
401 cannot be refreshed; otherwise one browser session could expose an earlier
operator's cached data to the next operator. Live Playwright tracing,
screenshots, and video are disabled because an authenticated browser artifact
can contain passwords, redirects, tokens, or case content.

## Reusable result

The durable React-specific rules are captured in
`docs/skills/react-risk-console-development.md`. The most important regression
rules are to preserve the refreshed bearer header during replay, clear server
state at identity boundaries, and report Playwright browser availability
separately from live environment readiness.
