# Q-016 (React) — C2 Live E2E: EXECUTED, and it found a blocking defect

Condition C2 (the live Keycloak → backend → MySQL browser slice) has now been
**executed by the reviewer** against a full local stack. It **found a genuine
blocking defect** in the committed React app that only manifests in a real
browser redirect — exactly the kind of thing the unit/component tests (MSW) and
Codex's honestly-skipped E2E could not catch. After fixing it, the full slice
passes end to end.

## Stack stood up

`docker compose --profile console` with dev-only `.env` values (ephemeral,
removed after; volumes deleted on teardown): MySQL 8.4, Redis, Kafka, Keycloak
26.7.3 (realm imported), and `console-backend` (Flyway V1–V8 applied). Operator
`q016-operator` password set and email completed via the Keycloak Admin API; the
security bootstrap granted it `risk-case:read` + `risk-case:note`. One eligible
trading-account reference and one OPEN Risk Case
(`RC-be97044d-…`) were seeded by SQL (the realm disables direct grants, so a
case cannot be created through the API without a full browser login).

Preflight (host): backend `/actuator/health` 200; `GET /api/risk-cases` without a
token → **401** (auth enforced); Keycloak openid-config + JWKS 200.

## Finding F5 (BLOCKING) — OIDC redirect callback dropped; login silently failed

**Symptom:** after a correct Keycloak login, the app returned to
`http://localhost:4173/?code=…&state=…` but **never exchanged the code** (no
`/token` request) and fell back to the sign-in page. Captured evidence: the
return URL contained a valid `code`, `sessionStorage` still held the unconsumed
`oidc.<state>` entry, and no token request was made.

**Root cause:** `react-oidc-context` processes the callback in a parent-level
effect, but the app's catch-all route (`<Route path="*" element={<Navigate
to="/cases">}`) plus `ProtectedRoute`'s redirect to `/login` run first (child
effects), doing `history.replaceState` that **strips `?code`/`?state` from the
URL before the code is exchanged**. Login could therefore never complete in a
real browser. Unit/component tests mock the repository and never drive the real
redirect, so this was invisible to them.

**Fix (reviewer, `src/app/App.tsx`):** gate the router — while
`hasAuthParams() || auth.isLoading || auth.activeNavigator`, render a
"Completing sign-in" spinner instead of mounting `<BrowserRouter>`, so the
callback URL is preserved until `react-oidc-context` exchanges the code. Then
routing proceeds to `/cases`. Typecheck 0 errors; Vitest still 27/27.

## Finding F6 (minor, test) — E2E spec raced the async list load

The delivered spec searched for the case button immediately after the "Risk
cases" heading appeared, while the list query was still in flight; "Next" is
disabled during load, so the pagination loop threw a false "case not found".
**Fix (reviewer, `tests/e2e/riskCaseSlice.spec.ts`):** wait for the loaded
pagination indicator (`/Page \d+/`) before searching/paginating. Test-file only.

## Result after the fixes — FULL SLICE PASS (live)

Driven headless (Playwright/Chromium) against the live stack:

| Step | Result | Live backend call |
| --- | --- | --- |
| Keycloak login (Auth Code + PKCE) → list | OK | `200 GET /api/risk-cases?page=0&size=20` |
| Seeded case visible in list | OK | — |
| Open detail | OK | `200 GET /api/risk-cases/RC-…` |
| History timeline | OK | `200 GET /api/risk-cases/RC-…/history?limit=100` |
| Add investigation note | OK | **`201 POST /api/risk-cases/RC-…/notes`** |
| Success message + dialog closed | OK | detail/history re-fetched |

The **official Codex spec** `npm run test:e2e` now passes: **1 passed**.

**Database confirms the write went through the real domain** (not a UI
illusion): `risk_case_note` has the exact note content, the case `version`
incremented **1 → 2**, and audit records were written. This proves AC 2 (live
OIDC/PKCE + Q-009 JWT verification + Bearer, no identity in body) and AC 6 (full
local slice) end to end.

## Updated gate

C2 is **satisfied**. A blocking defect (F5) and a test race (F6) were found and
fixed by the reviewer and verified live. With F5/F6 fixed, **all Q-016
acceptance criteria pass**. The two fixes are committed as a follow-up; the
ephemeral stack and dev `.env` were torn down (volumes removed).
