# Q-016 Implementation Review Summary

- Requirement: Q-016 — Frontend Foundation (Flutter Risk Console)
- Lifecycle stage: Implementation
- Review package: v3 implementation, generated 2026-09-02
- Approved inputs: Requirement V1, Architecture V1, ADR-017 (Accepted), Implementation Design V1
- Baseline: `8292e13a23a93fd34a486a46b04743786f122a1f`
- Gate Decision: **PASS WITH CONDITIONS**

The authorized Q-016 source implementation is present: a Flutter web thin client under
`frontend/`, a development-only Keycloak setup and one-command launcher, and the
additive bounded `GET /api/risk-cases` endpoint. The backend endpoint and the full
Q-008 through Q-014 backend regression suite passed against a disposable real MySQL
8.4 database. Flutter and Dart were unavailable in this environment, so frontend
code generation, analysis, tests, build, browser login, and end-to-end execution were
not run. Q-016 is therefore ready for independent implementation review but is not
declared complete or approved by this package.

## Delivered scope

- Flutter web application shell with Riverpod, go_router, dio, typed API contracts,
  light/dark theme tokens, and shared loading/empty/error states.
- OIDC Authorization Code + PKCE integration for Keycloak. Access tokens remain in
  memory, refresh tokens use secure storage, Bearer attachment is centralized, one
  refresh retry is allowed on `401`, and `403` becomes a typed authorization error.
- Risk Case list, filters, bounded pagination, detail/history/association rendering,
  and add-investigation-note operation with `expectedVersion` conflict handling.
- Additive authorized Risk Case list endpoint, bounded summary projection, stable
  ordering, optional filters, server maximum page size 100, and no migration.
- Dev-only Keycloak realm/client/operator identity, least-privilege capability
  bootstrap, exact localhost CORS configuration, and documented launcher.
- Backend application/contract/real-MySQL tests plus 13 frontend test cases as source.
- Reusable Flutter Risk Console skill and an honest Q-016 Lessons Learned entry.

## Acceptance criteria

| AC | Result | Evidence / condition |
| --- | --- | --- |
| 1 | **FAIL — not verified** | Flutter SDK is unavailable; the console was not built or run. Source and run instructions are present. |
| 2 | **FAIL — not verified end to end** | PKCE/Bearer/no-body-identity behavior is implemented and statically inspected, but live Keycloak login and backend JWT verification were not exercised in a browser. |
| 3 | **FAIL — frontend not executed** | List/detail/history/associations are implemented; the backend list endpoint passed real-MySQL tests. Flutter UI execution remains outstanding. |
| 4 | **FAIL — frontend not executed** | Add-note sends only content and `expectedVersion`; typed conflict handling and source tests exist but were not run. |
| 5 | **PASS** | Bounded authorized list/query endpoint passed focused and full real-MySQL verification; no migration or aggregate rule was added. |
| 6 | **FAIL — end-to-end not executed** | Compose, realm, bootstrap, CORS, and launcher contracts were checked; Flutter absence prevented full local slice execution. |
| 7 | **FAIL — mandatory frontend tests not run** | Backend: 305 tests, 0 failures, 0 errors, 0 skipped with all MySQL aliases. Frontend: 13 test cases not executed. |
| 8 | **PASS** | Diff and regression inspection found no Q-008–Q-014 aggregate/business-rule or migration change; backend authorization remains authoritative. |

## Changed files by responsibility

- Backend list API: `RiskCaseQueryService`, `RiskCaseRepository`,
  `JdbcRiskCaseRepository`, `RiskCaseController`, list query/page/summary models, and
  REST response DTOs.
- Backend configuration/tests: dev CORS/profile configuration,
  `Q016RiskCaseListApplicationTests`, `Q016RiskCaseListMySqlTests`, and an additive
  REST contract assertion.
- Frontend: `frontend/pubspec.yaml`, app/core/feature/shared Dart sources, web shell,
  frontend tests, analysis options, ignore rules, and README.
- Development environment: `docker-compose.yml`, `.env.example`, Keycloak realm and
  security bootstrap JSON, launcher, and script/configuration documentation.
- Engineering knowledge: `docs/skills/flutter-risk-console-development.md`, skills
  index, and `docs/lessons/2026-09-02-q-016-implementation.md`.

## Scope ownership

The Q-016 governance documents, prompt, Q-015 Requirement/review material, and the
older Q-016 v1/v2 review packages were already untracked in the initial worktree.
They were treated as Product Owner inputs and were not overwritten. No Git staging,
commit, or push was performed.

## Exit condition

Stop at this implementation-review gate. Independent review must resolve the Flutter
verification conditions before Q-016 can be considered complete or approved.
