# Q-013 Outstanding Items

## Blocking item

### OI-1 — Q-012 forward-compatible migration-count assertion

Severity: blocking for Q-013 Implementation Verification AC12.

The required Q009-Q013 real-MySQL gate failed only at:

- File: backend/src/test/java/com/brokeros/risk/decision/infrastructure/persistence/Q012MySqlMigrationTests.java
- Test: migrationUpgradesV4CreatesExactlyFourTablesWithoutSeedsAndValidatesOnRestart
- Line: 45
- Result: expected 1, but V5 plus V6 executed 2

Root cause: the unchanged Q-012 test targets V4 and then couples an unrestricted
Flyway migration to a hard-coded count. This exact future-migration defect was
already recorded in docs/lessons/2026-08-31-q011-migration-count-test-fix.md.

Required disposition: a separately authorized, Q-012-scoped maintenance task
should snapshot flyway.info().pending().length before the unrestricted migrate
call, change only that stale count assertion, and generate its own review
package. Q-013 authority explicitly forbids this repair.

After that repair, rerun the exact Q009-Q013 full real-MySQL gate. Until then,
Q-013 AC12 is FAIL and this stage is BLOCKED.

## Non-blocking observations

- Flyway 11.7.2 warns that MySQL 8.4 is newer than its latest explicitly tested
  MySQL version (8.1). All 17 Q-013 MySQL 8.4.11 tests passed; the warning is
  recorded rather than suppressed.
- The first sandboxed MySQL attempt failed because loopback socket creation was
  prohibited. Host-context re-execution passed; no code workaround was made.
- Independent implementation review has not occurred.
- Git staging, commit, and push have not occurred.

## Deferred by governance, not implementation gaps

A future Requirement must define any Action approval workflow or new status.
ActionOutcome, Execution, Account Control, Risk Case wiring, and vendor-specific
operations remain intentionally unimplemented.

## Gate Decision

**BLOCKED** — no in-scope Q-013 code finding remains, but the mandatory
repository gate is not green.
