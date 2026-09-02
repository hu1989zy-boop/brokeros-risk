# ADR-017: Frontend Foundation (Flutter Risk Console)

- Status: Accepted 2026-09-02, now **Superseded by ADR-018 — 2026-09-02 —
  Product Owner** — the Product Owner decided to build the Risk Console in
  **React + TypeScript** instead of Flutter and accepted ADR-018 at the Q-016
  frontend-pivot gate. The framework/stack decision in this ADR is replaced by
  **ADR-018**; the framework-agnostic decisions (thin client; Keycloak/OIDC
  Auth Code + PKCE; the additive `GET /api/risk-cases` endpoint; one vertical
  slice + skeleton) are carried forward by ADR-018 unchanged. This ADR remains
  as the historical record of the Flutter foundation delivered in `45ef769`.
- Date: 2026-09-02
- Approval origin: drafted under §16.5-B after Q-016 Requirement V1
  approval (2026-09-02). Prepared by Claude Code, external Architect role.
  Self-review artifact.
- Requirement: Q-016 — Frontend Foundation V1, APPROVED — 2026-09-02
- Architecture: `docs/architecture/q-016-frontend-foundation-architecture.md`
- Depends on: the committed backend Q-008 (Risk Case) contracts and Q-009
  (JWT verification). Introduces no dependency on Q-015.
- Supersedes: None
- Superseded by: **ADR-018 (Frontend Framework — React + TypeScript SPA)**,
  Accepted 2026-09-02 — on the framework/stack decision only.

## Context

The backend now has a real Core-Domain chain culminating in the Q-008 Risk
Case aggregate — the first thing a human operator works with, and the first
worth a UI. There is no frontend. The Product Owner chose Flutter, and at
the Requirement gate confirmed: Keycloak as the identity provider (dev +
production), web as the first target, and building the one backend Risk
Case list/query endpoint the console needs as part of this Foundation.

Two backend facts shaped this: authentication is delegated to an external
IdP (Q-009 verifies externally-issued JWTs; there is no in-backend login),
and read/query APIs are minimal (no Risk Case list/search endpoint exists).

## Decision

### A Flutter web "Risk Console" as a thin client

Q-016 introduces a Flutter web application, the **Risk Console**, in a
mono-repo `frontend/` directory beside `backend/`. It is a **thin client**:
all authorization and business rules stay in the backend; the console
renders bounded backend data, surfaces backend `ResultCode`/error
contracts, and never re-decides transitions, invariants, or authorization
(Principles §5/§15). It may disable a control the backend would reject for
UX, but always calls and honors the backend's answer.

### Authentication via Keycloak using OIDC Authorization Code + PKCE

The console authenticates the operator against **Keycloak** using **OIDC
Authorization Code flow with PKCE** — the user authenticates at Keycloak
(never in the app), the app obtains a signed access JWT (+ refresh token),
and sends it as `Authorization: Bearer` on every backend call. The backend
verifies the JWT against Keycloak's JWKS (Q-009) and derives `ActorContext`.
The console never places identity in a request body/param. Access token in
memory, refresh token in secure storage; silent refresh on `401`;
authorization error on `403`. Because verification is by configured
issuer/JWKS, dev→production Keycloak is a configuration repoint, not a code
change. A dev Keycloak (seeded realm/client/user, via a docker-compose dev
profile) makes the vertical slice runnable end to end locally.

Keycloak is chosen for both dev and production: self-hosted, free, and
capable of MFA/roles/SSO — fitting a self-hosted broker risk platform,
and it issues the standard OIDC JWTs Q-009 already verifies.

### Frontend technical stack: Riverpod + go_router + dio + a typed contract

- **Riverpod** for state management (async-first, testable, low
  boilerplate; notifiers wrap API calls and auth/session state and are
  unit-testable with a fake repository).
- **go_router** for declarative routing with an auth guard.
- **dio** as the HTTP client, with an interceptor that attaches the Bearer
  token and maps the backend's `ApiResponse`/`ResultCode` envelope.
- A **typed Dart contract** (models for `ApiResponse<T>`, the consumed
  DTOs, and a `ResultCode` enum) so a backend contract change is a
  compile-time signal, not a runtime surprise.

These are HOW decisions (Decision Authority §16.1); they are recorded here
because the frontend stack is a durable, hard-to-cheaply-reverse choice
(Reversibility §12) that all later frontend work builds on.

### One additive backend list/query endpoint; no aggregate change

The only backend change is one additive, read-only `GET /api/risk-cases`
list/query endpoint (bounded pagination, authorized by the existing Risk
Case read capability, `ApiResponse<Page<RiskCaseSummary>>` with a bounded
summary projection). It reuses the existing controller/query-service/
repository patterns and does **not** touch Q-008's aggregate, business
rules, or migrations. Q-008 shipped with get-by-number/detail/history but
no list/search; a usable operator console needs list/query, so it is built
here rather than deferred.

### One vertical slice + skeleton, not the whole UI

The Foundation delivers the app skeleton (auth, routing, theming, typed
client, error/loading UX) plus **one end-to-end vertical slice** over Risk
Case (login → list → open case with detail/history/associations → one
operation with optimistic-version handling), proving the whole stack top
to bottom. Broad shallow coverage of every module is deferred — the value
of a Foundation is a proven stack, not feature breadth.

## Alternatives Considered

### Alternative 1: In-backend username/password login instead of an external IdP

Rejected — it contradicts Q-009's deliberate verify-only design and would
put password/MFA/session handling (hard, high-risk) into the risk backend.
Delegating to Keycloak is the security-best-practice choice the backend was
built for.

### Alternative 2: Bloc instead of Riverpod

Rejected as the default — both are viable; Riverpod gives comparable
testability with less boilerplate and first-class async/provider
composition for API-driven state. A HOW preference, recorded for
consistency.

### Alternative 3: Separate frontend repository

Rejected for now — a mono-repo `frontend/` keeps one build/version/review
surface while the frontend is young; it can be extracted later without a
code change. Matches the "spend reversal effort proportional to cost"
principle (§12).

### Alternative 4: Build a dashboard / broad UI first

Rejected — a real-time exposure dashboard needs Q-015 trading data, which
is not ingested yet; and broad shallow UI does not prove the stack. One
Risk Case vertical slice is the right Foundation.

### Alternative 5: OIDC Implicit flow / password grant

Rejected — Authorization Code + PKCE is the current OIDC best practice for
public clients (SPAs); implicit and password grants are discouraged/
deprecated for security reasons.

## Consequences

Positive:

- Gives risk operators a real interface over the Q-008 Risk Case they
  otherwise drive by raw HTTP.
- Establishes a durable, testable frontend stack all later UI builds on.
- Proves the full authenticated stack (Keycloak → JWT → Q-009 → Q-008) end
  to end.
- Keeps the security model intact (backend-enforced authorization; thin
  client).

Costs and constraints:

- Introduces Keycloak as a new deployment component (the one new infra the
  Requirement authorized).
- Introduces a Flutter/Dart toolchain the team must install to build/run
  the frontend (freely installable).
- The frontend must be disciplined to stay a thin client; a future
  Requirement is needed for broad UI, dashboards, or any client-side logic
  beyond rendering + calling.

## Security Implications

Authorization stays server-side (Q-009); the console is not a trust
boundary. OIDC Auth Code + PKCE; no implicit/password grant. Access token
in memory, refresh token in secure storage; no token/sensitive content
logged. Bounded data only; tightly-scoped CORS/redirect URIs; TLS in
production.

## Data / Contract Implications

The console consumes the backend's `ApiResponse`/`ResultCode` contracts via
typed Dart models. The new list endpoint returns a bounded summary
projection with server-enforced pagination — no unbounded queries, no full
history/upstream payloads over the list path.

## Operational Implications

Adds Keycloak (dev via docker-compose profile; production as a deployed
service repointed by config). No other new infrastructure. Flutter web
build artifacts served as static assets (deployment detail). The backend
change is one additive read endpoint.

## Dependencies

Backend Q-008 contracts + Q-009 JWT verification (consumed; only the
additive list endpoint added). Keycloak (IdP). Flutter SDK (build-time).
Independent of Q-015 / the MT4/MT5 SDK.

## Deferred Decisions

Implementation Design fixes exact library versions, widget composition,
query-param validation, page-size cap, Keycloak realm export, and file
names. Future Requirements: broad UI, dashboards (post-Q-015), desktop/
mobile targets, production Keycloak HA/hardening, SSO federation.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-09-02**, together with
acceptance of the Q-016 Architecture/Design bundle and implementation
authorization at the implementation-authorization gate (Decision Authority
§16.5-B). Drafting under §16.5-B was not itself acceptance; this ADR governs
from that explicit acceptance.
