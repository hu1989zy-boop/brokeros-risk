# Addendum — Q012 Migration-Count Test Fix Applied and Verified

Recorded 2026-09-01, after the Product Owner adopted
`docs/engineering/Architecture-and-Design-Decision-Principles.md` and
delegated (§16.5-A) that pure test-maintenance, zero-business-impact
cross-module test fixes may be applied directly by Claude Code.

## What was done

Under that delegation, Claude Code directly fixed the sole failing test
that blocked Q-013's all-module gate:
`backend/src/test/java/com/brokeros/risk/decision/infrastructure/persistence/Q012MySqlMigrationTests.java`,
line 45 — the stale unrestricted post-V4 migration-count assertion:

```java
-        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
+        int pendingMigrationCount = flyway.info().pending().length;
+        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
```

`git diff --stat`: exactly one file, 2 insertions / 1 deletion. No
production code, no migration, no schema, no other module's file touched.

## Repository-wide sweep result (the recommended broader task)

A scan of every test file for the `target("N")`-then-hard-coded-count
pattern was performed. Critical finding: there are **two** distinct
patterns, and only one is a bug:

- **Fixed-target baseline assertions** (`target("N")…isEqualTo(N)`):
  present in Q-009 (→1), Q-011 (→3), Q-012 (→4), Q-013 (→5). These assert
  "migrating from empty to VN executes exactly N migrations" — correct by
  construction, never stale, and **must remain hard-coded**. Converting
  them to a dynamic count would weaken what they verify (Execution
  Protocol §10 / delegation guardrail #3). Left unchanged.
- **Unrestricted post-baseline assertions** (the stale bug class):
  Q-009's and Q-011's were already converted to
  `flyway.info().pending().length` in their earlier fixes; Q-013's was
  authored dynamic from the start (per its Design §16.4); **Q-012's line
  45 was the only remaining stale instance**, now fixed.

Conclusion: the recurring hard-coded-migration-count bug class is now
**fully eradicated** — all four modules use the correct "fixed baseline +
dynamic increment" form. No further test file needs changing. A naive
blanket sweep would have incorrectly broken the four baseline assertions;
distinguishing the two patterns was necessary.

## Independent verification (Docker / Java 21 / MySQL 8.4, Linux)

Full repository-wide real-MySQL gate, all five datasource families
(`Q009`…`Q013`) enabled, fresh disposable container:

```
Tests run: 204, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The previously blocking `Q012MySqlMigrationTests` failure is resolved; the
all-module gate is green. Docker resources removed after the run.

## Effect on Q-013

Q-013's all-module gate blocker (AC 12 / the BLOCKED gate decision in
`V6IndependentReview.md`) is now cleared. Q-013's own implementation was
already found defect-free. The only decisions remaining are Product Owner
gates: accept Q-013's implementation, and authorize commit.
