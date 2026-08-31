# Independent Review of Codex's V18 AC15 Closure Package

- Subject: `review/q-011/review-q-011-v18-ac15-closure-20260831-125801.zip`
  (7 files: `Summary.md`, `Verification.md`, `GitDiffStat.txt`,
  `GitStatus.txt`, `ArchitectureReview.md`, `OutstandingItems.md`,
  `ProjectTree.txt`).
- Note: an earlier chat exchange flagged that this package appeared to be
  missing at the time Codex's `docs/lessons/2026-08-31-q-011-ac15-closure.md`
  referenced it. It was a timing issue, not a fabricated reference — Codex
  delivered the zip afterward and it is now present and reviewed here.

## Code-diff verification (direct inspection, not just reading the package)

Confirmed via `git diff` myself, independent of the package's own
`GitDiffStat.txt`: the only change is in
`backend/src/test/java/com/brokeros/risk/security/infrastructure/persistence/Q009MySqlIntegrationTests.java`,
exactly 2 insertions / 1 deletion:

```java
int pendingMigrationCount = flyway.info().pending().length;
assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
```

This matches Codex's `Summary.md`/`Verification.md`/`GitDiffStat.txt`
exactly, and matches the authorized scope in
`prompts/Q-011-V18-AC15-Fix-And-Closure-Prompt.md` precisely — no other
Q-009 file, no Q-010 file, no Q-011 file, and no migration was touched.
This is exactly the fix I recommended: derive the expected count from
Flyway's own pending-migration metadata rather than hard-coding a number
that goes stale with every future additive migration.

## Independently executed re-verification (not just reading Codex's Verification.md)

Codex's own `Verification.md` reports the fix verified on their macOS/JBR21
host: 124 tests, 0 failures, 0 errors, 0 skipped — including
`Q010BootstrapMySqlIntegrationTests` passing there. That host's clock did
not expose the sub-microsecond entropy that caused the deferred Q010
timestamp-precision issue to reproduce during my own v17 review, so their
green run does not by itself prove the AC15 fix is robust independent of
environment.

I re-ran the exact same repository-wide gate myself, in the same
disposable Docker/Java 21/MySQL 8.4 setup used for the v17 review (fresh
container/network, not reused state):

```
Tests run: 124, Failures: 1, Errors: 0, Skipped: 0
```

The one failure:

```
Q010BootstrapMySqlIntegrationTests.controlledCommandUsesTrustedServiceAuthorizationAndExactReplay
```

— exactly the already-known, already-deferred, environment/clock-dependent
finding recorded in
`docs/lessons/2026-08-31-q-010-bootstrap-replay-timestamp-precision.md`.
Critically, **`Q009MySqlIntegrationTests` passed** in this run too (1
test, 0 failures) — confirming the dynamic-count fix works correctly
regardless of environment, not just on the host where it happened to be
written. AC15 is genuinely fixed, not fixed-by-coincidence-of-host-clock.

Docker resources (container `brokeros-q011-v18-verify-mysql`, network
`brokeros-q011-v18-verify-net`) were removed after this run.

## Assessment

- The v18 package's own claims (diff scope, architecture-impact analysis,
  deferred-item handling) are accurate and match what I independently
  observed.
- The fix is minimal, correctly targeted, and resolves the actual root
  cause (a hard-coded assertion that goes stale with each new migration)
  rather than papering over the symptom.
- No new defect was introduced; no boundary (Q-010/Q-011 files, migrations)
  was crossed.
- The one remaining repository-wide failure (Q010 timestamp precision) is
  correctly out of scope for this closure task and remains tracked
  separately.

## Recommendation

I recommend the Product Owner accept the v18 AC15 closure package. AC15
is now resolved in a way that is independently verified, not just
self-reported, and robust across at least two different host
environments. This does not itself constitute a decision to
stage/commit/push — that remains a separate, explicit Product Owner
decision, as does any decision on the deferred Q010 timestamp-precision
finding.
