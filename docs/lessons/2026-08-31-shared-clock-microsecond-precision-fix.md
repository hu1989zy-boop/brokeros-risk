# Shared Clock Precision Must Match Durable Storage

## Context

Q-010's exact-replay integration test exposed an environment-dependent
timestamp mismatch. The first response used an in-memory `Instant` at the
host clock's available precision, while replay returned the same value after
a MySQL `DATETIME(6)` round trip. On a Linux/Java 21 host with genuine
sub-microsecond clock entropy, the database necessarily discarded the last
three fractional digits. The original and replayed textual results therefore
differed despite representing the same logical operation.

Q-009, Q-010, and Q-011 all inject the single Clock bean defined by
`SecurityModuleConfiguration.securityClock()`, and every persisted timestamp
derived from that Clock is stored at microsecond precision. The Product Owner
authorized a unified correction at that shared boundary.

## Reusable Lesson

A shared application Clock should expose no finer precision than the coarsest
durable precision relied on by its consumers. When all consumers share the
same persistence constraint, normalize the Clock once at its bean or platform
boundary instead of scattering truncation among callers. This keeps first-use
and replay paths in one precision domain, protects future consumers by
default, and avoids drift between modules.

For MySQL `DATETIME(6)`, the matching Java Clock is:

```java
Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000))
```

The one-microsecond tick preserves UTC semantics while removing only the
precision that the database could not retain. It does not change timestamps'
meaning, authorization order, replay behavior, or module ownership.

## Scope and Verification Lesson

Before changing a shared Clock, inventory every injected consumer and every
persisted timestamp precision. Afterward, hash-check all callers, tests,
migrations, and dependencies when the authorized repair is deliberately
limited to the bean. This gives positive evidence that the shared boundary was
fixed without hidden call-site accommodations.

Environment diversity remains important. A green run on a host whose clock
already resolves only to microseconds would also have passed before the fix.
The verification record must distinguish regression coverage from proof in an
environment known to expose the original precision mismatch. Independent
Linux/Docker re-execution is therefore still required before approval.

## Status

The authorized one-bean implementation and local verification are recorded in
the Q-010 v9 shared-clock precision review package. The change is pending
Claude Code's independent review and is not declared complete, approved, or
ready for commit here.
