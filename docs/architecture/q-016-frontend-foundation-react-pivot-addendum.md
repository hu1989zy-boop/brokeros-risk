# Q-016 Frontend Foundation — React Pivot Architecture Addendum

## Document Status

- Requirement: Q-016 — V1, APPROVED — 2026-09-02 — Product Owner (unchanged).
- ADR: **ADR-018 (React + TypeScript SPA) — Accepted 2026-09-02 — Product
  Owner**, superseding ADR-017 (Flutter) on the framework/stack decision.
  Component library confirmed: **Ant Design**.
- Supersedes: the **frontend framework/stack sections** of
  `q-016-frontend-foundation-architecture.md` and
  `q-016-frontend-foundation-implementation-design.md` (both written for
  Flutter). Everything in those two documents that is **framework-agnostic**
  remains authoritative (see §"Carried over unchanged" below).
- Prepared by: Claude Code, external Architect role, as the §16.5-B connected
  chain for the React pivot. Presented as a bundle at the frontend-pivot
  authorization gate. Live status: the Q-016 Requirement §17 gate is
  authoritative.

## 1. Why this addendum exists

The Product Owner decided (2026-09-02) to build the Risk Console in **React +
TypeScript** instead of Flutter (rationale and alternatives: ADR-018). Rather
than rewrite the two Flutter-era Q-016 architecture documents line by line,
this addendum records the new client stack and points to the parts of those
documents that still hold. Where this addendum and the Flutter-era documents
disagree on client technology, **this addendum wins**.

## 2. Target stack (replaces the Flutter stack tables/sections)

| Concern | Flutter-era (ADR-017) | React pivot (ADR-018) |
| --- | --- | --- |
| Client app | Flutter web | **React 18 + TypeScript (strict), Vite SPA** |
| Routing | go_router | **React Router** (+ auth guard) |
| Server state | Riverpod | **TanStack Query** (cache/invalidation) |
| Client/auth state | Riverpod | **react-oidc-context** + light context/Zustand |
| HTTP | dio | **axios** wrapper + request interceptor |
| Typed contract | Dart freezed models | **hand-written TypeScript types** (`ApiResponse<T>`, DTOs, `ResultCode` union) |
| Data table | Flutter widgets | **TanStack Table** (headless) rendered via Ant Design |
| Component library | Flutter widgets | **Ant Design** (substitutable: MUI / Mantine / shadcn+Tailwind) |
| Unit/component test | Riverpod notifier tests | **Vitest + React Testing Library + MSW** |
| E2E test | (unrun on arm64) | **Playwright** (headless Chromium — runs on arm64 + CI) |

## 3. Repository layout (`frontend/`, replacing the Flutter tree)

```
frontend/
├── package.json / vite.config.ts / tsconfig.json
├── index.html
├── src/
│   ├── app/            # router, providers (QueryClient, OIDC, theme), app shell
│   ├── core/
│   │   ├── api/        # axios client, ApiResponse<T>, ResultCode, Page<T>, error mapping, bearer interceptor
│   │   ├── auth/       # OIDC/Keycloak (react-oidc-context), auth guard, 401 refresh / 403 mapping
│   │   └── config/     # runtime config (Keycloak authority/clientId, API base)
│   ├── features/
│   │   └── riskcase/
│   │       ├── api/    # RiskCaseSummary/Detail/History types + repository (typed API calls)
│   │       ├── model/  # view models / query hooks (TanStack Query)
│   │       └── ui/     # list, detail (detail/history/associations), add-note dialog
│   └── shared/         # async (loading/empty/error) views, table primitives
└── tests/              # Vitest unit/component + Playwright e2e
```

Domain/UI isolation is preserved: API calls and query hooks (`features/*/api`,
`features/*/model`) are unit-testable with MSW; `ui/` is thin rendering.

## 4. Carried over unchanged (still authoritative in the Flutter-era docs + ADR-017)

These decisions are framework-agnostic and are **not** changed by the pivot;
the corresponding sections of the two Flutter-era Q-016 documents remain the
reference for them:

- **Thin-client discipline** — no client-side authorization/business logic;
  render bounded data, honor backend `ResultCode`/errors (Principles §5/§15).
- **Authentication flow** — Keycloak IdP (dev + prod); OIDC Authorization Code
  + PKCE; backend Q-009 verifies the JWT; identity never in body/param; access
  token in memory, refresh managed client-side; silent refresh on `401`, typed
  auth error on `403`; dev→prod is a config repoint.
- **Backend list endpoint** — the delivered `GET /api/risk-cases` (bounded,
  authorized, projection-only, server cap 100, no migration) is **unchanged**;
  the pivot adds **no** backend change.
- **Security design, requirement traceability, and the one-vertical-slice +
  skeleton scope** — as in the Flutter-era architecture doc.
- **Dev environment** — the delivered dev Keycloak realm/bootstrap, exact-origin
  CORS, and docker-compose dev profile serve the React SPA identically (only
  the dev redirect URI/origin may need the SPA's dev port).

## 5. Testability (the concrete pivot win)

Q-016's review left condition C1 open because `flutter test --platform chrome`
could not launch a browser on arm64. The React stack removes that constraint:

- **Unit/component:** Vitest + React Testing Library run on Node (jsdom) — no
  browser needed; fast on any architecture.
- **API contract + behavior:** MSW mocks the backend envelope; a contract test
  asserts the TypeScript types parse the documented `ApiResponse`/`ResultCode`.
- **End-to-end:** Playwright drives headless Chromium, which installs and runs
  on Apple Silicon (arm64) and on CI — so C1 (run the tests in a real browser)
  and C2 (the live Keycloak browser slice) become routinely executable.

## 6. Architecture gate

This addendum + ADR-018 + the React implementation prompt formed the §16.5-B
bundle at the **Q-016 frontend-pivot authorization gate**, **accepted by the
Product Owner on 2026-09-02**: ADR-018 is Accepted, ADR-017 is Superseded, the
component library is Ant Design, and Codex is authorized to replace the Flutter
`frontend/` with the React implementation. Live status is recorded in the Q-016
Requirement §17.
