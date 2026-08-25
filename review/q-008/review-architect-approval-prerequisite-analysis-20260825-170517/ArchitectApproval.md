# Q-008 Implementation Design V4 Architect Approval

## Decision

**Q-008 Implementation Design V4 — APPROVED**

- Decision origin: explicit external Architect instruction
- Decision recorded: 2026-08-25
- Approved Design artifact:
  `docs/architecture/q-008-risk-case-foundation-implementation-design.md`
- Approved Design SHA-256:
  `44447933a0ec97d8236a3ba83bc9db6e08fd008c15250ac8574e8b7af1520a8a`
- Submitted V4 ZIP SHA-256:
  `cb9580734fff8ff317e8cf62a6a4e831e66da71e8fc84ded86ada7dedb92b8b3`

The approval is recorded from the Architect's explicit decision. It is not
Codex self-approval.

## Approved Decisions

The Architect approved the V4 design without modification, including:

1. `RC-<canonical-lowercase-UUIDv4>` CaseNumber with separate `BIGINT` primary
   key and database unique constraint;
2. immutable historical Resolution Cycles with normalized Decision, Evidence,
   and Action references/snapshots;
3. RiskCase as the bounded Aggregate Root for case-owned state and invariants;
4. root optimistic concurrency/version control;
5. case-local deterministic ordering using successful case version;
6. typed Evidence, Decision, Action, ActionOutcome, and subject reference
   boundaries without upstream ownership transfer;
7. Action as business intent and never external Execution;
8. Spring JDBC/MySQL persistence design and normalized append-only history;
9. same-application-database transaction consistency for case mutation and
   independent Audit Record;
10. explicit named lifecycle operations and strict history-preserving reopen;
11. named REST/API boundary, idempotency, ResultCode, validation, and DTO
    separation design;
12. compatibility with ADR-009 and ADR-010.

The approval also preserves all explicit deferrals and exclusions in V4.

## Version Decision

**No Implementation Design V5 is required.**

The precondition analysis in this Review does not revise the approved Design.
The existing V4 Design and V4 Review/ZIP remain immutable evidence. Their
historical status statements show the state when they were submitted; this
separate Architect Approval record is the later authoritative Design Gate
decision.

## Implementation Gate

- Requirement: APPROVED
- Architecture: APPROVED
- ADR-010: ACCEPTED
- Implementation Design: V4 — APPROVED
- Implementation: NOT STARTED
- Implementation Authorization: BLOCKED BY PREREQUISITES
- Implementation Allowed: **NO**

Implementation remains blocked only by unresolved implementation prerequisites:

1. a trusted, production ActorContext/authentication/authorization boundary;
2. authoritative Trading Account, Evidence, Decision, Action, and
   ActionOutcome reference providers and runtime wiring; and
3. a later explicit Architect authorization after those prerequisites are
   approved, implemented, and verified.
