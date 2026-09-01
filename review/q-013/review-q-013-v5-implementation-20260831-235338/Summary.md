# Q-013 Action Provenance Foundation — Implementation Summary

## Gate Decision

**BLOCKED**

Q-013's implementation-specific suite passed 39 of 39 tests with zero
failures, errors, or skips. The mandatory repository-wide Q009-Q013 real-MySQL
gate ran 204 tests and failed one unchanged Q-012 migration-count assertion:
Q012MySqlMigrationTests expected one unrestricted migration after V4, while V5
and the new V6 correctly produced two. Q-013's hard boundary forbids changing
Q-012, so the implementation-verification stage cannot reach PASS without a
separately authorized Q-012 test-maintenance task.

This package is implementation evidence for independent review. It does not
mark Q-013 complete or approved.

## Stage and approved inputs

- Task: Q-013
- Stage: Implementation and Implementation Verification
- Requirement: Q-013 Requirement V1, approved
- Architecture: Q-013 Architecture V1, approved
- ADR: ADR-015, Accepted
- Implementation Design: V1, approved
- Execution authority: prompts/Q-013-Implementation-Prompt.md
- Next stage: independent implementation review, after disposition of the
  repository-gate blocker

## Implemented scope

- New com.brokeros.risk.action module with domain, application, ports,
  persistence, configuration, observability, and REST packages.
- Server-generated canonical ActionRef values.
- HUMAN-only, action:record-protected recording in Design §11.1 order.
- Exactly one Q-012 DecisionRef confirmation; no set or join table.
- Raw-field SHA-256 fingerprint and operation-ledger replay.
- MANUAL source and PROPOSED status only; no transition, correction, or delete.
- Narrow in-process confirmProvenance contract without intentText.
- Separately protected full-detail read with access audit committed before
  content disclosure.
- POST /api/actions and GET /api/actions/{actionRef}, both returning ApiResponse.
- Eight additive ACTION_* ResultCodes.
- Additive V6 migration with exactly action_record, action_operation, and
  action_access_log.
- Static Q-013 migration checks and dynamic pending-migration assertion guard.
- Required implementation lesson.

## Acceptance Criteria status

| AC | Status | Evidence |
| --- | --- | --- |
| 1 | PASS | ActionRef and ActionOperationId domain tests plus V6 CHECK tests prove canonical lowercase UUIDv4 forms. |
| 2 | PASS | Application and real security tests prove HUMAN plus action:record for recording and action:read without actor-type restriction for reads. |
| 3 | PASS | ActionRecordingService accepts RECOGNIZED and maps only NOT_FOUND to ACTION_DECISION_NOT_RECOGNIZED before mutation. |
| 4 | PASS | ActionStatus has only PROPOSED; V6 status CHECK permits only PROPOSED; no transition use case exists. |
| 5 | PASS | Main-source scan finds no correction, update, or delete use case or SQL. |
| 6 | PASS | ActionProvenanceView has no intentText field; architecture test verifies the structural boundary. |
| 7 | PASS | ActionDetailReadService audits before return; forced audit failure returns no content; JDBC adapter uses REQUIRES_NEW and is not read-only. |
| 8 | PASS | Unit and MySQL concurrency tests prove exact replay, conflict, one durable row, and no second Q-012 call. |
| 9 | PASS | Scope scan finds no Q-008, ActionOutcome, Execution, Account Control, Alert, Rule Hit, or Rule Engine implementation. |
| 10 | PASS | Action content remains free text; main-source scan finds no vendor operation taxonomy or MT4/MT5 vocabulary. |
| 11 | PASS | Git scope checks show no Q-009/Q-010/Q-011/Q-012 source or test file and no V1-V5 migration modified. |
| 12 | FAIL | All 39 Q-013 tests pass, but the mandatory 204-test repository gate has one unchanged Q-012 migration-count failure and therefore is not green. |

## Change summary

- 41 new Action production Java files, 1,520 lines.
- 8 new Action test files, 1,803 lines and 39 tests.
- One 52-line additive V6 migration.
- ResultCode.java: 16 additive lines for exactly eight codes.
- scripts/verify-static.sh: 24 insertions and 1 deletion for migration count
  six, V6 shape checks, and Q-013 dynamic-count enforcement.
- One 59-line Q-013 implementation lesson.
- Pre-existing untracked Q-013 governance documents and review packages v1-v4
  were preserved unchanged.

## Recommendation

Authorize a narrowly scoped repair of the Q012MySqlMigrationTests post-V4
hard-coded count using Flyway pending metadata, then rerun the exact Q009-Q013
real-MySQL gate. Do not begin another Requirement. Independent Q-013 review and
any Git action remain separate decisions.
