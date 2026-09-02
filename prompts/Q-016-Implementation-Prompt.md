# Q-016 Frontend Foundation Implementation Prompt

**CLEARED FOR USE — Product Owner authorized implementation 2026-09-02.**
Q-016's Requirement V1 was approved (IdP = Keycloak; web-first; the Risk
Case list/query endpoint built in this Foundation), and — as one bundle at
the implementation-authorization gate per Decision Authority §16.5-B —
Architecture V1, ADR-017 (Accepted), and Implementation Design V1, with
implementation explicitly authorized. Recorded in
`docs/requirements/Q-016-Frontend-Foundation.md` §17 and each governing
document's own gate section. Governed by the two `docs/engineering/`
documents — read them first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-016-Frontend-Foundation.md` (V1, APPROVED).
2. `docs/adr/ADR-017-frontend-foundation.md` (Accepted).
3. `docs/architecture/q-016-frontend-foundation-architecture.md` (V1,
   APPROVED).
4. `docs/architecture/q-016-frontend-foundation-implementation-design.md`
   (V1, APPROVED) — the authoritative build spec.

Also read for context and **reuse/consume unchanged** (do not modify their
business logic): `com.brokeros.risk.security.*` (Q-009 JWT verification)
and `com.brokeros.risk.riskcase.*` (Q-008) — the only backend change
permitted is the one additive read/query endpoint in Design §7.

## The confirmed shape — summary; the four documents are authoritative

- A **Flutter web** "Risk Console" in a mono-repo **`frontend/`** directory
  (Design §2), thin client: no business rules client-side, backend
  authorization never bypassed, bounded data only.
- Stack: **Riverpod** + **go_router** + **dio** + a typed Dart
  `ApiResponse<T>`/`ResultCode`/`Page<T>` contract (Design §3–§4).
- Auth: **OIDC Authorization Code + PKCE against Keycloak** (Design §5);
  the app never sees a password; obtains a JWT and sends
  `Authorization: Bearer`; the backend (Q-009) verifies it. Access token in
  memory, refresh token in secure storage; `401`→silent refresh, `403`→auth
  error. Never place identity in a request body/param.
- One **vertical slice** over Risk Case (Design §6): login → list → open
  case (detail + history + associations) → one operation (recommend **add
  investigation note** and **assign**, both low-risk) with `expectedVersion`
  and `RISK_CASE_VERSION_CONFLICT` handling.
- One **additive backend endpoint** `GET /api/risk-cases` (Design §7):
  bounded pagination (server max size 100), authorized via the existing
  Risk Case read capability, `ApiResponse<Page<RiskCaseSummary>>` with a
  bounded summary projection — **no Q-008 aggregate/business/migration
  change; add no new migration**; do not invent a list-read audit if the
  existing detail/history pattern does not audit list reads (match the
  existing behavior and note the choice).
- Dev run: a **Keycloak** service in `docker-compose.yml` under a dev
  profile with a seeded realm/client/dev-user, and the backend dev
  `SecurityJwtProperties` pointed at it, so the slice runs locally end to
  end (Design §8).

## Task

Implement Q-016 exactly as specified in Implementation Design V1, and only
that: the `frontend/` Flutter web Risk Console (skeleton + the one Risk
Case vertical slice), the Keycloak dev setup, and the one additive backend
`GET /api/risk-cases` endpoint. Include the tests in Design §9 (frontend
unit + widget + typed-client contract; backend endpoint test), and the
documented one-command local bring-up.

## Hard boundaries — do not do these

- Do not modify any Q-008…Q-014 **business logic**, aggregate, or migration.
  The only backend change is the additive `GET /api/risk-cases` read
  endpoint (Design §7); **add no new Flyway migration** in this Foundation.
- Do not move any business rule, invariant, transition decision, or
  authorization into the frontend. The console renders backend data and
  honors backend answers; it may disable a control for UX but always calls
  the backend and respects its result.
- Do not build an in-backend login/password flow or an IdP; auth is OIDC
  against Keycloak (Q-009 stays verify-only). Do not use OIDC implicit or
  password-grant flows — Authorization Code + PKCE only.
- Do not expose unbounded list queries; the list endpoint's page size is
  server-capped (max 100) and returns only the bounded summary projection.
- Do not add new production infrastructure beyond the Keycloak already
  decided; do not add Kafka/Redis usage.
- Do not put any token, credential, or sensitive case content in logs or
  test artifacts; dev credentials are dev-only.
- Do not stage, commit, or push. Do not touch any existing timestamped
  review package.
- Do not silently reinterpret a contradiction; if you find one, resolve it
  toward the approved documents and record the assumption in
  `OutstandingItems.md`.

## Environment honesty (important)

Building/running the Flutter app requires the **Flutter SDK** installed. If
it is not available in your environment, **say so explicitly in
`Verification.md`** and report what you could and could not build/run —
do NOT fabricate a passing frontend build/test. In that case, still deliver
the complete, correct frontend source + the backend endpoint + all tests
as code, verify the **backend** endpoint against real MySQL (that toolchain
is present), run the repository test gate for the backend, and clearly
record the frontend build/test as "not executed — Flutter SDK unavailable"
for Claude Code's independent review. Never claim a check passed that did
not run.

## Required output

After implementation and verification, create ONE new, non-overwriting,
timestamped review package at
`review/q-016/review-q-016-v<N>-implementation-<YYYYMMDD-HHMMSS>/` (check
the directory for the next unused version) containing at minimum:
`Summary.md`, `ArchitectureReview.md`, `DesignTraceability.md` (map each
Q016-FR-XXX + each Design section to the implementing code/test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md`
(exact commands, environment/tool availability, pass/fail/skip counts —
honest; explicitly state Flutter SDK availability and what was/wasn't run),
`SecurityReview.md`, `TestInventory.txt`, and `OutstandingItems.md`. Add a
`docs/lessons/<date>-q-016-implementation.md` entry.

Run the full repository-wide real-MySQL **backend** gate (Q-009…Q-014 +
Q-008 datasources) plus the new endpoint's test; report honestly even if
something fails outside this task's boundary.

This review package is for Claude Code's independent implementation review,
not your own sign-off — do not mark Q-016 "complete" or "approved"; state
PASS/FAIL against each acceptance criterion honestly and list every open
question and assumption.

Stop after producing the review package. Do not begin any other
Requirement.
