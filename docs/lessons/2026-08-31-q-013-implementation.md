# Q-013 Action Provenance Foundation — Implementation Lessons

## Scope

Implementation and verification of Q-013 against approved Requirement V1,
Architecture V1, ADR-015 (Accepted), and Implementation Design V1. The work
adds the Action module, V6 migration, protected recording and two-tier reads,
tests, and review evidence. It does not approve or close Q-013.

## What worked

- Treating Implementation Design §11.1 as the only recording-order authority
  kept authorization, the HUMAN restriction, raw semantic fingerprinting,
  replay, content validation, Q-012 provenance confirmation, context creation,
  and mutation directly testable.
- Modeling the originating Decision as one DecisionRef, rather than copying
  Q-012's Evidence set mechanics, kept both the domain and the three-table
  schema aligned with Q013-FR-001.
- The narrow ActionProvenanceView has no intentText component, while the
  full-detail service commits a dedicated access-log transaction before
  returning content. This enforces the approved two-tier split structurally.
- Real MySQL 8.4.11 tests proved the V6 constraints, both internal foreign-key
  restrictions, atomic record/ledger writes, idempotent concurrency, bounded
  generated-reference retries, strict UTF-8 reads, and audit-failure isolation.

## Problems found honestly

- The first Q-013 MySQL execution was attempted inside a restricted sandbox.
  The JVM could not open the loopback socket and all 17 database tests ended
  with SocketException: Operation not permitted. No code was changed for
  that environmental failure. Re-execution in the approved host context
  connected to the same disposable MySQL 8.4.11 container and passed all
  17 tests with zero failures, errors, or skips.
- The required Q009-Q013 full database gate ran 204 tests and exposed the
  already-documented forward-compatibility defect in the unchanged
  Q012MySqlMigrationTests: its unrestricted migration after a V4 baseline
  still hard-codes one later migration. V5 plus Q-013's V6 correctly produce
  two pending migrations, so the assertion failed (expected: 1, but was:
  2). Q-013 expressly forbids modifying Q-012. The failure is recorded as a
  separately-authorized prerequisite, not hidden or repaired out of scope.

## Reusable rule applied

The recurrence was predicted in
docs/lessons/2026-08-31-q011-migration-count-test-fix.md. Q-013's own
migration suite snapshots flyway.info().pending().length immediately before
the unrestricted migration and asserts that exactly that many migrations
execute. scripts/verify-static.sh now rejects the hard-coded unrestricted
pattern in Q-013's test.

No new docs/skills rule was added because the reusable rule already exists in
the cited lesson and the repository static gate now enforces it for Q-013.

## Stage boundary

All 39 Q-013 tests pass, but the mandatory repository-wide gate is not green
because of the unchanged Q-012 test. The implementation-verification gate must
therefore report the external blocker honestly and stop before independent
review or any Git operation.
