# Q-011 V18 AC15 Fix and Closure Prompt

**CLEARED FOR USE.** The Product Owner approved the Q-011 implementation
in chat on 2026-08-31, based on your own review package
(`review/q-011/review-q-011-v16-implementation-20260830-161236/`) and
Claude Code's independent review, including an independently *executed*
full test run in a separate Docker/Java 21/MySQL 8.4.11 environment
(`review/q-011/review-q-011-v17-claude-code-independent-review-20260830-163904/`).
This confirmation is also recorded in
`docs/requirements/Q-011-Evidence-Provenance-Foundation.md` §17 and in
`docs/lessons/2026-08-31-q-011-implementation-approved.md`.

Your Q-011 implementation itself requires no changes. This prompt
authorizes exactly one narrowly-scoped follow-up task.

---

## Task: fix AC15's stale Q009 test assertion — nothing else

Your own `Verification.md` correctly identified AC15's failure as
pre-existing Q-009 test staleness unrelated to Q-011, and correctly did
not touch Q-009 to fix it without authorization. That authorization is now
given, narrowly.

**File in scope: `backend/src/test/java/com/brokeros/risk/security/infrastructure/persistence/Q009MySqlIntegrationTests.java`
— and only this file.**

The test currently does this (per your own Verification.md and Claude
Code's independent confirmation):

```java
Flyway v1Flyway = Flyway.configure().dataSource(url, username, password)
        .cleanDisabled(false).target("1").load();
v1Flyway.clean();
assertThat(v1Flyway.migrate().migrationsExecuted).isEqualTo(1);

Flyway flyway = Flyway.configure().dataSource(url, username, password)
        .cleanDisabled(false).load();
assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);   // <-- stale: hard-coded "1"
flyway.validate();
```

The hard-coded `1` was already wrong once Q-010's V3 migration existed (it
should have been 2), and is now wrong again with Q-011's V4 (it should be
3). Do not simply change the literal to `3` — that repeats the same
staleness the next time a migration is added (Q-012, etc.).

**Required fix:** compute the expected count dynamically instead of
hard-coding it, e.g. by reading `flyway.info().pending().length` (or
equivalent) after the V1-only baseline is established and before the full
`migrate()` call, then asserting `migrate().migrationsExecuted` equals
that dynamically-read value. Use whichever Flyway API on the existing
Flyway 11.7.2 dependency achieves this most simply — do not add a new
dependency. Keep the rest of the test (schema/constraint/query-plan/
lifecycle assertions) unchanged.

## Hard boundaries

- Touch only `Q009MySqlIntegrationTests.java`. Do not modify any other
  Q-009 file, any Q-010 file, any Q-011 file, or any existing migration
  (V1–V4).
- Do **not** investigate or fix the separate
  `Q010BootstrapMySqlIntegrationTests.controlledCommandUsesTrustedServiceAuthorizationAndExactReplay`
  timestamp-precision issue if you happen to encounter it. It is a real,
  already-documented, deliberately deferred finding — see
  `docs/lessons/2026-08-31-q-010-bootstrap-replay-timestamp-precision.md`.
  Whether it fails or passes in your environment is expected to depend on
  your host's clock resolution; either outcome is fine. If it fails, note
  it in `Verification.md` as a known, pre-existing, out-of-scope issue
  (cite the lessons file) and do not touch any Q010 file to address it.
- Do not add, remove, or modify any other test.
- Do not stage, commit, or push.

## Verification required

Re-run the full repository-wide real-MySQL gate exactly as your
`Verification.md` already documents doing once before (all of
`Q009_MYSQL_TEST_URL`/`Q010_MYSQL_TEST_URL`/`Q011_MYSQL_TEST_URL` set,
`mvn test`), plus `scripts/verify-static.sh` and `git diff --check`.
Confirm AC15 now passes. Record the exact commands and results — including
the Q010 bootstrap test's outcome, whichever way it goes, per the note
above — in the closure package's `Verification.md`, honestly, with no
fabricated results.

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-011/review-q-011-v18-ac15-closure-<YYYYMMDD-HHMMSS>/` (check
`review/q-011/` first for the actual next unused version number)
containing at minimum: `Summary.md` (state this is a narrowly-scoped AC15
test-maintenance fix, not a re-review of Q-011 itself; restate all 15
Acceptance Criteria's status), `GitStatus.txt`, `GitDiffStat.txt` (should
show exactly one file touched), `Verification.md`, and
`OutstandingItems.md`. Add one `docs/lessons/<date>-q-011-ac15-closure.md`
entry describing the dynamic-assertion fix as the reusable pattern (avoid
hard-coding migration counts in tests that must survive future additive
migrations).

This is still not your own sign-off. Do not mark Q-011 "complete,"
"approved," or "ready for commit" in any document — Claude Code performs
an independent review of this closure package next, and the Product Owner
makes any commit decision after that, separately.

Stop after producing the closure package. Do not begin any other
Requirement, and do not act on the deferred Q010 timestamp-precision
finding beyond noting it as instructed above.
