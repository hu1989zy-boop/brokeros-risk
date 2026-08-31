# Q-011 Outstanding Items

## Blocking repository-wide gate

1. **Unchanged Q-009 migration-count assertion — blocks Acceptance Criterion
   15.** With Q009/Q010/Q011 MySQL variables enabled, the full Maven suite has
   124 tests, 123 passing and one failing:

   ```text
   Q009MySqlIntegrationTests.java:63
   expected: 1
    but was: 3
   ```

   The test migrates to V1, then invokes unrestricted Flyway migration. The
   correct current remainder is V2, V3, and V4. Its fixed count was already
   stale after V3; V4 makes the mismatch visible as 3. The implementation task
   explicitly forbids modifying any Q-009 file, so Codex did not repair or
   weaken this assertion. Claude Code/Product Owner must independently decide
   whether to authorize a separate Q-009 regression-gate maintenance change.

## Non-blocking tool risk

2. **Flyway/MySQL support warning.** Existing Flyway 11.7.2 reports formal
   tested support only through MySQL 8.1, while Q-011 requires MySQL 8.4.11.
   All V1→V4, V3→V4, validate/restart/checksum, constraint, concurrency, and
   persistence tests passed on MySQL 8.4.11. No dependency was changed because
   Q-011 requires the Maven dependency tree to remain unchanged. Independent
   review should retain this warning as an operational compatibility risk.

## Independent review boundary

3. Claude Code has not yet performed the required independent implementation
   review. This package intentionally does not call Q-011 complete, approved,
   or ready for commit.

No other known Q-011 implementation defect, skipped mandatory Q-011 test,
architecture violation, migration risk, or scope expansion remains at package
generation time. Nothing is staged, committed, or pushed.
