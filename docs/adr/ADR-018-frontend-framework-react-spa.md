# ADR-018: Frontend Framework — React + TypeScript SPA (supersedes ADR-017's Flutter choice)

- Status: **Accepted — 2026-09-02 — Product Owner** — at the Q-016
  frontend-pivot authorization gate (Decision Authority §16.5-B). Component
  library confirmed by the Product Owner: **Ant Design**.
- Date: 2026-09-02
- Approval origin: drafted by Claude Code (external Architect role) after the
  Product Owner's explicit framework decision on 2026-09-02 ("用 React").
- Requirement: Q-016 — Frontend Foundation V1, APPROVED — 2026-09-02
  (unchanged; the WHAT — a thin-client Risk Console — is not reopened).
- Supersedes: **ADR-017 (Frontend Foundation — Flutter Risk Console)**, on
  the frontend-framework/stack decision only. ADR-017's still-valid,
  framework-agnostic decisions are re-affirmed below.

## Context

ADR-017 chose a **Flutter** web Risk Console. Q-016 was implemented on that
basis, independently reviewed (PASS WITH CONDITIONS, commit `45ef769`), and
the review made two things concrete:

1. **Fit.** The Risk Console is an **internal, web-first, data-dense
   operations tool** (risk-case list/filter/paginate, detail/history/
   associations, notes) consumed by analysts at desks. Flutter's core value —
   one codebase across native iOS/Android/web/desktop with pixel-perfect
   self-drawn UI — is a mobile-first / cross-platform proposition. For a
   web-only internal data console it is a mismatch: canvas rendering weakens
   accessibility, text selection and copy; the initial payload is heavy; and
   the mature enterprise **data-grid** ecosystem lives on the DOM (AG Grid,
   TanStack Table), not in Flutter.
2. **Verification friction.** The mandatory in-browser test path
   (`flutter test --platform chrome`) could not be executed on the Product
   Owner's **Apple Silicon (arm64)** machine: Google Chrome has no linux/arm64
   build and Ubuntu's chromium is a non-runnable snap stub, so Q-016's 13
   tests reached "compiles + analyzes clean" but not in-browser execution
   (review condition C1). The Dart codegen toolchain (freezed/build_runner
   analyzer pins) was also brittle (review finding F1).

The Product Owner confirmed there is **no near-term plan for native iOS/Android
apps** sharing one codebase — the single fact that would have justified
Flutter — and chose **React**. Because Q-016's frontend is a thin, just-built
foundation, the switching cost is at its minimum now, before more screens are
built on it.

## Decision

### Frontend framework: React + TypeScript, single-page app, built with Vite

The Risk Console is (re)built as a **React 18 + TypeScript (strict)** SPA under
the mono-repo `frontend/` directory, built and tested with **Vite**. This
replaces the Flutter/Dart stack of ADR-017. The thin-client discipline, the
Keycloak/OIDC authentication model, the backend contract, the single additive
list endpoint, and the security boundary are **carried over unchanged** from
ADR-017 (re-affirmed below); only the client technology changes.

### Frontend technical stack (HOW decisions, Decision Authority §16.1)

- **Routing:** React Router (declarative routes + an auth guard).
- **Server state:** TanStack Query (React Query) — caching, ret/refetch,
  invalidation for the bounded list/detail/history reads and the note
  mutation with `expectedVersion` conflict handling.
- **Auth:** `react-oidc-context` over `oidc-client-ts` — OIDC Authorization
  Code + PKCE against Keycloak; access token in memory, refresh handled by
  the library; a single HTTP wrapper attaches `Authorization: Bearer`, does
  one silent refresh on `401`, and maps `403` to a typed authorization error.
- **HTTP + typed contract:** an `axios` (or fetch) client with a request
  interceptor; hand-written **TypeScript types** mirroring `ApiResponse<T>`,
  the consumed DTOs, and a `ResultCode` string-union — a backend contract
  change becomes a compile-time signal.
- **Data table:** **TanStack Table** (headless) for the risk-case list
  (sort/filter/bounded pagination).
- **Component library:** **Ant Design** — richest built-in enterprise
  table/form/layout set for a data-dense admin console. (This one is the most
  taste-/team-driven choice; MUI, Mantine, or shadcn/ui + Tailwind are
  acceptable substitutes and do not change this ADR.)
- **Testing:** **Vitest + React Testing Library** (unit/component) with **MSW**
  for API mocking, and **Playwright** for the end-to-end browser slice. All of
  these run headless Chromium on arm64 and on any CI — directly resolving the
  Q-016 C1 verification limitation.

### Re-affirmed from ADR-017 (unchanged by this pivot)

- **Thin client** — all authorization/business rules stay in the backend;
  the console renders bounded data and honors backend `ResultCode`/errors
  (Principles §5/§15).
- **Keycloak** as IdP (dev + production); **OIDC Authorization Code + PKCE**;
  backend Q-009 verifies JWTs; identity never in a request body/param.
- The one additive, read-only, authorized, bounded **`GET /api/risk-cases`**
  list/query endpoint (already delivered in `45ef769`) — **kept as-is**; no
  further backend change, no aggregate/business-rule/migration change.
- **One vertical slice + skeleton**, not the whole UI (login → list → open
  case with detail/history/associations → add-note with version handling).
- Dev **Keycloak** realm/bootstrap/CORS and the docker-compose dev profile
  (already delivered) — **kept**; they serve any SPA identically.

## Alternatives Considered

- **Keep Flutter** — rejected: justified only by a native-mobile shared-codebase
  plan, which the Product Owner confirmed is not on the roadmap; it keeps the
  data-grid, accessibility, payload, and arm64-testing costs above.
- **Vue 3** — a valid peer of React (same DOM-native, headless-testable camp);
  rejected in favor of React for the deepest enterprise data-grid/admin-template
  ecosystem. A close call decided by the Product Owner.
- **Angular** — heavier framework; not chosen. Not requested.
- **Server-rendered (e.g. Next.js SSR / htmx)** — unnecessary for an
  authenticated internal tool with no SEO need; a plain SPA is simpler and the
  OIDC/PKCE + JWT model fits a public SPA client cleanly.

## Consequences

Positive:

- Fit-for-purpose: DOM-native accessibility/selection, light payload, and the
  strongest data-grid ecosystem for a data-dense console.
- **In-browser tests run on the Product Owner's Apple-Silicon machine and any
  CI** (Vitest/Playwright headless Chromium) — closes the Q-016 C1 gap that
  Flutter could not on arm64.
- Wider hiring/skill pool; the backend contract and Keycloak model are reused
  unchanged, so the pivot is confined to the client layer.

Costs and constraints:

- The Flutter `frontend/` from `45ef769` is discarded and re-implemented in
  React (thin foundation; lowest-cost moment to switch).
- Introduces a Node/TypeScript frontend toolchain (freely installable).
- The thin-client discipline must still be enforced (no client-side
  authorization/business logic beyond rendering + calling).

## Security Implications

Unchanged from ADR-017: authorization stays server-side (Q-009); the console is
not a trust boundary; OIDC Auth Code + PKCE, no implicit/password grant; access
token in memory, refresh in library-managed storage; no token/sensitive content
logged; bounded data only; tightly-scoped CORS/redirect URIs; TLS in production.

## Data / Contract Implications

The console consumes the same `ApiResponse`/`ResultCode` contract via typed
**TypeScript** types (previously Dart). The list endpoint and its bounded
summary projection are unchanged.

## Operational Implications

Keycloak and the one backend endpoint are unchanged. Frontend build output is
static assets (Vite build) served like any SPA. No new backend or infra beyond
what ADR-017/Q-016 already introduced.

## Dependencies

Backend Q-008 contracts + Q-009 JWT verification + the delivered
`GET /api/risk-cases` (all consumed, unchanged). Keycloak (IdP). Node/Vite
toolchain (build-time). Independent of Q-015 / the MT4/MT5 SDK.

## Deferred Decisions

Implementation Design fixes exact package versions, component composition,
query-param validation, page-size cap reuse, the Keycloak realm/redirect URIs
for the SPA origin, and file names. Future Requirements: broad UI, dashboards
(post-Q-015), any native/desktop target, production Keycloak hardening/SSO.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-09-02** at the Q-016
frontend-pivot authorization gate, together with the React pivot architecture
addendum and the React implementation prompt (§16.5-B). ADR-017 is thereby
**Superseded by ADR-018** on the framework/stack decision; the component
library is confirmed as Ant Design. Codex is authorized to replace the Flutter
`frontend/` with the React implementation.
