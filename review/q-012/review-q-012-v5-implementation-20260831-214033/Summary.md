# Q-012 Decision Provenance Foundation — Implementation Review Package

## Purpose and stage boundary

This is the new, non-overwriting implementation evidence package required by
`prompts/Q-012-Implementation-Prompt.md`. It is prepared for Claude Code's
independent implementation review. It is not an approval, and it does not mark
Q-012 complete.

The authorized stage was Q-012 implementation and verification only. No other
Requirement was started.

## Governing chain

- Requirement: `Q-012` V1, Product Owner approved.
- Architecture: Decision Provenance Foundation Architecture V1, approved.
- ADR: ADR-014, Accepted.
- Implementation Design: V1, approved; §11.1 was treated as the only canonical
  recording order and §8.5 as the canonical constraint-to-test list.
- Execution authority: Product Owner's explicit instruction to execute
  `prompts/Q-012-Implementation-Prompt.md`.

## Implemented scope

- Added the `com.brokeros.risk.decision` modular-monolith module with domain,
  application, ports, JDBC persistence, configuration, observability, and REST
  boundaries.
- Added immutable, human-recorded Decisions with Q-009 authorization, Q-010
  subject recognition, Q-011 Evidence recognition, set-valued Evidence basis,
  semantic idempotency, and a durable operation ledger.
- Added the narrow in-process `confirmProvenance` contract without conclusion
  text and a separately authorized/audited full-detail read.
- Added only `POST /api/decisions` and `GET /api/decisions/{decisionRef}`.
- Added the ten approved Decision ResultCodes.
- Added additive Flyway migration
  `V5__create_decision_provenance_foundation.sql`, creating exactly four tables
  and no seed data.
- Added 41 Q-012 tests: 22 non-database and 19 mandatory real-MySQL tests.
- Extended static verification, updated the reusable development skill, and
  added an honest Q-012 Lessons Learned entry.

## Explicitly not implemented

No Decision correction, supersession, deletion, automated source, eligibility
service, Q-008 wiring, Risk Case, Action, ActionOutcome, Alert, Rule Hit, Rule
Engine, Kafka topic, Redis key, dependency, or deployment manifest was added.
No existing Q-009/Q-010/Q-011 source or test file and no existing migration was
modified.

## Acceptance Criteria assessment

| AC | Evidence assessment | Result |
| --- | --- | --- |
| 1 | Canonical `DecisionRef` and operation UUIDv4 value objects plus domain/DB checks | PASS |
| 2 | Record authorizes then requires `HUMAN`; both reads require only `decision:read` and accept authorized `SERVICE` | PASS |
| 3 | Q-010 eligibility integration accepts both recognized states and rejects only `NOT_RECOGNIZED` | PASS |
| 4 | Q-011 provenance integration accepts active/superseded Evidence, rejects only not found, and requires at least one reference | PASS |
| 5 | Static architecture scan and route inventory prove no update/correction/supersession/delete path | PASS |
| 6 | `DecisionProvenanceView` has no conclusion field; reflective architecture test proves the structural boundary | PASS |
| 7 | Dedicated access-log transaction completes before detail return; forced-failure and concurrency tests prove fail-closed isolation | PASS |
| 8 | Operation ledger, fingerprint replay/conflict tests, and concurrent real-MySQL test prove idempotency | PASS |
| 9 | Changed-scope/static scan finds no Q-008 or deferred-domain implementation | PASS |
| 10 | Git scope confirms no existing Q-009/Q-010/Q-011 file was modified | PASS |
| 11 | Q-012 mandatory MySQL tests pass 19/19 with zero skips, but the mandated all-Q009/Q010/Q011/Q012 gate fails one unchanged Q-011 migration assertion after V5 is present | **FAIL / BLOCKED** |

## Verification headline

- Q-012 real MySQL 8.4.11 gate: **19 tests, 0 failures, 0 errors, 0 skipped**.
- All non-database regressions plus Q-012 MySQL: **165 tests, 0 failures, 0
  errors, 29 skipped**; the skips are older Q-009/Q-010/Q-011 database suites
  whose environment variables were deliberately not enabled in this separate
  diagnostic run.
- Mandatory all-Q009/Q010/Q011/Q012 real-MySQL gate: **165 tests, 1 failure,
  0 errors, 0 skipped**. The sole failure is unchanged
  `Q011MySqlMigrationTests` expecting one post-V3 migration; V4 and new V5 make
  the actual count two.
- Host-side `mvn package`: **BUILD SUCCESS**, 165 tests, 0 failures/errors, 48
  database tests skipped because this packaging check did not supply database
  variables.
- Static verification and dependency-tree checks: **PASS**.

## Gate Decision

**BLOCKED.** The Q-012 implementation and its 19 mandatory MySQL tests are
present and passing, but Acceptance Criterion 11 and Design §16.7/§18 require
the unchanged Q-009/Q-010/Q-011 regression suites to pass with all database
gates enabled. The sole failing Q-011 test is incompatible with the arrival of
V5, while the Q-012 Prompt explicitly forbids modifying any Q-011 file.
Resolving that conflict requires new authority; it was not silently worked
around. Independent review may proceed on the produced evidence, but Q-012 must
not be approved or declared complete from this package.
