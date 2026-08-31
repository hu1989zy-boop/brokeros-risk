# Q-012 AC11 Full-Gate Closure Follow-up

## Task and stage

- Task: narrowly scoped Q-011 migration-count test maintenance authorized to
  remove Q-012 Acceptance Criterion 11's regression-gate blocker.
- Stage: implementation verification and handoff for independent review.
- Governing prompt: `prompts/Q-011-Migration-Count-Test-Fix-Prompt.md`.
- This is not a re-review of Q-011 or Q-012 and is not an approval or completion
  declaration for either Requirement.

## Authorized repair

Only
`backend/src/test/java/com/brokeros/risk/evidence/infrastructure/persistence/Q011MySqlMigrationTests.java`
was changed in test code. In
`migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart`, the stale
post-V3 literal count was replaced with Flyway's pending-migration snapshot:

```java
int pendingMigrationCount = flyway.info().pending().length;
assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
```

The V3 baseline assertion and every table, emptiness, constraint, query-plan,
validation, and restart assertion remain unchanged. No production code,
migration, dependency, Q-009/Q-010/Q-012 file, or other Q-011 file was changed
by this follow-up.

The Prompt-required lesson was added at
`docs/lessons/2026-08-31-q011-migration-count-test-fix.md`. It records that this
bug class has now recurred twice and reports, without fixing, the same latent
post-baseline pattern in Q-012's migration test.

## Q-012 Acceptance Criteria status

| Acceptance Criteria | Status after this follow-up |
| --- | --- |
| AC 1–10 | Unchanged from the v6 independent review, which found the Q-012 implementation compliant; not re-reviewed or altered here |
| AC 11 | **Resolved in implementation evidence:** full Q-009/Q-010/Q-011/Q-012 real-MySQL gate passed 165 tests, 0 failures, 0 errors, 0 skipped |

Q-012 is still not marked complete or approved. The narrow repair and the new
zero-failure gate require independent review before the Product Owner considers
any later closure or Git action.

## Verification headline

- Java 21.0.5 / Maven 3.9.9 / MySQL 8.4.11.
- Full repository real-MySQL gate: 165/165 passed, zero skipped.
- `Q011MySqlMigrationTests`: 5/5 passed, zero skipped.
- `scripts/verify-static.sh`: PASS.
- `git diff --check`: PASS.
- Authorized test diff: exactly one file, 2 insertions, 1 deletion.
- Disposable MySQL container removed after evidence capture.
- No stage, commit, or push.

## Gate Decision

**PASS** for this narrowly scoped implementation-verification stage: the exact
authorized repair is present, scope is clean, and the previously blocked
all-module gate is green. Independent review remains the next lifecycle gate;
this package is evidence for that review, not self-sign-off.
