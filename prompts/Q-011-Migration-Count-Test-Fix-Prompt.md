# Q-011 Migration-Count Test Fix Prompt (Q-012 Closure Follow-up)

**CLEARED FOR USE.** The Product Owner authorized this fix in chat on
2026-08-31, after Claude Code's independent review of Q-012's
implementation (`review/q-012/review-q-012-v6-claude-code-independent-review-20260831-224500/`)
confirmed: Q-012's own implementation has no defect, but the mandatory
all-module real-MySQL gate is BLOCKED by one unchanged, pre-existing
Q-011 test whose hard-coded migration count went stale the moment Q-012's
V5 migration was added — the exact same bug class as AC15
(`Q009MySqlIntegrationTests`, fixed 2026-08-31 for Q-011's own closure).
This work is governed by
`docs/engineering/AI-Engineering-Execution-Protocol.md` — it applies to
you as much as to Claude Code.

## The bug

```
Q011MySqlMigrationTests.migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart
expected: 1
 but was: 2
```

File: `backend/src/test/java/com/brokeros/risk/evidence/infrastructure/persistence/Q011MySqlMigrationTests.java`.
The test establishes a `target("3")` baseline, then calls an unrestricted
`flyway.migrate()` and hard-codes the expectation that exactly `1` further
migration executes:

```java
Flyway flyway = Flyway.configure().dataSource(dataSource).load();
assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
```

That was correct when only V4 existed beyond V3. With Q-012's V5 now
present, two migrations (V4, V5) execute, so the hard-coded `1` is stale
— exactly the class of defect already fixed once for
`Q009MySqlIntegrationTests`.

## Required fix — one file, the same pattern already used once

Apply the identical fix pattern used for AC15
(`docs/lessons/2026-08-31-q-011-ac15-closure.md`): derive the expected
count dynamically from Flyway's own pending-migration metadata instead of
hard-coding it.

```java
Flyway flyway = Flyway.configure().dataSource(dataSource).load();
int pendingMigrationCount = flyway.info().pending().length;
assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
```

Do not simply change the literal `1` to `2` — that repeats the same
staleness the next time any migration (Q-013, etc.) is added. The
dynamically-derived count is the required fix, not a hard-coded update.

## Hard boundaries

- Touch only
  `Q011MySqlMigrationTests.java` — and only this one assertion's logic
  (the two lines above). Do not modify any other assertion in this file,
  any other Q-011 file, any Q-009/Q-010/Q-012 file, or any migration.
- Do not weaken the rest of this test (the four-table
  existence/emptiness/checksum/restart assertions immediately following
  must remain exactly as they are).
- Do not stage, commit, or push.

## Verification required

Re-run the full repository-wide real-MySQL gate
(`Q009_MYSQL_TEST_URL`/`Q010_MYSQL_TEST_URL`/`Q011_MYSQL_TEST_URL`/
`Q012_MYSQL_TEST_URL` all set, `mvn test`), plus `scripts/verify-static.sh`
and `git diff --check`. Confirm the full gate now passes with zero
failures (165/165 or whatever the then-current total is). Record exact
commands and results honestly in `Verification.md` — no fabricated
result.

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-012/review-q-012-v7-full-gate-closure-<YYYYMMDD-HHMMSS>/`
(check `review/q-012/` first for the actual next unused version number)
containing at minimum: `Summary.md` (state this is a narrowly-scoped
Q-011 test-maintenance fix undertaken to close Q-012's AC 11, not a
re-review of Q-011 or Q-012 themselves; restate Q-012's Acceptance
Criteria status with AC 11 now resolved), `GitStatus.txt`,
`GitDiffStat.txt` (should show exactly one file touched), `Verification.md`,
and `OutstandingItems.md`. Add one
`docs/lessons/<date>-q011-migration-count-test-fix.md` entry recording
that this exact bug class (a test hard-coding a post-baseline migration
count) has now recurred twice — once in Q-009's own test, once in
Q-011's — and recommend, as a lesson for the future, that any other
existing test using this same `.target("N")`-then-hard-coded-count
pattern should be checked and converted the same way before it causes a
third occurrence (do not go find and fix other instances yourself in this
task — only report whether you noticed any while working, as a
recommendation for a separate future task).

This is still not your own sign-off. Do not mark Q-011, Q-012, or this
fix "complete" or "approved" anywhere. Claude Code performs an
independent review (including re-executing tests) before the Product
Owner considers any commit.

Stop after producing the review package. Do not begin any other task.
