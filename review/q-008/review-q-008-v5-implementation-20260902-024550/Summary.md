# Q-008 Risk Case Foundation — Implementation Review Summary

## Gate Decision

**PASS WITH CONDITIONS — READY FOR INDEPENDENT IMPLEMENTATION REVIEW**

This is Codex's implementation-stage evidence package for Claude Code's
independent review. It is not Product Owner acceptance, Architect approval, or
authorization to begin another Requirement.

## Authorized boundary

- Requirement: Q-008 Risk Case Foundation, authoritative Implementation Gate
  in §26.
- Architecture: ADR-009, Accepted ADR-010, approved Implementation Design V4,
  and the authorized V5 Provider Binding Addendum.
- Prompt: `prompts/Q-008-Implementation-Prompt.md`.
- Stage: implementation, verification, and one new non-overwriting Review
  Package only.
- Prohibited actions respected: no changes under the Q-009 through Q-014
  implementation packages, no existing Flyway migration edits, no Action
  execution, Kafka topic, Redis key, IAM/RBAC invention, destructive delete,
  Git stage, commit, or push.

## Delivered implementation

- Added the `com.brokeros.risk.riskcase` modular-monolith capability with a
  framework-independent aggregate, named lifecycle operations, typed value
  objects, cohesive creation/command/association/resolution/query services,
  JDBC persistence, provider adapters, observability, and 21 named REST
  endpoints.
- Added the independent minimum `com.brokeros.risk.audit` write boundary. A
  material case change, case-owned history, and one Audit Record commit in one
  local transaction. Sensitive read operations append their audit record
  before returning content and fail closed on audit failure.
- Added immutable Flyway migration
  `V8__create_risk_case_foundation.sql`: 13 additive Q-008-owned tables,
  optimistic versioning, immutable histories, resolution cycles, exact
  uniqueness/foreign-key/check/index contracts, idempotency hashes, and no
  upstream cross-module foreign keys.
- Bound strict subject eligibility to Q-010 and reference recognition to the
  shipped Q-011 through Q-014 query services. Q-008 stores opaque references
  and does not read upstream persistence.
- Added exactly nine Q-008 ResultCodes, including the V5
  `RISK_CASE_SUBJECT_NOT_ELIGIBLE` contract.
- Added 54 Q-008 tests: 35 non-MySQL tests and 19 real-MySQL tests. The MySQL
  tests include failure-injected atomic rollback, CAS races, duplicate
  creation/association, read-audit failure, deterministic history pagination,
  and an append-only two-resolution-cycle path.
- Added the Q-008 Lessons Learned entry and a reusable aggregate/history/audit
  atomicity rule to the repository development standards.

## Verification outcome

- Q-008 suites: **54 tests, 0 failures, 0 errors, 0 skipped**.
- Full Q-009 through Q-014 plus Q-008 real-MySQL repository gate:
  **300 tests, 0 failures, 0 errors, 0 skipped** across 58 Surefire reports.
- Maven package: **PASS** on Java 21 / Maven 3.9.9.
- Dependency tree: **PASS**; no new dependency was introduced.
- Static verification: **PASS**.
- Kustomize base/test/prod render and contract checks: **PASS**.
- `git diff --check` and forbidden-boundary scans: **PASS**.
- Legacy `scripts/verify-infrastructure.sh`: **EXECUTED / FAIL** at isolated
  Compose startup because host port 6379 was occupied. Independent inspection
  also confirmed the verifier is still Q-004-specific and hard-codes V1-V3 and
  a seven-table database, so it cannot truthfully validate the current V8
  repository without separate maintenance authority. The Docker backend image
  build completed successfully before the port collision, and its isolated
  resources were cleaned up.

## Conditions requiring independent attention

1. The approved design requires authenticated assignee references but defines
   no active-actor-by-reference provider. The implementation therefore derives
   the acting principal from Q-009 `ActorContext` and validates an assignee as
   a canonical Q-009 `ActorRef`; it does not claim that Q-009 can look up the
   assignee's current active state.
2. Pre-existing governance artifacts contain stale status mirrors: the V4
   design header still says implementation is not authorized, and older
   Requirement deliverable/verification prose still describes the earlier
   governance-only stage. The authoritative Requirement §26, V5 addendum §5,
   Product Owner instruction, and implementation Prompt authorize this work.
   These governance files were not rewritten during implementation.
3. The legacy infrastructure verifier needs a separately authorized update to
   use isolated configurable host ports and the current dynamic Flyway/schema
   contract.
4. Flyway emitted its standard warning that its bundled support was tested
   through MySQL 8.1 while the required disposable server was MySQL 8.4.11.
   All migration and persistence tests nevertheless passed on MySQL 8.4.11.

No implementation acceptance criterion is knowingly left unimplemented. The
conditions above are governance/tooling/explicit-assumption disclosures for
the independent reviewer, not fabricated PASS evidence.

## Stop boundary

No other Requirement was started. Git staging, commit, and push were not
performed.
