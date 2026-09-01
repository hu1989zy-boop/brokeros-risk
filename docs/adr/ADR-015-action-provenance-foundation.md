# ADR-015: Action Provenance Foundation

- Status: **Accepted — 2026-09-01 — Product Owner**
- Date: 2026-09-01
- Approval origin: drafting authorized by explicit Product Owner
  decision, 2026-09-01 (Architecture V1 approval, "同意"). Prepared by
  Claude Code, holding the external Architect review role by explicit
  Product Owner direction. Self-review artifact, disclosed for the same
  reason every prior ADR in this repository discloses it.
- Requirement: Q-013 — Action Provenance Foundation V1, APPROVED —
  2026-08-31 — Product Owner
- Architecture: `docs/architecture/q-013-action-provenance-foundation-architecture.md`
  (see its own Document Status for the current version — not hard-coded
  here, per the lesson ADR-013's amendment history and ADR-014 both
  already recorded)
- Depends on: ADR-009 (core domain model), ADR-011 (Q-009 trust
  semantics), ADR-012 (Q-010 Trading Account authority), ADR-013 (Q-011
  Evidence provenance), ADR-014 (Q-012 Decision provenance, and the
  cross-module no-hard-FK pattern this ADR reuses)
- Supersedes: None

## Context

Q-008 (Risk Case) cannot begin implementation until Decision, Action, and
ActionOutcome providers all exist (Q-008 §26). Q-012/ADR-014 resolved
Decision. Action is next; ActionOutcome remains a separate, later
Requirement.

Q-007/ADR-009 places Action immediately downstream of Decision: business
response intent, explicitly not an execution attempt or outcome, and
explicitly separate from Execution (outside the Core Domain). Q-008 §13
needs to reference and display Action content as ordinary case work
("Case coordination may record proposed/approved Action references"),
while explicitly not owning the Action lifecycle, not defining any
vendor-specific operation, and not building any execution/Account Control
adapter — all of that is future, separately-approved work.

Q-013 Requirement V1 (approved 2026-08-31, with two design choices
confirmed directly by the Product Owner on 2026-08-31 against a stated
principle of prioritizing system extensibility and stability) resolves
the business boundary: `ActionSource` limited to `MANUAL`, recording
limited to `HUMAN` actors, exactly one originating `DecisionRef` required,
`ActionStatus` limited to `PROPOSED` with no transition implemented (but
the column shaped to extend without a breaking migration), Action
immutable once recorded, and — reversing this ADR's own drafter's
original single-read proposal — a two-tier narrow/full-detail read
matching Decision/Evidence's pattern. Architecture V1 (approved
2026-09-01) resolved identity/content representation, the originating
-Decision validation reuse, durable storage shape (three tables, no join
table, unlike Decision), and the consumer boundary. This ADR records the
resulting architectural decision as a durable, citable artifact.

## Decision

### Bounded capability and identity ownership

Q-013 introduces `com.brokeros.risk.action`, a module inside the existing
Phase 1 modular monolith. It owns: `ActionRef` identity
(`act-<canonical-lowercase-UUIDv4>`); bounded, immutable Action content
(exactly one originating `DecisionRef`, a free-text intent description,
recording actor, record time, and a `PROPOSED`-only status); the
recording use case and its idempotency ledger; a narrow, in-process
provenance/confirmation read contract; and an audited full-detail read.

It owns no Risk Case, ActionOutcome, Execution, or Account Control
behavior. It does not implement any approval-workflow transition — that
is explicitly deferred to a future Requirement (see "Alternatives
Considered").

### Exactly one originating Decision, no join table

Unlike Decision's one-to-many relationship to Evidence (requiring a join
table), Action's relationship to Decision is one-to-one: Requirement
`Q013-FR-001` requires exactly one originating `DecisionRef`. This ADR
records that `action_record.decision_ref` is therefore a direct column,
not a separate relationship table — three application tables total for
Q-013, one fewer than Q-012's four, tracing to an actual difference in
requirement cardinality, not an inconsistency with precedent.

### PROPOSED-only status, immutable Action, schema shaped for extension

Q-008 §13's "proposed/approved" language implies Action has some
lifecycle Risk Case does not own, but defines no capability, actor
authority, or reason semantics for an approval transition. Building that
workflow now would be an unsupported guess about undefined semantics —
the same category of premature-invention this repository has repeatedly
avoided (Decision's outcome taxonomy, Rule Engine, etc.). The Product
Owner confirmed Q-013 should not build it, while — separately — also
confirming `ActionStatus` should remain a real, `CHECK`-constrained enum
column (single value `PROPOSED` for now) specifically so a future
approval-workflow Requirement can relax the constraint and add a
transition use case without a breaking schema migration. This ADR records
that specific balance — extensibility achieved through schema shape, not
through building unrequested behavior — as the Product Owner's own stated
governing principle for this Requirement, not a default Claude Code chose
unilaterally.

### Two-tier read: reversing this ADR's own drafter's first proposal

An earlier draft of the Q-013 Requirement proposed a single read contract
for Action, reasoning that Q-008 treats Action references as ordinary
case-visible content, unlike Decision's more guarded reasoning content.
The Product Owner confirmed the two-tier narrow/full-detail split instead,
matching Decision/Evidence: the split's cost is negligible (a
third reuse of an already-proven shape) and it preserves optionality (a
cheap existence-only check separate from a full display read; room for a
future Action intent type that does need restricted visibility) and
cross-module consistency. This ADR records the two-tier design as
final for this Requirement, explicitly noting it as a Product Owner
correction of this document's own first proposal, not silently adopted as
if it had been the plan all along.

### Originating-Decision validation: reuse, not reimplementation

Recording an Action validates its originating `DecisionRef` by calling
Q-012's existing narrow provenance-read contract unchanged, with the
recording actor's own `ActorContext`, accepting any `RECOGNIZED` outcome
(Decision currently has no further status to gate on) and rejecting only
`NOT_FOUND`. No Q-009, Q-010, Q-011, or Q-012 file is modified. This is
the third real-world use of the "recognized, not merely eligible" bar
pattern this repository established (Q-011's subject validation via
Q-010, Q-012's evidence validation via Q-011, now Q-013's decision
validation via Q-012), each time reusing an existing narrow contract
without needing to extend it.

### Cross-module reference persistence: no hard foreign key

`action_record.decision_ref` is a validated `CHAR(40)` value, without a
real SQL foreign key to Q-012's `decision_record` table — the same
pattern ADR-014 established for Decision's reference to Evidence.
Cross-module referential validity is enforced by the live
application-layer call described above, at write time, keeping each
module's schema independently ownable.

### Durable storage and schema

Three tables, an additive Flyway migration (exact version number
determined at Implementation Design time): `action_record` (one row per
Action, including the direct `decision_ref` column), `action_operation`
(the idempotency ledger, mirroring `decision_operation`), and
`action_access_log` (the full-detail-read audit trail, mirroring
`decision_access_log`). No `action_operation_history`-equivalent table —
there is no correction to have history of.

### Recording and read exposure

One mutating use case (`POST /api/actions`), matching Decision's single
-mutation shape. The narrow provenance contract remains in-process only.
The full-detail read is exposed over HTTP, requires `action:read` with no
`ActorType` restriction, and commits an access-audit record before
returning content, in a dedicated non-read-only transaction.

### Idempotency

Recording is idempotent by operation identity and a SHA-256 semantic
fingerprint over the raw originating-Decision reference and intent-text
strings — the same mechanism Q-011/Q-012 use, applied to Action's own
content shape. Execution order: authorize → require `HUMAN` → compute
fingerprint → replay check (before any content validation or the Q-012
call) → content validation → Q-012 originating-Decision validation →
commit — the same ordering discipline established by Q-011's
Implementation Design §11.1 and reaffirmed by Q-012's.

## Alternatives Considered

### Alternative 1: Build the PROPOSED-to-APPROVED transition now

Rejected. Q-008 defines no capability, actor authority, or reason
semantics for approval — building the transition now would require
inventing those semantics without any approved specification, exactly the
class of premature invention this repository has repeatedly avoided
elsewhere (Decision's outcome taxonomy, Rule Engine). See "PROPOSED-only
status" above.

### Alternative 2: Omit the status column entirely (add it only when a transition is actually designed)

Rejected. The Product Owner's stated principle (extensibility and
stability) favors keeping the column present and `CHECK`-constrained now,
so a future transition-adding Requirement only needs to relax a
constraint and add an update path, not perform a schema migration that
adds a new column to an existing, populated table. The cost of the
column existing now (one extra `CHECK`-constrained `VARCHAR`) is
negligible.

### Alternative 3: Single read contract for Action (this ADR's own drafter's original proposal)

Rejected by the Product Owner, reversing the original Requirement draft.
See "Two-tier read" above — cross-module consistency and cheap future
optionality outweighed the argument that Action's content had no stated
need to be hidden.

### Alternative 4: A join table for Action-to-Decision, matching Decision's Evidence pattern for consistency

Rejected. Requirement `Q013-FR-001` requires exactly one originating
Decision, not a set — a join table would model a cardinality the
Requirement does not have, adding schema and query complexity for a
relationship that can never hold more than one row. Consistency with
Decision's pattern is not a reason to model a different cardinality the
same way.

### Alternative 5: Cross-module hard foreign key for `decision_ref`

Rejected, for the same reason ADR-014 rejected it for Decision's
reference to Evidence: would contradict the already-shipped pattern,
introduce inter-module schema coupling this modular monolith deliberately
avoids, and complicate any future module reorganization.

## Consequences

Positive:

- Q-008's Action prerequisite becomes resolvable; ActionOutcome remains
  the sole remaining prerequisite.
- Validates Q-012's narrow provenance contract as reusable by a second,
  different consumer (after Decision's own reuse of Q-011's), reinforcing
  that the "recognized, not merely eligible" pattern generalizes across
  the Core Domain chain.
- Establishes, for the first time, an explicit ADR-recorded example of
  the Product Owner overriding this project's own drafter/self-reviewer
  on a substantive design question (the two-tier read decision) — a
  concrete demonstration that self-review flagging genuine open questions
  (rather than silently picking an answer) produces real course
  correction, not just documentation theater.

Costs and constraints:

- A future approval-workflow Requirement must define capability, actor
  authority, and reason semantics for any `PROPOSED`-to-`APPROVED`/
  `REJECTED` transition, and must not silently reinterpret this ADR's
  immutability decision without its own explicit Requirement/ADR cycle.
- A future ActionOutcome Requirement must define its own relationship to
  Action without assuming this ADR already solved it.
- The lack of a cross-module hard FK means an application-level bug could
  theoretically insert an `action_record` row referencing a `DecisionRef`
  that was never validated — mitigated entirely by the application-layer
  validation call happening before every insert, the same trust boundary
  ADR-014 already accepted for Decision's own subject/evidence
  references.

## Security Implications

Identical shape to ADR-014: `AuthorizationGuard.requireAllowed` before any
lookup or mutation; `action:record` requires `HUMAN`, checked immediately
after authorization and before the replay check; `action:read` requires
no specific `ActorType`. The narrow provenance contract cannot expose
`intentText` structurally, not merely by convention. No intent text,
actor identity, or `DecisionRef` in logs or metric tags.

## Data and Integrity Implications

Every Action is permanently attributable to its recording actor, time,
and originating Decision, none of which may be altered after recording.
`ON DELETE RESTRICT` on the one real FK (`action_operation.action_id`,
`action_access_log.action_id` → `action_record.id`). No delete use case,
port, or SQL exists for Action. Full-detail access audited before content
disclosure, in a transaction isolated from the recording path.

## Operational Implications

No new Kafka topic, Redis key, deployment manifest, or external adapter.
Reuses the existing single shared `Clock` bean without introducing a
second one. Same disposable-MySQL-8.4 verification discipline as
Q-009 through Q-012, including proactive avoidance of the hard-coded
-migration-count test pattern documented in
`docs/lessons/2026-08-31-q011-migration-count-test-fix.md`.

## Dependencies

Q-013 reuses Java 21, Spring Boot, Spring JDBC, the local transaction
manager, MySQL, Flyway, and the implemented Q-009 `ActorContext`/
`AuthorizationGuard`/`Capability` contracts and Q-012's existing narrow
provenance-read contract. It depends on ADR-009 domain ownership, ADR-011
trust semantics, ADR-012 Trading Account authority precedent, ADR-013
Evidence provenance precedent, and ADR-014 Decision provenance and its
established cross-module reference pattern.

No new Maven dependency, framework, vendor SDK, service, database, Kafka
topic, Redis key, deployment object, or external integration is required.

## Deferred Decisions

Implementation Design will define exact Java/SQL/endpoint/transaction/
exception/query/test mechanics without reopening this decision.

A future approved Requirement is required for: any `ActionStatus`
transition (`APPROVED`, `REJECTED`, or otherwise) and its capability/actor
/reason semantics; ActionOutcome; any Execution/Account Control adapter
contract; any Action correction/withdrawal mechanism, if ever needed; and
any vendor-specific operation taxonomy.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-09-01** ("接受"),
distinct from and subsequent to Architecture V1's 2026-09-01
authorization to draft this ADR — drafting authorization and acceptance
kept as two separate recorded decisions, matching the discipline ADR-014
established. Implementation Design and implementation authorization each
remain separate, later decisions, per
`docs/engineering/AI-Engineering-Execution-Protocol.md` §2/§3/§12.
