# Q-016: Frontend Foundation (Risk Console)

> **Framework pivoted to React (2026-09-02):** the Product Owner chose
> **React + TypeScript** over Flutter. "Flutter"/"Dart" wording throughout
> this document is superseded — read the Risk Console as a **React SPA**. The
> framework is a HOW/technology detail; the Requirement's WHAT (a thin-client
> Risk Console) is unchanged. Authoritative pivot record: §17 + **ADR-018** +
> `docs/architecture/q-016-frontend-foundation-react-pivot-addendum.md`.

## Status

V1, by Claude Code holding the external Architect role, per the two
`docs/engineering/` governance documents. **The authoritative live status
is §17 (Current Gate)** (Execution Protocol §16). Requirement stage of a
new task; the §5.3 product/infrastructure questions are surfaced for
explicit Product Owner confirmation (Decision Authority §16.2) rather than
silently assumed.

- Requirement ID: `Q-016`
- Architecture phase: Phase 1 (introduces a new client-application surface)
- Frontend technology: **React + TypeScript SPA** (Product Owner decision
  2026-09-02, ADR-018; superseded the earlier Flutter choice, ADR-017).
- Depends on: the committed backend Q-008…Q-014 HTTP contracts and Q-009's
  JWT-verification security boundary. Introduces backend read/query
  additions (§5.3(3)); does not change existing backend business logic.
- Independent of the Q-015 SDK blocker: this workstream proceeds while the
  MT4/MT5 SDK is being organized.

## 1. Background

The backend now has a real Core-Domain chain (Q-009…Q-014) and, with
Q-008, an aggregate-root Risk Case that a human operator actually works
with — the first thing worth a user interface. There is no frontend yet.
The Product Owner has chosen **Flutter** for it.

Two facts about the current backend shape the frontend Foundation:

1. **Authentication is delegated to an external identity provider.** Q-009
   *verifies* an inbound Bearer JWT (`SecurityJwtDecoderFactory` /
   `ActorContextAuthenticationFilter` / `JwtVerifiedPrincipalAdapter`)
   against a configured issuer; there is **no** username/password login or
   token-issuance endpoint in the backend, by design. The frontend
   therefore authenticates the user against the identity provider that
   issues the JWTs the backend is configured to verify, obtains a JWT, and
   sends it as `Authorization: Bearer` to the backend.
2. **Read/query APIs are minimal.** The modules expose "record" and "get
   by reference" endpoints; Risk Case (Q-008) has get-by-number, detail,
   history, and lifecycle commands, but **no list/search** endpoint. A
   usable operator console needs list/query read APIs, which this
   Foundation must add on the backend.

## 2. Existing Capability and Gap Analysis

| Need | Existing | Gap |
| --- | --- | --- |
| A client application for risk operators | None | Q-016 delivers the Flutter Risk Console |
| User authentication for the UI | Q-009 verifies externally-issued JWTs | The UI must run an OIDC/OAuth login against the issuing identity provider; the provider itself is a §5.3 decision |
| A typed contract to call the backend | The committed REST endpoints | Q-016 defines a typed client + a first consumed slice |
| List/search of Risk Cases (and later others) | Only get-by-number | Q-016 adds the backend read/query API needed by the console |
| A first end-to-end operator workflow | None | Q-016 proves login → list → open case → act, top to bottom |

## 3. Problem Statement

Risk operators have no interface: every interaction with the risk platform
is a raw HTTP call. Q-016 must establish a Flutter client — its
architecture, authentication, typed backend client, and a first complete
vertical slice (a real operator workflow over Risk Case) — plus the minimal
backend read/query API that workflow needs, without changing existing
backend business logic and without weakening the Q-009 security boundary.

## 4. Goals

1. Establish the **Flutter Risk Console** application: project structure,
   state management, a typed API client, session/auth handling, error and
   loading states, theming (light/dark), and a build for the target
   platform (§5.3(2)).
2. Authenticate the operator via the external identity provider that issues
   backend-verified JWTs, attach the Bearer token to every backend call,
   handle token expiry/refresh, and never place the actor identity in a
   request the backend would trust from the body.
3. Deliver a first **end-to-end vertical slice** proving the whole stack:
   authenticated login → **list Risk Cases** → open a Risk Case (detail +
   history/timeline + current associations) → perform at least one
   lifecycle/operation (e.g. assign, add investigation note, or change
   priority) with optimistic-version handling surfaced to the user.
4. Add the **minimal backend read/query API** the slice needs — a
   list/search endpoint for Risk Cases with bounded pagination — following
   the existing controller/`ApiResponse`/authorization patterns, without
   changing Q-008's aggregate or business rules.
5. Keep the console a thin, honest client: it renders bounded backend data,
   never bypasses backend authorization, never invents business state, and
   surfaces backend `ResultCode`/error contracts meaningfully.
6. Do not modify Q-008…Q-014 business logic; the only backend additions are
   read/query endpoints and (if required) a dev-mode token/IdP
   configuration for local runs (§5.3(1)).

## 5. Scope and Non-Goals

### 5.1 In Scope

- The Flutter Risk Console app (structure, state management, typed backend
  client, routing, theming, error/loading UX).
- OIDC/OAuth login against the configured identity provider; Bearer-token
  attachment; token lifecycle (expiry/refresh/logout).
- The first vertical slice over Risk Case (list → detail/history → one
  operation), end to end.
- The backend Risk Case **list/query read endpoint** (bounded, paginated,
  authorized) needed by the slice.
- A repeatable local run/dev setup (including a dev identity provider or
  dev-token mechanism so the slice can run without the production IdP,
  §5.3(1)).
- Frontend tests (widget/unit + the typed client contract against the
  backend's documented responses) and a backend test for the new
  read/query endpoint.

### 5.2 Non-Goals (explicitly deferred)

- Full UI coverage of every module and every operation — the Foundation is
  one proven vertical slice plus the app skeleton, not the whole product.
- Any real-time trading-data / exposure dashboard — Trading Data ingestion
  (Q-015) is not built yet; there is no live data to show.
- Building an identity provider or an in-backend password login — auth
  stays delegated to the external IdP (Q-009's design); the Foundation may
  use a dev IdP for local runs.
- Analytics, reporting, charts, mobile-push, offline mode, i18n beyond
  basics, and design-system polish beyond a clean, consistent baseline.
- Changing any Q-008…Q-014 business rule, aggregate, or migration.

### 5.3 Product / infrastructure decisions — confirmed by the Product Owner, 2026-09-02

All three confirmed as recommended.

**(1) Identity provider (IdP) — CONFIRMED: Keycloak (both dev and
production).** An IdP is the service that authenticates the user and issues
the signed JWT the backend already verifies (Q-009 is verify-only, by
design). The Product Owner chose **Keycloak** (self-hosted OIDC, free,
MFA/roles/SSO capable) for both the local dev IdP now and the production
IdP later. Because the backend verifies tokens by configured issuer/JWKS,
moving from dev Keycloak to production Keycloak is a **configuration change
(issuer/JWKS URL), not a code change** — the console and backend are simply
repointed at deployment time.

**(2) Target platform — CONFIRMED: web first.** The Flutter Risk Console
targets **web** first (browser-accessible for desktop operators, no
install, previewable during development), with the same codebase able to
add desktop/mobile later.

**(3) Backend read/query API scope — CONFIRMED: built in this Foundation.**
The one Risk Case list/search endpoint the console needs (bounded,
paginated, authorized, additive) is built as part of this Foundation, not
a separate Requirement.

(Claude Code will decide, as HOW: the repo placement — recommended a
mono-repo `frontend/` directory alongside `backend/` for now, one place to
build/version, separable later — and the Flutter state-management/client
architecture, at the Architecture stage.)

## 6. Definitions

- **Risk Console** — the Flutter client application operators use to work
  with the risk platform. A thin client over the backend contracts.
- **Vertical slice** — one workflow implemented top to bottom (UI → typed
  client → backend endpoint → aggregate) to prove the whole stack, versus
  broad shallow coverage.
- **Bearer identity** — the operator's externally-issued JWT the console
  attaches to backend calls; the backend derives `ActorContext` from it
  (Q-009). The console never asserts identity in a request body.

## 7. Functional Requirements

- **Q016-FR-001:** The console shall authenticate the operator via the
  external identity provider and attach the resulting JWT as
  `Authorization: Bearer` to backend calls; it shall handle token expiry
  (refresh or re-login) and logout.
- **Q016-FR-002:** The console shall never place actor identity in a
  request body/param the backend would trust; identity flows only via the
  verified Bearer token.
- **Q016-FR-003:** The console shall list Risk Cases via a bounded,
  paginated, authorized backend read/query endpoint, and open a selected
  case to show its detail, history/timeline, and current associations.
- **Q016-FR-004:** The console shall perform at least one Risk Case
  operation (assign / add note / change priority) end to end, sending the
  expected version and surfacing `RISK_CASE_VERSION_CONFLICT` and other
  backend `ResultCode`s meaningfully to the user.
- **Q016-FR-005:** The backend shall gain a Risk Case list/query endpoint
  (bounded pagination, authorized via the existing capability model,
  `ApiResponse` shape) without changing Q-008's aggregate or business
  rules.
- **Q016-FR-006:** The console shall render only bounded backend data,
  never expand vendor/upstream payloads, and keep no business rule
  client-side that the backend does not enforce.
- **Q016-FR-007:** The Foundation shall be runnable locally end to end
  (dev IdP or dev-token mechanism), with documented setup.

## 8. Security Requirements

- All backend authorization remains server-side; the console is not a
  trust boundary. A hidden/disabled UI control is never the security
  control — the backend rejects unauthorized calls regardless.
- Tokens are stored using the platform's secure mechanism; no token,
  credential, or sensitive case content is logged.
- The console handles `401/403` by re-authenticating or showing an
  authorization error, never by working around the backend.
- Any dev IdP / dev-token mechanism is clearly dev-only and never a
  production path.

## 9. Data / Contract Requirements

- The console consumes the backend's documented `ApiResponse` + `ResultCode`
  contracts; a typed client models them so a contract change is a compile
  -time signal.
- Pagination is bounded (server-enforced limit); the console never requests
  unbounded lists.
- The new list/query endpoint returns only bounded Risk Case summary fields
  (case number, subject, status, priority, assignment, timestamps), not
  full history or upstream payloads.

## 10. Acceptance Criteria

1. The Flutter Risk Console builds and runs on the confirmed target
   platform (§5.3(2)).
2. An operator can log in via the (dev) IdP and the console attaches the
   verified Bearer token to backend calls; identity is never body-supplied.
3. The console lists Risk Cases (paginated) and opens a case showing
   detail, history/timeline, and associations.
4. The operator can perform at least one lifecycle operation with
   expected-version handling and meaningful error surfacing.
5. The backend has a new bounded, authorized Risk Case list/query endpoint;
   no Q-008 aggregate/business rule or migration changed.
6. The whole slice runs locally end to end with documented setup.
7. Frontend tests (widget/unit + client contract) and a backend test for
   the new endpoint pass; no mandatory test skipped.
8. No Q-008…Q-014 business logic modified; the console bypasses no backend
   authorization.

## 11. Technical Constraints

- Flutter (stable channel); Dart. A defined state-management and typed
  -client architecture (chosen at the Architecture stage).
- Repo: recommended mono-repo `frontend/` alongside `backend/` (HOW,
  Architecture stage).
- The Flutter SDK is a freely-installable toolchain (unlike the Q-015
  Manager SDK); implementation needs it installed, but Requirement/
  Architecture/Design do not.
- Reuse the backend's existing security/`ApiResponse`/authorization
  patterns; add only additive read/query endpoints on the backend.
- No new production infrastructure beyond the IdP decision (§5.3(1)).

## 12. Dependencies

- Backend Q-008 (Risk Case) contracts + Q-009 JWT verification — consumed,
  not modified (except the additive list/query endpoint).
- An identity provider issuing backend-verified JWTs (§5.3(1)) — a dev IdP
  for the Foundation; production IdP is a separate infra decision.
- Flutter SDK for implementation.
- Independent of Q-015 / the MT4/MT5 SDK.

## 13. Verification Plan

- Frontend: widget/unit tests for the slice's screens and state; a typed
  -client contract test against the backend's documented responses.
- Backend: a test for the new Risk Case list/query endpoint (authorized,
  bounded pagination, correct summary projection).
- End-to-end: the documented local run (dev IdP) exercised for the login →
  list → open → operate slice.
- No credential/PII in test artifacts.

## 14. Risks and Inputs

- **The IdP decision (§5.3(1)) gates real authentication.** A dev IdP keeps
  the Foundation unblocked; the production choice is deferred infra.
- Flutter web vs desktop vs mobile (§5.3(2)) changes build/deploy and some
  UX; picking one first (recommended web) de-risks it.
- The frontend is a genuinely new surface; keeping it a thin client over
  the existing backend contracts (not a second home for business rules) is
  the main architectural discipline (Principles §5/§15).

## 15. Deliverables

- Approved `docs/requirements/Q-016-Frontend-Foundation.md`.
- Architecture, an ADR (frontend app architecture + auth flow + repo/
  client-contract decisions), and Implementation Design.
- The `frontend/` Flutter Risk Console (slice + skeleton) and the backend
  list/query endpoint.
- Lessons Learned; non-overwriting review packages under `review/q-016/`.

## 16. Review Checklist

- [x] Requirement self-reviewed by Claude Code (external Architect role).
- [ ] Product Owner Gate Decision recorded (§17).
- [x] §5.3's three decisions answered by the Product Owner (2026-09-02):
      dev Keycloak IdP now / production Keycloak by config later; web
      first; backend list/query endpoint built in this Foundation.
- [ ] Console confirmed as a thin client; no business rule moved to the
      frontend; backend authorization never bypassed.
- [ ] Only additive backend read/query endpoints; no Q-008…Q-014 business
      logic changed.

## 17. Current Gate

Q-016 Requirement status: **APPROVED — V1 — 2026-09-02 — Product Owner.**
Gate Decision: **PASS**, with §5.3 confirmed: **IdP = Keycloak (dev +
production)**; target platform = web first; the Risk Case list/query
endpoint is built in this Foundation.

Q-016 Architecture V1 / ADR-017 / Implementation Design V1: **APPROVED
(bundle) — 2026-09-02 — Product Owner.** ADR-017 is **Accepted**.

Q-016 Implementation: **AUTHORIZED — 2026-09-02 — Product Owner**, against
Requirement V1 / Architecture V1 / ADR-017 (Accepted) / Implementation
Design V1.

Q-016 Implementation Allowed: **YES.**

Q-016 Independent Implementation Review (Claude Code): **PASS WITH
CONDITIONS — 2026-09-02** — see
`review/q-016/review-q-016-v4-claude-code-independent-review-20260902-190419/`.
Backend independently reproduced (305 tests, 0 failures, real MySQL 8.4).
Independent Flutter verification found and fixed **two blocking compile
defects** (F1 `build_runner`/`freezed` dependency conflict that broke
`flutter pub get`; F2 `RiskCaseListPage` data-model/widget name collision →
15 compile errors) plus two minor test defects (F3 `AsyncValue.hasError`
absent in flutter_riverpod 3.4.2; F4 unused import). After the fixes,
`flutter analyze` reports **"No issues found!"** — all app code and all 13
test files compile and type-check for web. Codex could not have caught
F1–F4 (no Flutter/Dart SDK) and honestly declared the frontend unverified
rather than fabricating a pass.

Outstanding acceptance conditions (not code defects; executions the
reviewer's arm64 Docker harness could not run): **C1** execute the 13
frontend tests in a real browser (`flutter test --platform chrome` on a host
with a runnable Chrome); **C2** live Keycloak browser end-to-end slice
(AC 2, AC 6). See the v4 package `OutstandingConditions.md`.

Q-016 (Flutter) acceptance: **ACCEPTED — PASS WITH CONDITIONS — 2026-09-02 —
Product Owner**; committed as `45ef769` (reviewer's F1–F4 fixes included).

### Frontend framework pivot to React — 2026-09-02

After acceptance, the Product Owner decided (2026-09-02) to build the Risk
Console in **React + TypeScript** instead of Flutter. This is a Product Owner
(WHAT/strategic) decision; the Q-016 Requirement (a thin-client Risk Console)
is **unchanged**. Governance delta (Decision Authority §16.5-B connected
chain), presented as one bundle at the **frontend-pivot authorization gate**:

- **ADR-018** (`docs/adr/ADR-018-frontend-framework-react-spa.md`) — React +
  TypeScript SPA (Vite), **superseding ADR-017** on the framework/stack only;
  Status **Accepted — 2026-09-02 — Product Owner**. Component library: **Ant
  Design**.
- **React pivot architecture addendum**
  (`docs/architecture/q-016-frontend-foundation-react-pivot-addendum.md`) —
  supersedes the Flutter stack sections of the Q-016 Architecture and
  Implementation Design; the framework-agnostic content of those docs stands.
- **React implementation prompt**
  (`prompts/Q-016-React-Implementation-Prompt.md`) — Codex replaces the Flutter
  `frontend/` with the React foundation; **no backend change** (the delivered
  `GET /api/risk-cases`, dev Keycloak, and CORS are reused unchanged).

Carried over unchanged: thin-client discipline; Keycloak/OIDC Auth Code + PKCE;
the additive `GET /api/risk-cases` endpoint (already in `45ef769`); one vertical
slice + skeleton. The React stack also **resolves the C1 verification gap** —
Vitest/Playwright run headless Chromium on Apple Silicon (arm64) and CI.

Frontend-pivot gate status: **AUTHORIZED — 2026-09-02 — Product Owner** (ADR-018
Accepted; ADR-017 Superseded; component library = Ant Design). The React
implementation prompt (`prompts/Q-016-React-Implementation-Prompt.md`) is CLEARED
FOR USE.

Next gate: Codex implements the React Risk Console (replacing the Flutter
`frontend/`, no backend change) → Claude Code independent review → Product Owner
acceptance → commit. The governance pivot bundle is committed; the Flutter
`frontend/` remains in the tree until the React implementation replaces it.
