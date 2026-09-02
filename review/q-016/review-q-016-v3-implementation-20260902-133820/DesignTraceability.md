# Q-016 Design Traceability

## Functional requirements

| Requirement | Implementation | Test/evidence | Status |
| --- | --- | --- | --- |
| Q016-FR-001 | `auth_gateway.dart`, `browser_pkce_authenticator.dart`, `auth_controller.dart`, `api_client.dart`, router guard | Static inspection; frontend tests unavailable | Implemented; runtime unverified |
| Q016-FR-002 | Central Bearer interceptor; `addNote` body contains only `content` and `expectedVersion`; backend JWT context unchanged | `api_contract_test.dart`; source inspection | Implemented; frontend test unexecuted |
| Q016-FR-003 | List/detail pages, notifiers, repository; backend list controller/service/repository; existing detail/history routes | Focused backend tests: 9/9 pass; frontend source tests | Backend verified; UI unverified |
| Q016-FR-004 | Add-note dialog/notifier/repository with expected version; explicit conflict reload/message | Notifier and widget test cases present | Implemented; tests unexecuted |
| Q016-FR-005 | `GET /api/risk-cases`, summary DTO/model, server cap, existing READ authorization | Q016 application + real-MySQL + REST contract tests; full regression | PASS |
| Q016-FR-006 | Summary-only list; page size 20 client/100 server; history limit 100; no client business rule | SQL/source inspection and real-MySQL cap test | PASS for source/backend |
| Q016-FR-007 | Compose console profile, Keycloak realm/bootstrap, dev profile/CORS, launcher and READMEs | Compose render, JSON/static/shell checks; no live Flutter run | Implemented; E2E unverified |

## Implementation Design sections

| Design section | Implementing artifact | Verification |
| --- | --- | --- |
| §1 Scope | Q-016-only frontend/dev setup plus additive list query | Git scope and migration inspection |
| §2 Repository Layout | `frontend/` app/core/features/shared/test/web layout | Bounded project tree |
| §3 Frontend Dependencies | Exact versions in `frontend/pubspec.yaml` | Static inspection; resolution unavailable |
| §4 Typed Backend Contract | `api_contract.dart`, `api_client.dart`, Freezed/JSON DTOs | Three contract test cases present; unexecuted |
| §5 Auth | PKCE authenticator, Keycloak gateway, secure refresh store, auth controller, route guard | Source/security inspection; browser flow unexecuted |
| §6 Risk Case Slice | list/detail/note UI, repository, providers/notifiers, shared states | Ten notifier/widget cases present; unexecuted |
| §7 Backend List Endpoint | query/page/summary application types, controller/DTO, repository SQL | 9 focused tests pass; full 305-test MySQL gate passes |
| §8 Configuration & Dev Run | Compose profile, realm, capability bootstrap, dev YAML/CORS, launcher/docs | Compose config, JSON assertions, shell syntax, bootstrap jar against MySQL |
| §9 Testing | backend tests and 13 frontend source tests | Backend executed; Flutter tests not executed |

## Design choices resolved during implementation

- Implemented add investigation note as the required single command. Assignment was
  not added because one operation satisfies the approved Foundation scope.
- Used `hasNext` rather than an expensive total count, as expressly allowed by the
  typed page design.
- Used a small web-specific wrapper around `openid_client`'s authorization-code PKCE
  flow. Its browser convenience authenticator was not used because its implementation
  builds an implicit flow, which conflicts with ADR-017.
- Did not add list-read audit records: the existing audit factory and detail/history
  pattern are bound to one Risk Case, while a list response is multi-case.
- Left Freezed/JSON generated sources and `pubspec.lock` absent. They must be generated
  by the pinned toolchain after Flutter is installed, then independently reviewed.
