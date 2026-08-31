# Q-012 Outstanding Items

## Blocking item

### OI-1 — Unchanged Q-011 migration test is not forward compatible with V5

- Mandatory command: all Q009/Q010/Q011/Q012 MySQL environment variables
  enabled, full `mvn test`.
- Result: 165 tests, 1 failure, 0 errors, 0 skipped.
- Exact failure:
  `Q011MySqlMigrationTests.migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart`,
  line 45, expected `migrationsExecuted == 1`, actual `2`.
- Cause: the test cleans, migrates to target V3, then invokes an untargeted
  migration and assumes only V4 exists. With approved additive V5 present, both
  V4 and V5 correctly run.
- Governing conflict: Q-012 Implementation Design §16.7/§18 and Requirement
  AC11 require the unchanged full regression gate to pass; the Q-012 Prompt hard
  boundary prohibits modifying any Q-011 file.
- Current treatment: no workaround, skip, migration hiding, or Q-011 edit was
  applied. Gate is BLOCKED.
- Authority needed: a separate explicit decision authorizing forward-compatible
  maintenance of the Q-011 test (or another explicit governance resolution),
  followed by a zero-failure all-database rerun. This package does not choose or
  implement that resolution.

## Non-blocking verification notes

- Flyway 11.7.2 warns that MySQL 8.4 is newer than its latest explicitly tested
  support level (8.1). Migration, validation, persistence, constraint, and
  concurrency tests nevertheless passed on actual MySQL 8.4.11. Dependency
  changes are outside Q-012 and none were made.
- A sandboxed `mvn package` run could not dynamically attach Mockito. The exact
  host-side rerun succeeded and produced the jar; this is recorded in
  `Verification.md` as an environment limitation and successful retry, not a
  product-code failure.
- Independent implementation review remains outstanding. This package is not a
  Product Owner or architect approval.

## Open questions

1. Which explicitly approved stage may repair the Q-011 forward-compatibility
   assertion without violating Q-012's hard boundary?
2. After that repair, will the independent reviewer require the all-database
   gate on isolated per-Q test schemas or the existing single disposable schema?

No other Q-012 implementation question was identified.
