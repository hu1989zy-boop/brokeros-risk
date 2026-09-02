# Q-008 Implementation Lessons Learned

## Scope

Q-008 added the Risk Case Foundation as a downstream Phase 1 aggregate. It
owns case lifecycle, individual assignment, operational priority, append-only
case history, resolution cycles, notes, and typed references to Q-010 through
Q-014, while leaving Evidence, Decision, Action, ActionOutcome, and execution
under their existing owners.

## What worked

- Named aggregate operations made the lifecycle table directly testable and
  prevented a generic status setter from bypassing transition invariants.
- Reserving each new case version with one optimistic root compare-and-set,
  then appending business history and the independent Audit Record inside the
  same Spring transaction, gave deterministic command ordering without locks
  or a distributed transaction.
- Exact Q-008 table and ResultCode inventories avoided the shared-prefix
  ownership defect previously exposed between Q-013 and Q-014.
- The V5 provider bindings stayed thin: each adapter delegates to the shipped
  Q-010 through Q-014 query service and maps its bounded recognition result;
  Q-008 never reads an upstream table or accepts an unchecked reference.
- A real MySQL full-path test covered two resolution cycles and retained the
  prior resolution, closure, reopen, association, note, transition, and audit
  facts instead of relying only on unit tests.

## Problems encountered

- The filesystem sandbox denied loopback JDBC access. Mandatory MySQL tests
  were rerun against the disposable MySQL 8.4 container with host permission.
- MySQL binary logging prevented the disposable application user from creating
  test-only failure-injection triggers. Enabling
  `log_bin_trust_function_creators` only inside that disposable container
  allowed rollback tests without elevating the application user or changing
  product configuration.
- The repository infrastructure verifier still encodes the Q-004 V1-V3 schema
  and fixed host ports. It built the backend image successfully but could not
  start Redis because host port 6379 was already occupied; even with a free
  port its exact seven-table assertion is incompatible with the current V8
  repository. This was reported honestly rather than weakening Q-008 or
  editing an out-of-scope verifier.
- Initial Maven invocations were made from the repository root, which has no
  POM. The commands were corrected to run from `backend/`, and the operator
  errors remain part of the verification record.

## Reusable rule

For a versioned aggregate that writes append-only business history and an
independently owned audit fact, a passing happy path is insufficient atomicity
evidence. Inject a failure separately into the history insert and the audit
insert on the supported real database, then assert that the root CAS, all
history rows, and all audit rows rolled back. Also assert that a concurrent CAS
loser leaves no command-owned rows. This rule was added to
`docs/skills/development-standards.md`.

## Verification implication

Repository-wide test success and an infrastructure-script result are separate
facts. Q-008's real-MySQL suites and the full Q-009 through Q-014 regression
gate can pass while an older environment verifier remains unusable because its
schema and port assumptions are stale. Review evidence must report both facts
without converting the stale tool failure into a product PASS.
