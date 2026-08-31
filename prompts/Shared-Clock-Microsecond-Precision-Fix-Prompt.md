# Shared Clock Microsecond-Precision Fix Prompt

**CLEARED FOR USE.** The Product Owner authorized this fix in chat on
2026-08-31. This supersedes the earlier, narrower
`Q-010-Timestamp-Precision-Fix-Prompt.md` (deleted — do not look for it),
which proposed patching only `AuthorizedMutationFactory`. The Product
Owner asked for a **unified** fix that matches the database's precision
everywhere, not a module-by-module patch, so the fix now targets the
single shared `Clock` bean instead.

## Background

`Q010BootstrapMySqlIntegrationTests.controlledCommandUsesTrustedServiceAuthorizationAndExactReplay`
fails on hosts whose JVM clock exposes genuine sub-microsecond precision
(reproduced deterministically in a Linux/Docker Java 21 environment;
did not reproduce on a macOS/JBR21 host). Root cause, already diagnosed:
`AuthorizedMutationFactory.authorize()`
(`backend/src/main/java/com/brokeros/risk/tradingaccount/application/AuthorizedMutationFactory.java`,
line 36) mints an `Instant` via `clock.instant()` with whatever precision
the underlying clock provides. That instant is returned directly to the
caller on the first invocation, and separately persisted into the
`occurred_at DATETIME(6)` column and reloaded on replay. MySQL's
`DATETIME(6)` caps at microsecond precision, so the DB round-trip silently
truncates anything finer, and the two paths disagree.

## Why the fix is now at the Clock bean, not the call site

There is exactly **one** `Clock` bean in the entire application:
`SecurityModuleConfiguration.securityClock()`
(`backend/src/main/java/com/brokeros/risk/security/infrastructure/configuration/SecurityModuleConfiguration.java`,
lines 20-23), currently `return Clock.systemUTC();`. Every module —
`security` (Q-009), `tradingaccount` (Q-010), and `evidence` (Q-011) —
injects this exact same bean (grep confirms 8 call sites across
`ActorProvisioningService`, `ServiceActorContextFactory`,
`JdbcAuthorizationAdapter`, `JwtVerifiedPrincipalAdapter`,
`AuthorizedMutationFactory`, `EvidenceRecordingService`,
`EvidenceCorrectionService`, `EvidenceDetailReadService`, all parameter-
named `securityClock`). Every timestamp column across all four migrations
(V1–V4) that stores a value derived from this clock is consistently
`DATETIME(6)` (confirmed by grep: `security_actor`, `security_principal_mapping`,
`security_actor_capability`, `trading_account_authority_operation`,
`trading_account_authority_history`, `evidence_record`,
`evidence_operation`, `evidence_operation_history`, `evidence_access_log`
all use `DATETIME(6)`).

Patching each of the 8 call sites individually would be the same fix
repeated 8 times, exactly the kind of drift this project's governance has
repeatedly found costly (see `docs/lessons/` on "fix the rule everywhere
it is stated, not just where it was cited"). Fixing the single shared bean
fixes every current consumer at once, and any future consumer
automatically, with zero chance of a ninth call site reintroducing the
same bug.

## Required fix — one file, no call sites touched

In `SecurityModuleConfiguration.java`, change:

```java
@Bean
Clock securityClock() {
    return Clock.systemUTC();
}
```

to:

```java
@Bean
Clock securityClock() {
    return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000));
}
```

(`Duration.ofNanos(1000)` = 1 microsecond; `Clock.tick(Clock, Duration)` —
available since Java 8 — returns a clock whose `instant()` truncates the
current time down to whole multiples of the given duration, i.e. to
microsecond precision, matching `DATETIME(6)` exactly.) Add the
`java.time.Duration` import. Do not use a convenience method that does not
exist on `Clock` (there is no `Clock.tickMicros(...)` in the JDK) —
`Clock.tick(baseClock, tickDuration)` is the correct, always-available
API.

## Hard boundaries

- Touch only `SecurityModuleConfiguration.java` — a two-line change plus
  one import. Do not rename the `securityClock` bean method, its
  parameter name in any consumer, or touch any consumer file
  (`ActorProvisioningService`, `ServiceActorContextFactory`,
  `JdbcAuthorizationAdapter`, `JwtVerifiedPrincipalAdapter`,
  `AuthorizedMutationFactory`, `EvidenceRecordingService`,
  `EvidenceCorrectionService`, `EvidenceDetailReadService`) — they need no
  changes.
- Do not modify `Q010BootstrapMySqlIntegrationTests.java` or any other
  test to accommodate this — the existing byte-for-byte replay assertion
  should simply start passing.
- Do not modify any migration, any DTO, any ResultCode, or add a
  dependency.
- Do not introduce a second `Clock` bean or a test-specific clock
  override; production code path only.
- Do not stage, commit, or push.

## Verification required

Re-run the full repository-wide real-MySQL gate
(`Q009_MYSQL_TEST_URL`/`Q010_MYSQL_TEST_URL`/`Q011_MYSQL_TEST_URL` all
set, `mvn test`), `scripts/verify-static.sh`, and `git diff --check`.
Confirm all 124 tests pass, including `Q010BootstrapMySqlIntegrationTests`
and every Q-009/Q-011 test that depends on the shared clock (none should
behave differently — microsecond truncation only removes precision no
persisted column could keep anyway).

State plainly in `Verification.md`, as before: if your host's JVM clock
does not expose sub-microsecond entropy for `Instant.now()`, the *old*
code would also have passed there, so a green result on your host alone
does not fully prove the fix. Claude Code will independently re-verify in
the Linux/Docker environment where the bug is known to reproduce
deterministically.

## Required output

Create ONE new, non-overwriting, timestamped review package. Because the
changed file lives in the `security` module (Q-009) even though the
originally observed symptom was in Q-010's test, and the fix also affects
Q-011's clock-derived timestamps, place it at
`review/q-010/review-q-010-v9-shared-clock-precision-fix-<YYYYMMDD-HHMMSS>/`
(check `review/q-010/` first for the actual next unused version number —
v8 is the latest as of this writing) and explicitly note in `Summary.md`
that the changed file is technically in the `security` module's
configuration but is being tracked under Q-010 because that is where the
bug was originally observed; also note that the fix benefits Q-011's
`evidence` module identically, and that Q-009's own timestamps (actor
provisioning, principal mapping, capability grants) are equally protected
even though no Q-009 test previously exposed this class of bug. Contents
at minimum: `Summary.md`, `Verification.md`, `GitDiffStat.txt`,
`GitStatus.txt`, `OutstandingItems.md`. Add one
`docs/lessons/<date>-shared-clock-microsecond-precision-fix.md` entry
capturing the reusable lesson (a single shared `Clock` bean should
truncate to the coarsest precision any consumer's persistence layer
actually supports, fixed once at the bean, not scattered across callers),
and update
`docs/lessons/2026-08-31-q-010-bootstrap-replay-timestamp-precision.md`'s
own "Status" section to note the fix landed and where (do not rewrite the
rest of that lesson).

This is not your own sign-off. Do not mark this fix, Q-009, Q-010, or
Q-011 "complete" or "approved" anywhere. Claude Code performs an
independent review (including re-executing tests in the environment where
this bug reproduces) before the Product Owner considers any commit.

Stop after producing the review package. Do not begin any other task.
