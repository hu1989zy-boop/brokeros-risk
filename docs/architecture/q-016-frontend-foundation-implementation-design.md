# Q-016 Frontend Foundation Implementation Design

> **Frontend stack superseded (2026-09-02):** the Product Owner chose **React +
> TypeScript** over Flutter. The Flutter/Dart specifics below (pubspec,
> Riverpod, go_router, dio, freezed, widget names) are replaced by **ADR-018**
> and `q-016-frontend-foundation-react-pivot-addendum.md`, and by the React
> implementation prompt `prompts/Q-016-React-Implementation-Prompt.md`. The
> framework-agnostic parts (backend `GET /api/risk-cases` design, auth flow,
> AC traceability) remain valid.

## Document Status

- Requirement: Q-016 — V1, APPROVED — 2026-09-02 — Product Owner
- Architecture: Q-016 — V1 (see its own §14)
- ADR: **ADR-017 — Accepted — 2026-09-02 — Product Owner**
- Implementation Design submission: **V1 — live status in §12 (Gate),
  authoritative if this header disagrees** (Execution Protocol §16).
- Prepared by: Claude Code, external Architect role. Part of the connected
  bundle accepted at the implementation-authorization gate.
- Implementation: **AUTHORIZED — 2026-09-02 — Product Owner** (see §12).

## 1. Scope

Turn approved Architecture V1 + ADR-017 into exact build instructions for
the Flutter web Risk Console (skeleton + one Risk Case vertical slice), the
Keycloak dev setup, and the one additive backend list/query endpoint.
Thin-client discipline (no business rules client-side; backend authorization
never bypassed). No Q-008…Q-014 business logic or migration is changed.

## 2. Repository Layout

```
frontend/                      # new Flutter web app (mono-repo)
├── pubspec.yaml               # pinned deps (see §3)
├── analysis_options.yaml      # lints (flutter_lints)
├── web/                       # web index/manifest
├── lib/
│   ├── main.dart
│   ├── app/                   # RiskConsoleApp, theme (light/dark tokens), router
│   ├── core/
│   │   ├── api/               # dio provider, ApiResponse<T>, ResultCode, Page<T>, error mapping, bearer interceptor
│   │   ├── auth/              # OIDC/Keycloak client, TokenStore, AuthController (Riverpod), auth guard
│   │   └── config/            # AppConfig (backend base URL, Keycloak issuer/clientId/redirect) from --dart-define
│   ├── features/riskcase/
│   │   ├── data/              # RiskCaseSummary/Detail/History DTOs, RiskCaseRepository (interface + dio impl)
│   │   ├── application/       # riskCaseListProvider, riskCaseDetailProvider, operation notifiers
│   │   └── presentation/      # RiskCaseListPage, RiskCaseDetailPage, operation dialogs, list/detail widgets
│   └── shared/                # LoadingView, ErrorView, EmptyView, PaginatedListView
└── test/                      # unit (notifiers w/ fake repo) + widget + contract tests
deploy/keycloak/               # dev realm export (brokeros-realm.json)
```

Backend changes are confined to the existing `riskcase` module (§7).

## 3. Frontend Dependencies (pin exact versions at implementation time)

- `flutter_riverpod` (state), `go_router` (routing), `dio` (HTTP),
  `openid_client` + `url_launcher`/`flutter_web_auth_2` (OIDC Auth Code +
  PKCE for web), `flutter_secure_storage` (refresh token),
  `freezed`/`json_serializable` (immutable typed models + JSON),
  `flutter_lints` (lints). No dependency beyond what the slice needs
  (Principles §11); pin exact versions.

## 4. Core: Typed Backend Contract

- `ApiResponse<T>` — `{ code: String, message: String, data: T?, timestamp }`
  mirroring the backend envelope; `ResultCode` Dart enum covering the codes
  the slice can receive (SUCCESS, VALIDATION_ERROR, MALFORMED_REQUEST,
  RISK_CASE_NOT_FOUND, RISK_CASE_VERSION_CONFLICT, RISK_CASE_INVALID_TRANSITION,
  RISK_CASE_INVARIANT_VIOLATION, plus a fallback for unknown codes).
- `Page<T>` — bounded page model `{ items, page, size, totalOrHasNext }`
  matching the list endpoint (§7).
- `ApiClient` (dio): base URL from config; **request interceptor** attaches
  `Authorization: Bearer <token>`; **response/error mapping** turns the
  envelope + HTTP status into a typed `Result<T>` (success or a typed
  `Failure(resultCode, message, httpStatus)`); `401` triggers the auth
  refresh hook, `403` yields an authorization failure.
- Models are immutable (`freezed`) and JSON-mapped (`json_serializable`);
  a **contract test** asserts they parse the backend's documented example
  responses.

## 5. Core: Auth (OIDC + Keycloak)

- `AppConfig` provides Keycloak `issuer`, `clientId`, `redirectUri`, and
  the backend base URL via `--dart-define` (dev values documented; prod is
  a repoint).
- `KeycloakAuth` runs **Authorization Code + PKCE**: build auth URL →
  redirect (web) → receive code → exchange (with PKCE verifier) for access
  + refresh tokens.
- `TokenStore`: access token in memory; refresh token in
  `flutter_secure_storage`. `AuthController` (Riverpod
  `AsyncNotifier<AuthState>`) exposes `login()`, `logout()`, `currentToken`,
  and a `refresh()` used by the dio `401` hook.
- `go_router` **auth guard**: unauthenticated routes redirect to `/login`;
  authenticated app routes require a valid session.
- Logout clears the store and optionally calls Keycloak end-session.
- No token or sensitive content is ever logged.

## 6. Feature: Risk Case Vertical Slice

- **List** (`RiskCaseListPage`): `riskCaseListProvider` (Riverpod) calls
  `RiskCaseRepository.list(query, page, size)` → `GET /api/risk-cases`;
  renders a `PaginatedListView` of `RiskCaseSummary` (case number, subject,
  status chip, priority, assignee, updated-at) with loading/empty/error
  states and simple filters (status/priority) + pagination controls.
- **Detail** (`RiskCaseDetailPage`): `riskCaseDetailProvider` loads
  detail + history/timeline + current associations via the existing
  backend detail/history endpoints; renders the lifecycle state, cycle,
  assignment, associated Evidence/Decision/Action/Outcome refs (as opaque
  refs), and the history timeline.
- **One operation:** an operation dialog (choose one to implement fully —
  recommend **add investigation note** and **assign**, both low-risk) that
  posts to the existing backend command endpoint with the case's
  `expectedVersion`; on `RISK_CASE_VERSION_CONFLICT` it shows a clear
  "case changed, reload" message and refetches; other `ResultCode`s are
  surfaced with their meaning. Success refreshes the detail.
- All view logic lives in Riverpod notifiers with an injected repository
  interface, unit-tested with a fake (no backend needed).

## 7. Backend Addition: `GET /api/risk-cases` list/query

Additive only, in the existing `riskcase` module; no aggregate/business/
migration change.

- **Controller:** add a `@GetMapping` on `RiskCaseController` for
  `/api/risk-cases` accepting optional `status`, `priority`, `subjectRef`,
  `assignee`, and `page` (default 0), `size` (default 20, **server-max 100**).
  `ActorContext` from the verified JWT; authorized by the existing Risk
  Case **read** capability via `AuthorizationGuard`.
- **Query service:** add `listCases(ActorContext, RiskCaseListQuery)` to
  `RiskCaseQueryService` returning a bounded `Page<RiskCaseSummary>`;
  appends a read audit consistent with the existing read-audit approach if
  and only if the existing pattern audits list reads (follow Q-008 §9.5's
  intent; if a list read is not an individually-audited disclosure, do not
  invent one — match the existing detail/history behavior and note the
  choice).
- **Repository:** add a bounded, indexed query to `JdbcRiskCaseRepository`
  (`SELECT <summary cols> FROM risk_case WHERE <optional filters> ORDER BY
  <stable key> LIMIT ? OFFSET ?`), returning only summary columns — never
  history rows or upstream payloads. Use existing indexes; add none unless
  a new migration is separately authorized (do **not** add a migration in
  this Foundation).
- **DTO:** `RiskCaseSummaryResponse` (case number, subject ref, status,
  priority, assignee ref, created-at, updated-at, version) via the existing
  response-mapping style; `ApiResponse<Page<...>>`.
- Malformed query params use the existing `VALIDATION_ERROR`/`MALFORMED_REQUEST`;
  no new `ResultCode`.

## 8. Configuration & Dev Run

- Add a **Keycloak** service to `docker-compose.yml` under a dev profile
  (image, admin creds via env, import `deploy/keycloak/brokeros-realm.json`).
  The realm seeds: a `brokeros` realm, a public web client (Auth Code +
  PKCE, redirect URIs for the local Flutter web origin), and a dev operator
  user with the capabilities the slice needs.
- Backend dev config: `SecurityJwtProperties` issuer/JWKS → the dev
  Keycloak realm.
- Document a one-command bring-up (MySQL + backend + Keycloak) and the
  Flutter web run with the dev `--dart-define`s, so the slice runs end to
  end locally. Dev creds are dev-only.

## 9. Testing

- **Frontend unit:** each Riverpod notifier (list, detail, operation) with
  a fake `RiskCaseRepository` — success, empty, error, version-conflict.
- **Frontend widget:** list/detail/operation-dialog render their
  loading/empty/error/success states.
- **Contract:** the typed models parse the backend's documented example
  `ApiResponse`/DTO JSON (guards against drift).
- **Backend:** a test for the new list endpoint — authorization enforced
  (unauthorized → denied), pagination bounded (size capped at 100), filters
  applied, summary projection correct, no unbounded query. Follows the
  Q-008 real-MySQL test pattern and the session's test-discipline lessons
  (no hard-coded migration counts; exact-name ownership assertions; do not
  add a migration).
- No credential/PII in test artifacts.

## 10. Requirement/AC Traceability

| Requirement/AC | Design section |
| --- | --- |
| Q016-FR-001/002 (OIDC, Bearer, no body identity) | §5 |
| Q016-FR-003 (list + open case) | §6, §7 |
| Q016-FR-004 (one operation + version handling) | §6 |
| Q016-FR-005 (additive backend list endpoint) | §7 |
| Q016-FR-006 (thin client, bounded) | §4, §6, §7 |
| Q016-FR-007 (runnable locally) | §8 |
| AC 1–8 | §2–§9 collectively |

## 11. Design Gaps / Outstanding

None requiring a Product Owner decision before implementation. The choice
of exactly which one operation to implement fully (recommend note + assign)
and exact library versions are implementation-time details. If the Flutter
SDK is not installed in the implementer's environment, that must be stated
honestly in Verification (like any unavailable tool), not worked around.

## 12. Design Gate

- Implementation Design submission complete: YES (V1)
- Implementation Design V1 approved: **YES — 2026-09-02 — Product Owner**
  (bundle, with Architecture V1 and ADR-017-as-accepted)
- Implementation: **AUTHORIZED — 2026-09-02 — Product Owner**
- Implementation Allowed: **YES**

Next gate: Codex executes the implementation prompt built from this Design;
Claude Code then performs an independent implementation review before any
commit.
