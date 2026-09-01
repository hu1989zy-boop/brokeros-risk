# Q-013: Action Provenance Foundation

## Status

Drafted 2026-08-31, V1, by Claude Code holding the external Architect
role, per `docs/engineering/AI-Engineering-Execution-Protocol.md`. This is
the Requirement stage of a new task. **Not yet approved.** Self-reviewed
before presentation, but self-review is a disclosed limitation, not a
substitute for Product Owner approval — see §17.

- Requirement ID: `Q-013`
- Architecture phase: Phase 1
- Authoritative domain model: ADR-009 (`Evidence → Decision → Action →
  Risk Case`)
- Depends on: Q-009 (Trusted Actor Authorization), Q-010 (Trading Account
  Reference Authority), Q-011 (Evidence Provenance Foundation), Q-012
  (Decision Provenance Foundation) — all approved, implemented, committed.
  Q-012 in particular is reused, not modified.
- Unblocks (partially): Q-008 (Risk Case) implementation, which is
  authorized to begin only after Decision, Action, and ActionOutcome
  providers are all resolved (Q-008 §26). Q-012 resolved Decision; Q-013
  resolves Action. ActionOutcome remains a separate, later Requirement.

## 1. Background

Q-007/ADR-009 place Action immediately downstream of Decision: "business
response intent produced by a Decision. It is not an execution attempt or
outcome" (Q-007 §3.1). Q-008 (Risk Case) is fully approved through
Implementation Design but explicitly blocked from implementation until
real Decision, Action, and ActionOutcome providers exist (Q-008 §26).
Q-012 resolved Decision. Action is next.

Q-008 does not need to own Action's lifecycle — it explicitly states
"Risk Case does not own the Action lifecycle or the execution record"
(Q-008 §13). It needs to *reference* Actions associated with a case and
display their business intent, without inventing vendor-specific
execution semantics, which Q-008 explicitly defers ("Q-008 defines no
supported vendor operation," §13) and without conflating Action with
Execution, which ADR-009 keeps strictly separate.

This continues the same shape of problem Q-009 through Q-012 each solved:
a narrow, trustworthy, minimally-scoped provider for a downstream
consumer, without prematurely building capability (here: an approval
workflow, and the future Account Control execution adapter) that later,
separately-approved work will define.

## 2. Existing Capability and Gap Analysis

| Need | Existing capability | Gap |
| --- | --- | --- |
| Business-intent record originating from a Decision | None | No `Action` type, storage, or recording use case exists anywhere in the repository |
| The originating Decision to reference | Q-012 Decision Provenance Foundation (`DecisionRef`, narrow provenance-read contract) | None — reusable as-is |
| Actor identity/authorization for recording an Action | Q-009 Trusted Actor Authorization | None — reusable as-is |
| A reference contract Q-008 can trust for case-associated Action display, without inventing vendor operations | None | This is what Q-013 must deliver |
| Approval workflow for an Action (who may approve, under what capability, with what reason) | None; Q-008 mentions "proposed/approved" but defines no semantics for the transition | **Out of scope for Q-013** — see §5.3. A future Requirement must define this before any `APPROVED` state exists |
| Execution / Account Control adapter (MT4/MT5, CRM, etc.) | None | Explicitly deferred by Q-008 §13 ("Future Account Control") and by ADR-009 (Execution is outside the Core Domain) — out of scope for Q-013 |
| ActionOutcome | None | A separate, later Requirement — not addressed here |

## 3. Problem Statement

Q-008 cannot begin implementation until it can reference a real,
trustworthy Action. Without Q-013, any attempt to implement Q-008's
Action-association case coordination would have to either fabricate an
Action or block indefinitely.

Q-013 must provide exactly enough Action capability — recording a
business-intent record originating from one Decision, and a trustworthy
read contract — for Q-008 to consume, without designing an approval
workflow, without inventing vendor-specific operation types, and without
building any execution/adapter capability that belongs to a future,
separately-approved Account Control Requirement.

## 4. Goals

1. Establish `Action` as a real, persisted concept: identity, originating
   Decision, a free-text business-intent description, and authorship —
   attributable and immutable once recorded.
2. Provide a two-tier read contract a future Q-008 can consume — a
   narrow, in-process provenance/confirmation check and a separately
   protected, audited full-detail read — matching the Decision/Evidence
   pattern exactly, confirmed at the Requirement gate for consistency and
   forward-compatibility (§5.3).
3. Require `ActorType.HUMAN` for recording an Action — the only source
   this Requirement authorizes, since no automated proposer exists yet.
4. Reuse Q-009, Q-010 (not directly needed, but its pattern precedent
   applies), Q-011 (not directly needed), and Q-012 exactly as they
   exist; modify none of them.
5. Explicitly do not implement an approval/rejection workflow, any
   vendor-specific operation type, ActionOutcome, or any Execution/Account
   Control adapter capability.

## 5. Scope and Non-Goals

### 5.1 In Scope

- `ActionRef` identity type (`act-<uuidv4>`, lowercase canonical form,
  matching the `ta-`/`ev-`/`dec-` convention).
- Recording an Action: exactly one originating `DecisionRef` (validated
  as "recognized" via Q-012's existing narrow provenance-read contract —
  rejecting only a not-found reference; Decision currently has no status
  to gate on beyond existence), a free-text intent description (bounded
  length, non-blank, matching the `ObservationText`/`ConclusionText`
  validation pattern), the recording actor, and the record time.
- `ActionStatus`: exactly one value, `PROPOSED`, for this Requirement.
  Recording an Action always produces a `PROPOSED` Action. No transition
  out of `PROPOSED` exists yet, though the enum column is designed to
  extend without a breaking schema change once a future Requirement
  defines the transition (§5.3).
- Idempotent recording: a replayed request with the same operation
  identity and unchanged content returns the original outcome without
  creating a duplicate Action, matching Q-011/Q-012's operation-ledger
  pattern.
- A narrow, in-process `confirmProvenance`-style contract (mirroring
  Q-011/Q-012): given an `ActionRef`, returns whether it is recognized,
  and if so, its originating `DecisionRef`, status, recording actor, and
  record time — never the intent description.
- A separate, audited full-detail HTTP read returning an Action's
  complete content including the intent description, requiring
  `action:read`, committing an access-audit record before content is
  returned — matching Q-011/Q-012's full-detail read exactly (§5.3).
- Real MySQL 8.4 migration and test coverage, matching the rigor
  established by Q-009 through Q-012.

### 5.2 Non-Goals (explicitly deferred)

- Any transition out of `PROPOSED` (e.g. `APPROVED`, `REJECTED`,
  `WITHDRAWN`). Q-008 mentions "proposed/approved" but defines no actor,
  capability, or reason semantics for that transition — inventing one now
  would be exactly the kind of undefined-complexity guess this
  Requirement must not make. A future Requirement must define it.
- Action correction or deletion. Like Decision, an Action is immutable
  once recorded — confirmed at the Requirement gate (§5.3): no
  correction/transition use case is implemented by this Requirement, but
  the schema is shaped so a future approval-workflow Requirement can add
  one without a breaking migration.
- Any vendor-specific operation type or taxonomy (restricting trading,
  changing leverage, disabling withdrawal, etc.) — Q-008 §13 explicitly
  lists these only as *examples* of intent, not a schema to implement.
  Action's content is free text, not a structured operation enum.
- ActionOutcome, execution attempts, execution outcomes, or any Account
  Control / MT4/MT5/CRM/bridge/LP adapter.
- Rule Engine or automated/`SERVICE`-sourced Actions.
- Any Q-008 code, wiring, or `RiskCase` type.
- An eligibility-for-case-association service (same reasoning as Q-012
  §5.3 — Action has no mutable per-reference state after recording that a
  consumer needs to distinguish from "exists").

### 5.3 Two design choices — confirmed by the Product Owner, 2026-08-31

The Product Owner's stated governing principle for these two questions:
prioritize the system's extensibility and stability. Both choices below
were decided against that principle, not against raw minimalism.

**Two-tier read split (narrow-vs-full-detail), matching Decision/Evidence
— CONFIRMED.** An earlier draft of this Requirement proposed a single
read contract, reasoning that Q-008 §13 treats Action references as
ordinary case-visible content (unlike Decision, which Q-008 §12 keeps at
arm's length), so hiding intent text behind a second contract seemed
unnecessary. The Product Owner confirmed the two-tier pattern instead:
the cost of the split is negligible (an already-proven shape, reused a
third time), and it buys real optionality — if a future Action intent
type ever needs restricted visibility, or Q-008 needs a cheap
existence-only check separate from a full display read, the contract
shape is already there. Consistency across every module also means a
future implementer never has to remember "Action is the one exception."
§7/§10 below reflect this: a narrow `confirmProvenance`-style contract
(no intent text) plus a separate audited full-detail read.

**`PROPOSED`-only status, immutable Action — CONFIRMED, with the schema
shaped for extension.** Q-008 §13's "proposed/approved" language implies
Action has some lifecycle Risk Case does not own, but defines no
capability, actor authority, or reason semantics for an approval
transition — inventing that workflow now would be an unsupported guess.
The Product Owner confirmed Q-013 should not build it. Separately, the
Product Owner confirmed Action should remain immutable in this
Requirement (no correction/transition use case), while the `ActionStatus`
column itself stays a real enum (single value `PROPOSED` for now, not a
boolean or its absence) specifically so a future approval-workflow
Requirement can relax the `CHECK` constraint and add a transition use
case without a breaking schema migration — extensibility achieved by
schema shape, not by building unrequested behavior now.

## 6. Domain Definitions

- **Action** — business response intent originating from exactly one
  Decision. Recorded once, immutable thereafter (§5.3, confirmed),
  authored by a `HUMAN` actor at a point in time, carrying a free-text
  intent description and a `PROPOSED` status.
- **ActionRef** — canonical identity, `act-<uuidv4>`.
- **ActionSource** — enumerated set of how an Action was produced. This
  Requirement authorizes exactly one value: `MANUAL`.
- **ActionStatus** — enumerated set of an Action's lifecycle state. This
  Requirement authorizes exactly one value: `PROPOSED`.
- **Action intent description** — free-text rationale/content. Not a
  structured vendor-operation field.

## 7. Functional Requirements

- **Q013-FR-001:** The system shall support recording an Action given an
  actor context, exactly one `DecisionRef`, and an intent description.
- **Q013-FR-002:** Recording shall require `ActorType.HUMAN` and the
  `action:record` capability under Q-009's `AuthorizationGuard`.
- **Q013-FR-003:** Recording shall validate the originating `DecisionRef`
  as "recognized" via Q-012's existing narrow provenance-read contract,
  rejecting only a not-found reference.
- **Q013-FR-004:** Recording shall be idempotent by operation identity and
  semantic content fingerprint, matching Q-011/Q-012's replay pattern.
- **Q013-FR-005:** A recorded Action's status shall always be `PROPOSED`;
  no other status value exists in this Requirement, and no transition out
  of `PROPOSED` is implemented.
- **Q013-FR-006:** An Action, once recorded, shall never be corrected or
  deleted through any use case this Requirement authorizes (§5.3,
  confirmed).
- **Q013-FR-007:** The system shall provide a narrow, in-process
  provenance/confirmation read: given an `ActionRef`, return whether it
  is recognized, and if so, its originating `DecisionRef`, status,
  recording actor reference, and record time. This contract shall never
  return the intent description. It requires the `action:read`
  capability but no specific `ActorType` (§5.3, confirmed).
- **Q013-FR-008:** The system shall provide a full-detail read (including
  the intent description) requiring the `action:read` capability, with no
  specific `ActorType` restriction, that commits an access-audit record
  before returning content — mirroring Q-011/Q-012's full-detail read
  exactly, including that a failed audit write must prevent content
  disclosure (§5.3, confirmed).
- **Q013-FR-009:** No vendor-specific operation type, taxonomy, or
  execution semantics shall be introduced.

## 8. Security Requirements

- All use cases call Q-009's `AuthorizationGuard.requireAllowed` before
  any Action lookup or mutation. Default-deny; no implicit grant.
- Capabilities: `action:record`, `action:read`. No other capability is
  introduced.
- Recording requires `ActorType.HUMAN`; the read use case permits any
  authorized `ActorType`, including `SERVICE`.
- No intent description, actor identity, or `DecisionRef` may appear in
  logs or metric tags, matching Q-011/Q-012's observability constraints.

## 9. Data Integrity and Provenance Requirements

- Every Action is permanently attributable to its recording actor, time,
  and originating Decision; none of these may be altered after recording
  (§5.3, confirmed).
- Cross-module reference to `DecisionRef`: a validated `CHAR` column, not
  a SQL foreign key to Q-012's table — matching the established
  cross-module pattern (Architecture will confirm the exact shape, per
  ADR-014's precedent).
- Full-detail access must be audited before content is returned, in a
  dedicated, non-database-read-only transaction — mirroring Q-011/Q-012.
- Idempotency: an exact replay never creates a second Action row; a
  changed replay under the same operation id is rejected, never silently
  applied.

## 10. Acceptance Criteria

1. `ActionRef`/operation identity use the canonical lowercase UUIDv4
   forms established by Q-010/Q-011/Q-012.
2. Recording requires `HUMAN` and `action:record`; reading requires only
   `action:read`, with no `ActorType` restriction.
3. Recording accepts any recognized `DecisionRef` and rejects only a
   not-found reference.
4. A recorded Action always has status `PROPOSED`; no other status value
   exists and no transition use case exists.
5. An Action is immutable after recording — no correction or delete use
   case exists anywhere in the implementation (§5.3, confirmed).
6. The narrow provenance/confirmation read never returns the intent
   description, structurally (not by convention).
7. The full-detail read commits an access-audit record before returning
   the intent description; a failed audit write returns no content.
8. Idempotent replay (same operation id + unchanged content) returns the
   original outcome without a new database row; a changed replay under
   the same operation id is rejected.
9. No Q-008, ActionOutcome, Execution, Account Control adapter, Alert,
   Rule Hit, or Rule Engine code exists anywhere in the change.
10. No vendor-specific operation type or execution semantic exists
    anywhere in the change.
11. No existing Q-009/Q-010/Q-011/Q-012 file is modified.
12. All mandatory tests, including real MySQL 8.4 integration/persistence/
    concurrency/security tests, pass; no mandatory test is skipped.

## 11. Technical Constraints

- Java 21 / Spring Boot modular monolith, `com.brokeros.risk.action`
  package, matching the existing module layout convention.
- Additive Flyway migration only (next available version number); no
  existing migration may be modified.
- Reuse the single shared `Clock` bean
  (`SecurityModuleConfiguration.securityClock()`).
- MySQL-compatible constraints only; apply the `IS NOT NULL`-guarded
  CHECK pattern documented in `docs/skills/development-standards.md` for
  any nullable-column constraint, and avoid the hard-coded-migration
  -count test pattern documented in `docs/lessons/2026-08-31-q011-migration-count-test-fix.md`.

## 12. Dependencies

- Q-009 (`ActorContext`, `Capability`, `AuthorizationGuard`) — reused
  unchanged.
- Q-012 (`DecisionRef`, its narrow provenance-read contract) — reused
  unchanged. Q-013 is the second consumer of Q-012's narrow contract
  (the first being Q-008, which still does not exist).
- Does not depend on and does not implement any part of Q-008, Q-010, or
  Q-011 directly (though it follows their established schema/pattern
  precedent).

## 13. Verification Plan

- Real disposable MySQL 8.4 migration, persistence, concurrency, and
  security integration tests, matching Q-011/Q-012's structure.
- Architecture tests proving no vendor-operation vocabulary and no
  Execution/Account Control code exists in this module.
- Full repository-wide real-MySQL gate (`Q009`/`Q010`/`Q011`/`Q012`/`Q013`
  all enabled) must pass with zero unexplained failures — including a
  proactive check for the hard-coded-migration-count pattern in any new
  test this Requirement's implementation adds, per the lesson cited in
  §11.
- Static verification (`scripts/verify-static.sh`) extended with a
  Q-013-specific section mirroring the existing pattern.

## 14. Risks and Architecture Inputs

- Risk: a future approval-workflow Requirement may need to reopen this
  Requirement to add `ActionStatus.APPROVED`/`REJECTED` and define who
  may transition an Action. Anticipated, not prevented — §5.3 confirms
  the `ActionStatus` column is deliberately shaped to absorb this without
  a breaking migration.
- Architecture input needed: exact `action_ref` column length (`act-` is
  4 characters, same as `dec-`, so `CHAR(40)` by the same arithmetic Q-012
  used — Architecture should verify this rather than copy it blindly, the
  same discipline Q-012's own Architecture stage applied to itself).

## 15. Deliverables

- Approved `docs/requirements/Q-013-Action-Provenance-Foundation.md`.
- Architecture document, ADR, and Implementation Design, following the
  same gate sequence as Q-009 through Q-012.
- `com.brokeros.risk.action` module implementation, additive migration,
  full test suite.
- Lessons Learned entries for any reusable pattern.
- Non-overwriting, timestamped review packages under `review/q-013/` at
  each gate.

## 16. Review Checklist

- [x] Requirement self-reviewed by Claude Code (external Architect role).
- [ ] Product Owner Gate Decision recorded (§17).
- [x] §5.3's two design choices confirmed by the Product Owner
      (two-tier read matching Decision/Evidence; immutable/`PROPOSED`-only
      with an extensible enum column) — 2026-08-31.
- [ ] No Q-008/Q-009/Q-010/Q-011/Q-012 file referenced for modification.
- [ ] No vendor-specific operation vocabulary introduced anywhere.

## 17. Current Gate

Q-013 Requirement status: **APPROVED — V1 — 2026-08-31 — Product Owner.**
Gate Decision: **PASS**, including §5.3's two design choices (two-tier
read matching Decision/Evidence; immutable/`PROPOSED`-only with an
extensible `ActionStatus` column), confirmed against the stated governing
principle of prioritizing extensibility and stability.

Q-013 Implementation Allowed: **NO — Requirement approval is not
Architecture, ADR, Design, or Implementation authorization.**

Q-013 Architecture status: **APPROVED — V1 — 2026-09-01 — Product
Owner.** See
`docs/architecture/q-013-action-provenance-foundation-architecture.md`
§24.

Q-013 ADR: **AUTHORIZED to be drafted — 2026-09-01 — Product Owner**
(ADR-015).

ADR-015 status: **ACCEPTED — 2026-09-01 — Product Owner.** See
`docs/adr/ADR-015-action-provenance-foundation.md`.

Q-013 Implementation Design status: **DRAFTED, self-reviewed — V1 —
2026-09-01 — Claude Code. Not yet approved.** See
`docs/architecture/q-013-action-provenance-foundation-implementation-design.md`.

Q-013 Implementation Design: **APPROVED — V1 — 2026-09-01 — Product
Owner.**

Q-013 Implementation: **AUTHORIZED — 2026-09-01 — Product Owner**, against
Requirement V1 / Architecture V1 / ADR-015 (Accepted) / Implementation
Design V1.

Q-013 Implementation Allowed: **YES.**

Q-013 Implementation: **SUBMITTED — 2026-08-31/2026-09-01 — Codex.**
Package `review/q-013/review-q-013-v5-implementation-20260831-235338/`.
Codex's own Gate Decision: BLOCKED — Q-013's 39 mandatory tests pass, but
the mandatory all-module (Q-009…Q-013) real-MySQL gate has one failure:
`Q012MySqlMigrationTests.migrationUpgradesV4CreatesExactlyFourTablesWithoutSeedsAndValidatesOnRestart`,
an unchanged Q-012 test whose hard-coded post-V4 migration count went
stale the moment V6 was added. Codex correctly did not modify the Q-012
file, honoring the Q-013 Prompt's hard boundary.

Q-013 Implementation Independent Review: **BLOCKED — 2026-09-01 — Claude
Code.** Package
`review/q-013/review-q-013-v6-claude-code-independent-review-20260901-111500/`.
Verified by direct code inspection (§11.1 execution order, §8.4
constraint-to-test mapping, structural narrow-contract guarantee, 2-route
HTTP surface, single-`DecisionRef` not a set) and by independently
re-executing the full suite in Docker/Java 21/MySQL 8.4: 204 tests, 1
failure — the same `Q012MySqlMigrationTests` failure Codex reported,
confirmed genuine and unrelated to Q-013's own correctness. **No defect
found in Q-013's own implementation.** This is the third occurrence of
the hard-coded-migration-count bug class (after Q-009's AC15 and Q-011's
migration test) and was explicitly forecast as a tracked follow-up during
Q-012's own review. Recommendation: accept Q-013's implementation;
separately authorize the same dynamic-migration-count fix on
`Q012MySqlMigrationTests.java`; and consider a one-time repository-wide
sweep of the remaining hard-coded-count test pattern.

Q-013 all-module gate blocker: **RESOLVED — 2026-09-01 — Claude Code**,
under the Technical Decision Authority delegation
(`docs/engineering/Architecture-and-Design-Decision-Principles.md`
§16.5-A: pure test-maintenance, zero-business-impact cross-module test
fixes may be applied directly). The stale `Q012MySqlMigrationTests.java`
post-V4 count assertion was fixed dynamically (one test file, 2/1 diff),
a repository-wide sweep confirmed no other stale instance remains (the
`target("N")` baseline assertions are correct-by-construction and must
stay hard-coded), and the full Q-009…Q-013 real-MySQL gate was
independently re-run green: **204 tests, 0 failures, 0 errors, 0
skipped.** See
`review/q-013/review-q-013-v6-claude-code-independent-review-20260901-111500/Q012TestFixAddendum.md`.

**No defect remains in or around Q-013.** All 12 Q-013 Acceptance
Criteria now effectively PASS (AC 12's blocker cleared).

Next gate: Product Owner decision on (a) accepting Q-013's implementation
and (b) authorizing commit. Per §16.5-A, the test fix is in the working
tree but is not committed until the Product Owner triggers a commit gate.
