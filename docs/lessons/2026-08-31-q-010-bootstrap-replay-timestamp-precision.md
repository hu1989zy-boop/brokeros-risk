# Q010 Bootstrap Replay Timestamp-Precision Mismatch (Deferred)

## What Was Found

While independently re-executing the repository's full real-MySQL test
gate for the unrelated Q-011 review (see
[[2026-08-31-q-011-implementation-approved]]), running the exact same
Maven commands Codex's own verification used — but in a Linux/Docker
container (Java 21, MySQL 8.4.11) instead of Codex's macOS host — surfaced
a test failure Codex's own verification never saw:

```
Q010BootstrapMySqlIntegrationTests.controlledCommandUsesTrustedServiceAuthorizationAndExactReplay
expected: "... occurredAt=2026-08-30T10:18:49.442501179Z\n"
 but was: "... occurredAt=2026-08-30T10:18:49.442501Z\n"
```

This test invokes `TradingAccountAuthorityBootstrapCommand` twice with an
identical manifest (an idempotent-replay assertion) and requires the two
textual outputs to be byte-for-byte identical. The `occurredAt` field
differs only in its last three digits. Root cause: the first invocation's
output is built from an in-memory `Instant.now()`, which on this
environment's JVM/clock genuinely carries nanosecond-level precision; the
replay path instead reads the timestamp back from MySQL, whose maximum
fractional-seconds column precision is 6 digits (microseconds), silently
truncating the sub-microsecond digits. The two code paths pull the same
logical timestamp from two different precision domains.

Reproduced twice (different random UUIDs, same truncation pattern both
times) — deterministic given this environment's clock, not a one-off
flake. It did not appear on Codex's own macOS/JBR21 host, apparently
because that JVM's clock did not expose genuine sub-microsecond entropy
for `Instant.now()` there.

## Reusable Lesson

An idempotent-replay test that asserts byte-for-byte output equality is
only as strong as the assumption that every value in that output round-
trips through storage without precision loss. Timestamps are a common
place this assumption silently breaks: `Instant.now()`'s actual resolution
is JVM/OS-dependent, while most SQL timestamp columns cap out at
microseconds. The fix belongs in the application code, not the test: the
first-call output should be built from the same precision the value will
be persisted and reloaded at (e.g., truncate with
`Instant.now().truncatedTo(ChronoUnit.MICROS)` before both persisting and
formatting), so replay and original always agree regardless of the host
clock's actual resolution. Patching the test's expected value instead
would hide the inconsistency rather than fix it.

Also reusable: running the same verification suite in a *different*
environment than the original implementer used is a legitimate, additional
way to find latent environment-dependent bugs — not just a redundant
re-check. See [[2026-08-31-q-011-implementation-approved]] for the
broader point.

## Status

**Fix implemented and independently confirmed on 2026-08-31.** The single
shared `SecurityModuleConfiguration.securityClock()` bean now returns
`Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000))`, so Q-009, Q-010,
and Q-011 obtain timestamps at the microsecond precision supported by the
application's `DATETIME(6)` columns. No Clock consumer, test, migration, or
dependency was changed. Recorded in
`review/q-010/review-q-010-v9-shared-clock-precision-fix-20260831-141025/`.

The full macOS/JBR21 plus MySQL 8.4.11 gate passed all 124 tests with zero
failures, errors, or skips, but that host never exposed the old code's
sub-microsecond mismatch, so it alone would not have proven the repair.
Claude Code independently re-ran the full suite in the Linux/Docker Java 21
environment that had twice reproduced the original failure deterministically
(v17's `IndependentTestExecution.md` and the pre-fix v18 re-verification):
124 tests, 0 failures, 0 errors, 0 skipped —
`Q010BootstrapMySqlIntegrationTests` now passes in the one environment where
it was previously guaranteed to fail. See
`review/q-010/review-q-010-v9-shared-clock-precision-fix-20260831-141025/ClaudeCodeIndependentReview.md`.

**Accepted by the Product Owner on 2026-08-31.** Recorded in
`docs/requirements/Q-010-Trading-Account-Reference-Authority-Foundation.md`
§15. Staging, commit, and push remain separate, explicit Product Owner
decisions not made by this acceptance.
