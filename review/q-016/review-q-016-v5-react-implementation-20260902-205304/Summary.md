# Q-016 React Implementation Review Summary

- Requirement: Q-016 — Frontend Foundation (Risk Console)
- Lifecycle stage: Implementation + Implementation Verification
- Authorized pivot: React 18 + strict TypeScript SPA; ADR-018 Accepted;
  ADR-017 Superseded on client stack; Ant Design selected
- Baseline: `275ef06` (`docs: pivot Q-016 frontend from Flutter to React (ADR-018, PO authorized)`)
- Review package: v5 React implementation, generated 2026-09-02 20:53 +08:00
- Gate Decision: **PASS WITH CONDITIONS**

The authorized React implementation is present under `frontend/` and replaces
the Flutter/Dart client. Strict TypeScript checking, 27 Vitest/React Testing
Library/MSW tests, the production Vite build, npm advisory scan, repository
static verification, Playwright Chromium installation/launch, and a live Vite
login-page browser smoke all passed. The complete Keycloak → backend → Risk
Case browser slice was not executed because the repository root `.env` and an
existing `E2E_CASE_NUMBER` were unavailable. The live Playwright spec is
delivered and was discovered correctly, but its one test was honestly skipped.

This package is evidence for Claude Code's independent implementation review.
It does not mark Q-016 complete or approved and does not authorize another
Requirement.

## Delivered scope

- React 18 + strict TypeScript + Vite SPA, React Router auth guard, lazy Risk
  Case routes, Ant Design shell/components, and TanStack Table.
- `react-oidc-context` / `oidc-client-ts` Authorization Code + PKCE settings,
  memory-only signed-in user/access-token state, and session-only protocol
  state.
- Central axios client that attaches Bearer, coordinates one silent refresh,
  retries one `401`, preserves the refreshed header, maps `403`, and parses the
  backend envelope at runtime.
- TanStack Query repository/hooks for bounded list/filter/pagination,
  detail/history, and add-investigation-note with `expectedVersion` conflict
  reload.
- Loading, empty, error, success, submission, ordinary operation error, and
  version-conflict UI states covered with MSW-backed tests.
- Runtime endpoint/public-client configuration, documented local Keycloak/
  backend workflow, updated one-command dev launcher, and configuration catalog.
- Live Playwright login → list → detail → add-note spec with trace/screenshot/
  video disabled to prevent credential/token/case-content artifacts.
- Q-016 React Lessons Learned and reusable React Risk Console skill; the
  Flutter skill is explicitly historical.

## Acceptance criteria

| AC | Result | Evidence / condition |
| --- | --- | --- |
| 1 | **PASS** | `npm run build` passed strict typecheck and Vite production bundling; Vite + headless Chromium loaded `/login` with HTTP 200 and zero browser errors. |
| 2 | **FAIL — live verification outstanding** | Authorization Code configuration, memory-only user store, Bearer attachment, one-refresh retry, refresh-failure logout, and no body identity passed automated tests. A real Keycloak login/JWT exchange was not executed without `.env`. |
| 3 | **PASS** | MSW-backed list and detail tests passed for loading/empty/error/success; detail renders bounded history and association references plus the current decision. The backend does not expose a separate complete current-association projection; this inherited contract limitation is recorded. |
| 4 | **PASS** | Add note sends only `content` + `expectedVersion`; success, pending, validation, ordinary ResultCode error, and `RISK_CASE_VERSION_CONFLICT` reload/input-preservation paths passed. |
| 5 | **PASS** | The already-delivered bounded authorized `GET /api/risk-cases` is consumed unchanged; `git diff --stat -- backend/` and `git diff --name-only -- backend/` are empty. No migration exists in the task diff. |
| 6 | **FAIL — live verification outstanding** | React local-run instructions and the updated launcher are present. The complete dev stack was not started because required ignored `.env` values and an existing Risk Case were absent. |
| 7 | **FAIL — one environment-dependent test skipped** | Unit/component/contract: 27 passed, 0 failed, 0 skipped. Playwright: 1 live spec discovered, 1 skipped because live credentials/case input were absent. |
| 8 | **PASS** | Backend task diff is empty; no Q-008…Q-014 code, security implementation, migration, Keycloak realm, CORS, or Compose topology was changed. Frontend never supplies actor identity in a command. |

## Changed files by responsibility

- React application/toolchain: `frontend/package*.json`, Vite/TypeScript/
  Playwright config, runtime config, `src/app`, `src/core`, feature/UI/shared
  source, tests, ignore rules, and React README.
- Removed stack: tracked Flutter/Dart `frontend/lib`, `frontend/test`,
  `frontend/web`, pubspec/lock, generated models, and analysis configuration.
- Local workflow: `scripts/run-risk-console-dev.sh` now runs npm/Vite;
  `scripts/README.md` names the React console.
- Engineering knowledge/config: React settings catalog, React console skill,
  Flutter skill historical marker, skills index, and Q-016 React lesson.
- Review: this new v5 directory only; no older timestamped review package was
  modified.

## Exit condition

Stop at the Q-016 React implementation-verification gate. Claude Code should
independently inspect this package/code and run the live Playwright slice when
the required local-only inputs are supplied. No stage, commit, or push occurred.
