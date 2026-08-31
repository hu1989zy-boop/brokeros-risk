# ADR-014: Decision Provenance Foundation

- Status: **Accepted — 2026-08-31 — Product Owner**
- Date: 2026-08-31
- Approval origin: drafting authorized by explicit Product Owner decision,
  2026-08-31 ("PASS，批准Q-012 Architecture V1，同意开ADR-014"). Prepared by
  Claude Code, holding the external Architect review role by explicit
  Product Owner direction. This is a self-review artifact, not an
  independent one, disclosed here for the same reason every prior ADR in
  this repository discloses it.
- Requirement: Q-012 — Decision Provenance Foundation V1, APPROVED —
  2026-08-31 — Product Owner
- Architecture: `docs/architecture/q-012-decision-provenance-foundation-architecture.md`
  (see its own Document Status for the current version — do not hard-code
  a version number here, per the lesson ADR-013's own amendment history
  already recorded)
- Depends on: ADR-009 (core domain model), ADR-011 (Q-009 trust
  semantics), ADR-012 (Q-010 Trading Account authority), ADR-013 (Q-011
  Evidence provenance)
- Supersedes: None

## Context

Q-008 (Risk Case) cannot begin implementation until Decision, Action, and
ActionOutcome providers all exist (Q-008 §26). Q-007/ADR-009 define
Decision semantically — the Core Domain's explainable risk conclusion,
attributable to Evidence — but authorize no runtime implementation. Q-009
(actor/authorization), Q-010 (Trading Account reference), and Q-011
(Evidence provenance) are already implemented and reusable. Decision is
the next prerequisite, and the first of the three to be started.

Q-012 Requirement V1 (approved 2026-08-31) resolves the business
boundary: `DecisionSource` limited to `MANUAL`, recording limited to
`HUMAN` actors, a Decision immutable once recorded (no correction or
supersession — a deliberate departure from Q-011's pattern, directly
required by Q-008 §12's own statement that "reassessment creates a new
Decision... never overwrites or mutates the historical Decision"), at
least one supporting `EvidenceRef` required, and a two-tier read-contract
design mirroring Q-011's. Architecture (V1, approved 2026-08-31) resolved
identity/content representation, subject and Evidence validation reuse,
durable storage shape, and the consumer boundary, and additionally
corrected a Requirement-stage technical aside about cross-module foreign
keys after checking the actual committed Q-011 schema. This ADR records
the resulting architectural decision as a durable, citable artifact — the
same role ADR-011/012/013 play for Q-009/Q-010/Q-011.

## Decision

### Bounded capability and identity ownership

Q-012 introduces `com.brokeros.risk.decision`, a new module inside the
existing Phase 1 modular monolith, giving the Core Domain (Decision, per
ADR-009) its first real implementation. It owns:

- `DecisionRef` identity (`dec-<canonical-lowercase-UUIDv4>`);
- bounded, immutable Decision content: subject (`TradingAccountRef`),
  evidentiary basis (one or more `EvidenceRef`), a free-text conclusion,
  recording actor, and record time;
- the recording use case and its idempotency ledger;
- a narrow, in-process provenance/confirmation read contract; and
- an audited full-detail read.

It owns no Risk Case, Action, ActionOutcome, Rule, or Rule Engine
behavior. It does not implement automated/`SERVICE`-sourced Decisions —
that remains explicitly deferred to a future Requirement, matching how
ADR-013 deferred automated Evidence sources.

### Immutability instead of correction

Unlike Evidence (ADR-013), Decision has no correction or supersession use
case. This is not a scope reduction invented by this ADR — it is what
Q-008 §12 itself already requires ("reassessment creates a new Decision
in the Core Domain; it never overwrites or mutates the historical
Decision"). Consequently, Q-012's schema has no status column, no
supersession chain, and no correction-history table — a genuine
architectural simplification relative to Q-011, traced to an actual
domain difference (Decision's conclusion is a point-in-time explainable
fact; Evidence's content is a mutable-with-audit-trail record), not an
oversight.

### Subject and Evidence validation: reuse, not reimplementation

Recording a Decision validates its subject by calling Q-010's existing
`TradingAccountReferenceEligibilityService.validateForNewRiskCaseAssociation`
unchanged, with the recording actor's own `ActorContext` — identical to
how Q-011 validates its subject. It validates each referenced `EvidenceRef`
by calling Q-011's existing narrow provenance-read contract unchanged,
once per distinct reference, accepting Evidence in either `ACTIVE` or
`SUPERSEDED` status and rejecting only a not-found reference. No Q-009,
Q-010, or Q-011 file is modified. This is the same "recognized, not
merely eligible" bar Q-011 established for its own subject validation,
now applied a second time for a second dependency (Evidence), and is also
the first real-world use of Q-011's narrow provenance contract by
something other than the as-yet-unbuilt Q-008 — validating that Q-011's
contract was actually designed reusably, not just described as such.

### No eligibility-style service for Decision

Q-010 needed a tri-state eligibility check
(`ELIGIBLE_FOR_NEW_ASSOCIATION` / `RECOGNIZED_NOT_ELIGIBLE` /
`NOT_RECOGNIZED`) because a trading account's own lifecycle state can
change after creation, independent of any consumer, and that mutable
state genuinely affects whether a new association should be allowed.
Decision has no equivalent mutable state — it exists once recorded and
never changes. The fact that Q-008 §12 limits a Decision to "at most one
primary case association" is a fact that lives entirely in Risk Case's
own data (which case currently claims which Decision as primary), not in
Decision's — Q-008 must enforce that itself (e.g., via a uniqueness
constraint on its own schema) rather than Decision tracking "already
claimed" state on its behalf, which would duplicate ownership across
module boundaries. Q-012 therefore exposes only a "recognized or not"
read, deliberately narrower than Q-010's contract, and this ADR records
that the narrower shape is correct, not incomplete.

### Cross-module reference persistence: no hard foreign key

Decision's evidentiary references are stored in a join table
(`decision_evidence_reference`) as validated `CHAR(39)` `EvidenceRef`
values, without a real SQL foreign key to Q-011's `evidence_record` table.
This corrects a Requirement-stage technical aside (see Q-012 Requirement
§9's own correction note) that had assumed a hard FK, before checking
that Q-011's own already-committed schema does not use one for its
subject reference to Q-010 either. Cross-module referential validity is
enforced by the live application-layer service calls described above, at
write time — not by a database constraint. This keeps each module's
schema independently ownable inside the shared physical database, the
same modular-monolith property ADR-002/ADR-009 already commit to at the
code level; this ADR makes explicit that the same boundary applies at the
schema level too, for any future Q-0XX module that references another
module's identity.

### Durable storage and schema

Four tables, an additive Flyway migration (exact version number
determined at Implementation Design time from the actual then-current
committed state): `decision_record` (one row per Decision),
`decision_evidence_reference` (the one-to-many join to `EvidenceRef`,
intra-module FK to `decision_record.id` only), `decision_operation` (the
idempotency ledger, mirroring `evidence_operation`), and
`decision_access_log` (the full-detail-read audit trail, mirroring
`evidence_access_log`). No `decision_operation_history`-equivalent table
exists — there is no correction to have history of.

### Recording and read exposure

Exactly one mutating use case (Record) is exposed over HTTP
(`POST /api/decisions`), unlike Q-011's two (Record and Correct) — there
is nothing to correct. The narrow provenance contract remains in-process
only, not an HTTP route, matching Q-011's forward-compatible-but-unwired
posture toward its own not-yet-built consumer. The full-detail read is
exposed over HTTP, requires `decision:read` with no `ActorType`
restriction, and commits an access-audit record before returning content,
in a dedicated non-read-only transaction — identical discipline to Q-011.

### Idempotency

Recording is idempotent by operation identity and a SHA-256 semantic
fingerprint over subject, the de-duplicated `EvidenceRef` set, and the
conclusion text — the same mechanism Q-011 uses, applied to Decision's own
content shape. Execution order: authorize → require `HUMAN` → compute
fingerprint → replay check (before any content validation or Q-010/Q-011
call) → content validation → Q-010 subject validation → Q-011 Evidence
validation (once per distinct reference) → commit. This is the same
ordering discipline Q-011's Implementation Design §11.1 established, after
several governance rounds proved how easily it drifts if not stated as a
single authoritative sequence.

## Alternatives Considered

### Alternative 1: Give Decision an eligibility service mirroring Q-010's

Rejected. Decision has no mutable per-reference state after recording;
building a tri-state eligibility check would duplicate the "already
claimed by a case" fact that belongs to Q-008's own data, and would be
speculative capability nothing has actually asked for. See "No
eligibility-style service for Decision" above.

### Alternative 2: Cross-module hard foreign keys for both subject and Evidence references

Rejected. Would contradict the pattern Q-011 already established and
shipped (no hard FK from `evidence_record.subject_ref` to Q-010's table),
introduce inter-module schema coupling this modular monolith has
deliberately avoided elsewhere, and complicate any future module
reorganization (e.g., a later move toward separately deployable services)
that a hard cross-schema FK would resist.

### Alternative 3: Allow Decision to reference a superseded Evidence only through its replacement

Rejected. Requiring Decision to always resolve to the current/`ACTIVE`
Evidence would silently change what evidentiary basis a Decision appears
to rest on after a later, unrelated correction — the opposite of
explainability. A Decision must continue to point at the Evidence set it
was actually recorded against.

### Alternative 4: A single-Evidence-reference Decision (one-to-one, not one-to-many)

Rejected. Requirement `Q012-FR-004` requires at least one `EvidenceRef`
but does not cap it at one; a Decision synthesizing multiple pieces of
Evidence is a normal, expected case, and a join table costs nothing extra
in complexity relative to a single nullable-or-not column.

### Alternative 5: Add `decision_operation_history` now as future-proofing for a correction feature that might be added later

Rejected. Building an unused table for a use case the current Requirement
explicitly forbids is speculative schema, contrary to this repository's
stated discipline against designing for hypothetical future requirements.
If a future Requirement adds Decision correction, that Requirement should
design its own audit-history shape against whatever the actual correction
semantics turn out to be.

## Consequences

Positive:

- Q-008's Decision prerequisite becomes resolvable; two prerequisites
  (Action, ActionOutcome) remain.
- Establishes the Core Domain's first real, persisted implementation,
  validating that ADR-009's conceptual model (Evidence → Decision → Action
  → Risk Case) actually composes correctly once two of its stages are
  real code, not just design documents.
- Validates Q-011's narrow provenance contract as genuinely reusable by a
  second consumer, not merely designed to look reusable.
- Establishes the "no hard cross-module foreign key" pattern as an
  explicit, ADR-recorded architectural rule rather than an implicit
  convention only visible by reading Q-011's SQL.

Costs and constraints:

- A future Rule Engine Requirement, when it arrives, must add
  `DecisionSource.AUTOMATED` and design how an automated Decision's
  authorship differs from a `HUMAN`-recorded one, without reopening this
  ADR's immutability decision.
- If a future Requirement determines Decision itself (not just Risk Case)
  needs to track that a later Decision reassessed an earlier one, that
  requires a new Requirement and likely a new ADR, not a silent schema
  addition.
- The lack of a cross-module hard FK means an application-level bug could
  theoretically insert a `decision_evidence_reference` row pointing at an
  `EvidenceRef` that was never validated — mitigated entirely by the
  application-layer validation call happening before every insert, the
  same trust boundary Q-011 already accepted for its own subject
  reference.

## Security Implications

Identical shape to ADR-013: `AuthorizationGuard.requireAllowed` before any
lookup or mutation; `decision:record` requires `HUMAN`, checked
immediately after authorization and before the replay check;
`decision:read` requires no specific `ActorType`. The narrow provenance
contract cannot expose the conclusion text structurally (no such field on
the type), not merely by convention. No conclusion text, actor identity,
or subject reference in logs or metric tags.

## Data and Integrity Implications

Every Decision is permanently attributable to its recording actor, time,
subject, and evidentiary basis, none of which may be altered after
recording. `ON DELETE RESTRICT` on the one intra-module FK
(`decision_evidence_reference.decision_id` → `decision_record.id`). No
delete use case, port, or SQL exists for Decision. Full-detail access
audited before content disclosure, in a transaction isolated from the
recording path so neither blocks the other under normal concurrent
operation.

## Operational Implications

No new Kafka topic, Redis key, deployment manifest, or external adapter.
Reuses the existing single shared `Clock` bean
(`SecurityModuleConfiguration.securityClock()`) without introducing a
second one. Same disposable-MySQL-8.4 verification discipline as
Q-009/Q-010/Q-011: no H2 substitution, no skipped mandatory database test.

## Dependencies

Q-012 reuses Java 21, Spring Boot, Spring JDBC, the local transaction
manager, MySQL, Flyway, and the implemented Q-009 `ActorContext`/
`AuthorizationGuard`/`Capability` contracts, Q-010's existing
`validateForNewRiskCaseAssociation` contract, and Q-011's existing narrow
provenance-read contract. It depends on ADR-009 domain ownership, ADR-011
trust semantics, ADR-012 Trading Account authority, and ADR-013 Evidence
provenance and its established cross-module reference pattern.

No new Maven dependency, framework, vendor SDK, service, database, Kafka
topic, Redis key, deployment object, or external integration is required.

## Deferred Decisions

Implementation Design will define exact Java/SQL/endpoint/transaction/
exception/query/test mechanics without reopening this decision.

A future approved Requirement is required for: `DecisionSource.AUTOMATED`
and any Rule-Engine-produced Decision; any Decision correction,
reassessment-linking, or supersession mechanism; any Decision-history
-per-case query beyond the narrow provenance contract, if Q-008 later
needs one; and any additional Decision subject type beyond
`TRADING_ACCOUNT`.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-08-31** ("ADR-014
Accept"), distinct from and subsequent to Architecture V1's 2026-08-31
authorization to draft this ADR — drafting authorization and acceptance
were kept as two separate recorded decisions, not conflated. Implementation
Design and implementation authorization each remain separate, later
decisions, per
`docs/engineering/AI-Engineering-Execution-Protocol.md` §2/§3/§12.
