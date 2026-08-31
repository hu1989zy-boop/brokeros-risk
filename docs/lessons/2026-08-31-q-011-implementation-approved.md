# Q-011 Implementation Approved

## What Was Decided

The Product Owner approved the Q-011 Evidence Provenance Foundation
implementation on 2026-08-31, based on two independent inputs: Codex's own
implementation review package
(`review/q-011/review-q-011-v16-implementation-20260830-161236/`) and
Claude Code's independent review, holding the external-Architect role
(`review/q-011/review-q-011-v17-claude-code-independent-review-20260830-163904/`).

The independent review did not stop at reading Codex's report. It traced
the actual recording/correction service code against Implementation
Design V5's execution-order and constraint tables line by line, and — once
the Product Owner pointed out that Docker could run Java 21 locally — went
further and independently *executed* the full Maven test suite (Java 21,
MySQL 8.4.11, disposable Docker containers) rather than relying on
re-reading Codex's reported numbers. Result: 124 tests, 2 failures, 0
errors, both pre-existing and outside Q-011's change boundary (the known
AC15 `Q009MySqlIntegrationTests` staleness, plus a newly found
`Q010BootstrapMySqlIntegrationTests` timestamp-precision issue — see
[[2026-08-31-q-010-bootstrap-replay-timestamp-precision]]).

Two follow-ups were authorized/recorded alongside the approval, not folded
into it:

1. A narrowly-scoped AC15 fix (`Q009MySqlIntegrationTests.java` only,
   test-assertion logic, no behavior change) — authorized for Codex to
   execute as part of Q-011 closure.
2. The Q010 timestamp-precision finding — deferred, recorded as a lesson,
   not authorized for a code fix at this gate.

## Reusable Lesson

An independent review that only reads code is weaker than one that also
re-executes the verification, when re-execution is actually available. The
first pass of this review (code/DDL inspection only) was honest about that
gap rather than papering over it, and disclosing the gap is what let the
Product Owner supply the missing piece (Docker/Java 21) instead of the
review silently settling for less. Re-executing the exact same commands in
a different environment (Linux/Docker vs. Codex's macOS host) also
surfaced a real defect — the Q010 timestamp-precision issue — that would
have stayed invisible if "the numbers already match" had been treated as
sufficient. Different environments exercise different edge conditions
(here, clock resolution); an independent review that runs in a different
environment than the original implementer is strictly more valuable than
one that reruns the identical setup.

A second, procedural lesson: when a re-run first fails for a reason
unrelated to the code under test (here, mounting only the `backend/`
subdirectory broke the architecture tests' `repositoryRoot()` marker
lookup), the fix is to diagnose and correct the verification environment,
not to report the failure as a code defect. Read the actual assertion
before concluding a test failure means the implementation is wrong.

## Not Yet Resolved

Git staging/commit/push has not been authorized and was not performed.
The AC15 fix has not yet been executed by Codex. The Q010
timestamp-precision issue has no fix scheduled — it is deferred pending a
separate, later decision.
