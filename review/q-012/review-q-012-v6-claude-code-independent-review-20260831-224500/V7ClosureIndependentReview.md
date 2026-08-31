# Independent Review of Codex's V7 AC11 Closure Package

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§8–§12. Subject:
`review/q-012/review-q-012-v7-full-gate-closure-20260831-225443.zip`.

## Code-diff verification (direct inspection, not just reading the package)

Confirmed via `git diff` myself:

```java
-        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
+        int pendingMigrationCount = flyway.info().pending().length;
+        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
```

Exactly 2 insertions / 1 deletion in `Q011MySqlMigrationTests.java`,
matching the authorized scope precisely. No other assertion in that file,
no other Q-011 file, and no Q-009/Q-010/Q-012 file was touched.

## Independently executed re-verification (the decisive check)

Re-ran the full repository-wide gate myself in a disposable Docker/Java
21/MySQL 8.4 Linux environment (fresh container/network):

```
Tests run: 165, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Matches Codex's own reported 165/0/0/0 exactly, now confirmed in a second
independent environment. This bug class (a deterministic integer
migration-count mismatch) is not environment/clock-dependent like the
earlier timestamp-precision issue was, so identical reproduction across
both hosts is the expected and correct outcome — it confirms the fix is
genuinely correct, not host-specific luck.

Docker resources (`brokeros-q012v7-verify-mysql` container,
`brokeros-q012v7-verify-net` network) were removed after this run.

## A valuable proactive finding from Codex, independently confirmed

Codex's own `Verification.md` reports a read-only recurrence scan that
found `Q012MySqlMigrationTests.java` (Q-012's own migration test) has the
**identical latent pattern**: a `target("4")` baseline followed by a
hard-coded `isEqualTo(1)`. I independently confirmed this by reading the
file directly (line ~45). It currently passes only because V5 is the sole
migration after V4 — it will fail the same way the moment any future
migration (a hypothetical Q-013, etc.) is added. Codex correctly did not
fix this, since it was out of scope for the narrowly authorized task, and
correctly reported it rather than staying silent.

**Recommendation:** track this as a minor, non-urgent follow-up — either
fix it proactively now (cheap, same one-line pattern) or leave it
documented and catch it the next time a migration is actually added. Not
a blocker for Q-012's closure either way, since it is not currently
failing.

## Q-012 Acceptance Criteria — final status

All 11 Acceptance Criteria (Requirement §10) now PASS:
- AC 1–10: confirmed compliant in the v6 independent review (code
  inspection + 165-test-suite execution before this fix).
- AC 11: confirmed resolved here — the full Q-009/Q-010/Q-011/Q-012
  real-MySQL gate is green, independently verified in two separate
  environments (Codex's macOS host, and this session's Linux/Docker
  environment).

## Recommendation

Accept Q-012's implementation. Its own code was already found defect
-free in the v6 review; the one blocking issue (AC11, a pre-existing,
unrelated, now-fixed Q-011 test staleness) is resolved and independently
confirmed. The `Q012MySqlMigrationTests` latent-pattern finding above is
worth a decision (fix now or track for later) but does not block
acceptance.

## Gate Decision

**PASS.** This does not itself constitute Product Owner acceptance,
approval, or authorization to stage/commit/push — those remain separate,
explicit Product Owner decisions.
