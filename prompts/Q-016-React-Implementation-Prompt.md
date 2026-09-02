# Q-016 Frontend Foundation — React Implementation Prompt

**CLEARED FOR USE — Product Owner authorized the React pivot 2026-09-02.** The
React-pivot bundle (ADR-018 Accepted + the React pivot architecture addendum +
this prompt) was accepted at the Q-016 frontend-pivot authorization gate; the
component library is **Ant Design**; ADR-017 (Flutter) is Superseded. Recorded in
`docs/requirements/Q-016-Frontend-Foundation.md` §17. Governed by the two
`docs/engineering/` documents — read them first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-016-Frontend-Foundation.md` (V1, APPROVED; §17 records
   the React pivot).
2. `docs/adr/ADR-018-frontend-framework-react-spa.md` (React + TypeScript SPA;
   supersedes ADR-017 on the stack).
3. `docs/architecture/q-016-frontend-foundation-react-pivot-addendum.md` — the
   authoritative React stack + repository layout; it supersedes the Flutter
   stack sections of the two documents below.
4. For the **framework-agnostic** design still in force — the auth flow, the
   `ApiResponse`/`ResultCode` contract shape, AC traceability, security, and the
   already-built `GET /api/risk-cases` design: `ADR-017`,
   `q-016-frontend-foundation-architecture.md`, and
   `q-016-frontend-foundation-implementation-design.md` (read the parts NOT
   about Flutter/Dart/Riverpod/dio/freezed).

Also read for context and **reuse/consume unchanged** (do not modify):
`com.brokeros.risk.security.*` (Q-009 JWT verification),
`com.brokeros.risk.riskcase.*` (Q-008), and the **already-delivered** Q-016
backend from commit `45ef769`: `GET /api/risk-cases` (`RiskCaseController` +
`RiskCaseQueryService` + list/page/summary types + REST DTOs), the dev
`DevConsoleCorsConfiguration`, the dev Keycloak service/realm/bootstrap in
`docker-compose.yml` + `deploy/keycloak/`, and `application-dev.yml`.

## The confirmed shape — summary; the governing documents are authoritative

- A **React 18 + TypeScript (strict), Vite** SPA "Risk Console" in the mono-repo
  **`frontend/`** directory — **replacing** the Flutter app now there. Thin
  client: no business rules client-side, backend authorization never bypassed,
  bounded data only.
- Stack (addendum §2–§3): **React Router**, **TanStack Query**,
  **react-oidc-context** (over `oidc-client-ts`), an **axios** client with a
  bearer/`ResultCode` interceptor, hand-written **TypeScript** types for
  `ApiResponse<T>` / DTOs / `ResultCode`, **TanStack Table** for the list, and
  **Ant Design** components. (If the Product Owner has substituted the component
  library, follow that; everything else is fixed.)
- Auth: **OIDC Authorization Code + PKCE against Keycloak** — the app never sees
  a password; obtains a JWT and sends `Authorization: Bearer`; the backend
  (Q-009) verifies it. Access token in memory; refresh managed client-side;
  `401`→one silent refresh-and-retry, `403`→typed authorization error. Never put
  identity in a request body/param.
- One **vertical slice** over Risk Case: login → list (filter + bounded
  pagination) → open case (detail + history + associations) → one operation
  (**add investigation note**) with `expectedVersion` and
  `RISK_CASE_VERSION_CONFLICT` handling.
- **Backend: reuse the delivered endpoint as-is.** `GET /api/risk-cases` already
  exists and passed review. **Make no backend code change.** The only backend
  touch permitted is, if strictly required, adding the SPA's dev redirect
  URI/origin to the **dev** Keycloak realm/CORS config (dev profile only,
  configuration only) — and only if the existing dev origin does not already fit
  the Vite dev server; prefer aligning the Vite dev port to the existing origin.

## Task

Implement the Q-016 Risk Console in **React + TypeScript** exactly as specified
in ADR-018 + the React pivot addendum, and only that: the `frontend/` React SPA
(skeleton + the one Risk Case vertical slice) with the tests below. **Remove the
Flutter `frontend/` contents** (pubspec, lib, Dart tests, analysis_options, the
Flutter web shell) and replace them with the React project; keep `frontend/`
as the directory. Update `frontend/README.md` for the React toolchain. Make **no
backend code change**.

Tests (addendum §5):
- **Unit/component:** Vitest + React Testing Library — query hooks/repository
  with **MSW** mocking the backend envelope; list/detail/add-note components for
  loading/empty/error/success and the version-conflict path.
- **Contract:** a test asserting the TypeScript types parse the documented
  `ApiResponse` / `ResultCode` / list envelope (mirrors the old
  `api_contract_test`).
- **E2E (Playwright):** the login→list→detail→add-note slice, runnable against
  the dev Keycloak + backend. If Playwright's browser download is unavailable in
  your environment, deliver the specs and say so (see Environment honesty).

## Hard boundaries — do not do these

- **No backend code change.** The delivered `GET /api/risk-cases`, Q-008…Q-014,
  and Q-009 stay exactly as-is; add **no** Flyway migration. (Dev-only Keycloak
  redirect-URI/CORS config for the SPA origin is the sole exception, config
  only.)
- Do not move any business rule, invariant, transition decision, or
  authorization into the frontend. The console renders backend data and honors
  backend answers; it may disable a control for UX but always calls the backend
  and respects its result.
- OIDC **Authorization Code + PKCE only** — no implicit/password grant, no
  in-backend login. Q-009 stays verify-only.
- Do not expose unbounded list queries; rely on the endpoint's server page cap
  (100) and its bounded summary projection.
- Do not add new production infrastructure beyond the Keycloak already decided;
  no Kafka/Redis.
- Do not put any token, credential, or sensitive case content in logs, test
  fixtures, or artifacts; dev credentials are dev-only.
- Do not stage, commit, or push. Do not touch any existing timestamped review
  package.
- Do not silently reinterpret a contradiction; resolve toward the approved
  documents (Requirement > ADR-018 > addendum) and record the assumption in
  `OutstandingItems.md`.

## Environment honesty (important)

The React toolchain is Node + a package manager + Vite. Vitest/RTL/MSW run on
Node (jsdom) with **no browser** — run them and report real pass/fail counts.
`vite build` (type-check + bundle) should also run. **Playwright** needs a
headless Chromium download; on Apple Silicon (arm64) and CI this works, but if
your environment blocks the browser download, say so explicitly in
`Verification.md` and deliver the Playwright specs as code marked "not executed —
browser unavailable." If Node itself is unavailable, say so and deliver the
complete correct source without claiming any frontend check passed. **Never claim
a check passed that did not run.** (Unlike the Flutter attempt, the unit/
component layer here should be genuinely executable without a browser — run it.)

## Required output

After implementation and verification, create ONE new, non-overwriting,
timestamped review package at
`review/q-016/review-q-016-v<N>-react-implementation-<YYYYMMDD-HHMMSS>/` (check
the directory for the next unused version) containing at minimum: `Summary.md`,
`ArchitectureReview.md` (against ADR-018 + addendum), `DesignTraceability.md`
(map each Q016-FR-XXX + each addendum section to implementing code/test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact
commands, tool availability, pass/fail/skip counts — honest; state Node/Vite/
Vitest/Playwright availability and what was/wasn't run), `SecurityReview.md`,
`TestInventory.txt`, and `OutstandingItems.md`. Add a
`docs/lessons/<date>-q-016-react-implementation.md` entry.

Confirm (do not re-run a backend change you did not make) that the delivered
backend endpoint and gate are untouched: report `git diff --stat` for `backend/`
is empty for this task.

This review package is for Claude Code's independent implementation review, not
your own sign-off — do not mark Q-016 "complete" or "approved"; state PASS/FAIL
against each acceptance criterion honestly and list every open question and
assumption.

Stop after producing the review package. Do not begin any other Requirement.
