# Q-019 Implementation Review Summary

- Task: Q-019 — Risk Case Association Projection Endpoint
- Lifecycle stage: Implementation + Implementation Verification
- Authorized inputs: Requirement V1 (APPROVED), ADR-021 (Accepted),
  Architecture V1, Implementation Design V1, and the CLEARED implementation prompt
- Baseline: `a9cc877e77363d912e1799695cca8574a5b61371` on `main`
- Package generated: 2026-09-03 20:49 +08:00
- Gate Decision: **PASS WITH CONDITIONS**

The authorized Q-019 code scope is implemented and all locally executable gates
pass. The condition is limited to AC4: the live Keycloak/backend/browser slice
could not run without an operator credential and seeded Risk Case, Decision, and
Action references. The committed-shape Playwright spec was discovered and
reported one skip; no live result is claimed.

This package is evidence for Claude Code's independent implementation review. It
does not self-approve Q-019 or authorize another Requirement.

## Delivered scope

- `GET /api/risk-cases/{caseNumber}/associations` on the existing controller,
  using `risk-case:read`, the standard `ApiResponse`, and the standard not-found
  contract.
- A bounded application projection and REST DTO containing all evidence
  association event refs/dispositions/sources/replacements, all associated
  decisions with the current marker, and effective actions with outcome refs.
- Exactly two additive repository reads: all evidence association events and all
  decision associations. Existing effective-evidence/effective-action reads are
  reused; no DDL or migration was added.
- A real-MySQL HTTP contract/gateway class covering correct projection, 403 before
  target lookup, 404, and the per-collection server cap.
- Typed frontend contract parsing, repository method, TanStack Query hook, and an
  authoritative `AssociationsPanel`.
- Evidence disposition now uses an on-case `eventRef` picker. Decision selection
  and action outcome targeting also consume the authoritative projection.
- Association success invalidates the projection; a version conflict actively
  refetches both detail and projection. Tests exercise both paths.
- A live Q-019 Playwright specification for decision association → current
  selection → action association → projection rendering → resolve → close.
- Q-019 Lessons Learned and this new, non-overwriting review package.

## Acceptance criteria

| AC | Result | Evidence |
| --- | --- | --- |
| 1 | **PASS** | `Q019RiskCaseAssociationsMySqlTests`: 4/4 against MySQL 8.4, including event refs, supersession/replacement, two decisions/one current, action outcome, authorization, not-found, and cap. |
| 2 | **PASS** | Boundary `git diff --name-only` returned no aggregate/domain, migration, capability, association/command/resolution write-service changes. Only additive read/API/test and console files changed. |
| 3 | **PASS** | Projection parser/repository/panel/picker tests pass; the history-reconstruction caveat and manual evidence-event UUID input are removed. |
| 4 | **FAIL (environment not available)** | The Q-019 Playwright spec exists and is discovered, but the required credential and seeded refs were absent: 1 discovered, 1 skipped, 0 passed, 0 failed. Live resolve/close is not claimed. |
| 5 | **PASS** | Backend full MySQL gate: 309/309. Frontend: 12 files, 150/150; typecheck and production build passed. |

## Scope and safety result

- No Risk Case aggregate/domain rule or write-service change.
- No Flyway migration, table, column, DDL, Kafka, Redis, adapter, or dependency
  manifest change.
- No new capability or bootstrap grant; the endpoint reuses `risk-case:read`.
- No other module endpoint or Option B browse/search work.
- No staging, commit, or push. Three pre-existing untracked Q-016/Q-017/Q-018 ZIP
  files were preserved unchanged.
- The disposable `brokeros-q019-mysql-20260903` container was removed after the
  final database verification.

## Exit condition

Stop at the Q-019 implementation-verification handoff. Independent review and a
credentialed/seeded AC4 live run remain outside this implementation turn.
