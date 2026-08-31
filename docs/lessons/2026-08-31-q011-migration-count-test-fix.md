# Q-011 Migration-Count Test Fix

## Context

The Q-012 all-module real-MySQL gate exposed an obsolete assertion in
`Q011MySqlMigrationTests`: after deliberately migrating to V3, the test
hard-coded that exactly one later migration would execute. That was true while
V4 was the latest migration, but became false when the approved Q-012 V5
migration was added.

This is the second occurrence of the same defect class. The first was the Q-009
test repaired during Q-011 AC15 closure. Both failures came from treating the
number of migrations added after a historical baseline as a permanent
contract.

## Narrow fix

Immediately before the unrestricted migration, take a snapshot of Flyway's
pending metadata:

```java
int pendingMigrationCount = flyway.info().pending().length;
assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
```

The assertion still proves that every migration Flyway reported as pending was
executed. It no longer assumes that future approved migrations stop after V4.
The V3 baseline assertion and every Evidence table, emptiness, validation, and
restart assertion remain unchanged.

An assertion for an explicitly targeted fixed baseline remains valid when the
exact target version is the contract, for example `target("3")` followed by an
executed count of three. The unstable pattern is an unrestricted migration
after that baseline coupled to a hard-coded later-migration count.

## Read-only recurrence scan

The scope-limited scan noticed the same post-baseline pattern in
`Q012MySqlMigrationTests`: it targets V4 and then hard-codes one unrestricted
later migration. This task did not modify that Q-012 file, as explicitly
forbidden. A separately authorized future maintenance task should convert that
post-V4 count to Flyway pending metadata before a V6 migration causes a third
occurrence.

No other currently visible post-baseline hard-coded count was identified. The
Q-009 instance already uses the dynamic pattern.

## Verification

The repository-wide Q-009/Q-010/Q-011/Q-012 gate ran on disposable MySQL
8.4.11 with all mandatory database variables enabled: 165 tests, 0 failures,
0 errors, 0 skipped. Static verification and `git diff --check` also passed.

This lesson records narrowly authorized test maintenance. It is not an
independent review, approval, or completion declaration for Q-011 or Q-012.
