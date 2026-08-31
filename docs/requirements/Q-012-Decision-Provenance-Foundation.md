# Q-012: Decision Provenance Foundation

## Status

Drafted 2026-08-31, V1, by Claude Code holding the external Architect
role, per `docs/engineering/AI-Engineering-Execution-Protocol.md`. This is
the Requirement stage of a new task. **Not yet approved.** Self-reviewed
before presentation, but self-review is a disclosed limitation, not a
substitute for Product Owner approval — see §17.

- Requirement ID: `Q-012`
- Architecture phase: Phase 1
- Authoritative domain model: ADR-009 (`Evidence → Decision → Action →
  Risk Case`; Decision is the Core Domain)
- Depends on: Q-009 (Trusted Actor Authorization), Q-010 (Trading Account
  Reference Authority), Q-011 (Evidence Provenance Foundation) — all
  approved and implemented; Q-011 in particular is reused, not modified.
- Unblocks (partially): Q-008 (Risk Case) implementation, which is
  authorized to begin only after Decision, Action, and ActionOutcome
  providers are all resolved (Q-008 §26). Q-012 resolves the Decision
  provider only.

## 1. Background

Q-007 established the canonical domain model and named Decision the Core
Domain: the explainable risk conclusion derived from Evidence. Q-008
(Risk Case) was designed against this model and is fully approved through
Implementation Design, but its own Implementation Gate (§26) explicitly
blocks implementation until real authoritative Decision, Action, and
ActionOutcome providers exist. Q-009 (actor/authorization), Q-010 (trading
account reference), and Q-011 (evidence provenance) are the three
prerequisites already resolved; Decision is the next.

Q-008 does not need to own or produce Decisions — Risk Case is explicitly
downstream and "cannot produce, approve, overwrite, or delete a Decision"
(Q-008 §7.5). It needs to be able to *reference* an existing Decision
(for `DECISION_DRIVEN` intake and for changing a case's current
case-relevant Decision reference) and to *trust* that reference without
being able to see or duplicate Decision's own internal reasoning, which
Q-008 explicitly defers ("Decision outcome taxonomy, confidence, rule
metadata, approval semantics... are deferred," Q-008 §12).

This mirrors exactly the shape of the Q-010/Q-011 problem: a narrow,
trustworthy reference contract for a downstream consumer, without
building the full capability the Core Domain will eventually need (a
Rule Engine producing automated Decisions remains explicitly out of scope
per Q-007 and ADR-009).

## 2. Existing Capability and Gap Analysis

| Need | Existing capability | Gap |
| --- | --- | --- |
| Explainable risk conclusion, attributable to Evidence | None | No `Decision` type, storage, or recording use case exists anywhere in the repository |
| Evidence to base a Decision on | Q-011 Evidence Provenance Foundation (`EvidenceRef`, narrow provenance-read contract) | None — reusable as-is |
| Subject (which trading account a Decision concerns) | Q-010 Trading Account Reference Authority | None — reusable as-is |
| Actor identity/authorization for recording a Decision | Q-009 Trusted Actor Authorization | None — reusable as-is |
| A narrow reference contract Q-008 can trust for `DECISION_DRIVEN` intake and current-decision-reference changes, without exposing Decision's internal reasoning | None | This is what Q-012 must deliver |
| Automated/Rule-Engine-produced Decisions | None; explicitly deferred by Q-007/ADR-009 | Out of scope for Q-012 — human-recorded Decisions only |
| Decision correction/reassessment | N/A — Q-008 §12 states reassessment always creates a *new* Decision and never mutates a historical one | Q-012 must NOT provide a correction/supersede use case; this is a deliberate scope reduction relative to Q-011's pattern, not an oversight |

## 3. Problem Statement

Q-008 cannot begin implementation until it can reference a real,
trustworthy Decision. Without Q-012, any attempt to implement Q-008's
`DECISION_DRIVEN` intake or its "current case-relevant Decision
reference" would have to either fabricate a Decision (violating Q-008's
own explicit prohibition) or block indefinitely.

Q-012 must provide exactly enough Decision capability — recording a
Decision, and a narrow, trustworthy read/reference contract — for Q-008 to
consume, without prematurely designing the Rule Engine, an outcome
taxonomy, confidence scoring, or any decisioning automation that belongs
to later, separately-approved work.

## 4. Goals

1. Establish `Decision` as a real, persisted Core Domain concept:
   identity, subject, evidentiary basis, an explainable conclusion, and
   authorship — attributable and immutable once recorded.
2. Provide a narrow provenance/reference contract a future Q-008 can
   consume to validate a Decision reference and display minimal,
   non-sensitive metadata, without exposing the Decision's own conclusion
   text.
3. Provide a separate, audited full-detail read for actors who are
   authorized to see a Decision's actual conclusion.
4. Require `ActorType.HUMAN` for recording a Decision — the only source
   this Requirement authorizes, since no automated producer (Rule Engine)
   exists yet. Reads do not require `HUMAN`.
5. Reuse Q-009, Q-010, and Q-011 exactly as they exist; modify none of
   them.
6. Explicitly do not implement Decision correction, reassessment,
   outcome taxonomy, confidence, rule metadata, approval workflow, or any
   Rule Engine behavior — Q-008 defers all of these, and so does Q-012.

## 5. Scope and Non-Goals

### 5.1 In Scope

- `DecisionRef` identity type (`dec-<uuidv4>`, lowercase canonical form,
  matching the `ev-`/`ta-` convention already established).
- Recording a Decision: subject (`TradingAccountRef`, validated as
  "recognized" via Q-010's existing eligibility service — the same
  "recognized" bar Q-011 uses, i.e. accept both
  `ELIGIBLE_FOR_NEW_ASSOCIATION` and `RECOGNIZED_NOT_ELIGIBLE`, reject
  only `NOT_RECOGNIZED`), at least one supporting `EvidenceRef`
  (validated as "recognized" via Q-011's existing narrow provenance-read
  contract — accepting evidence in either `ACTIVE` or `SUPERSEDED`
  status, since a Decision's historical basis remains valid even after
  the Evidence it relied on is later corrected), a free-text conclusion
  (bounded length, non-blank, matching Q-011's `ObservationText`-style
  validation), the recording actor, and the record time.
- Idempotent recording: a replayed request with the same operation
  identity and unchanged content returns the original outcome without
  creating a duplicate Decision, matching Q-011's operation-ledger
  pattern.
- A narrow, in-process `confirmProvenance`-style contract (mirroring
  Q-011 §10.3): given a `DecisionRef`, returns whether it is recognized,
  and if so, its subject, evidentiary `EvidenceRef` set, recording actor,
  and record time — never the conclusion text.
- A full-detail read (including the conclusion text) for actors
  authorized under a dedicated capability, with the access committed to
  an audit log before the content is returned — mirroring Q-011's
  full-detail read exactly.
- Real MySQL 8.4 migration and test coverage, following the same rigor
  Q-009/Q-010/Q-011 established (disposable database, no H2 substitution,
  exhaustive constraint coverage).

### 5.2 Non-Goals (explicitly deferred)

- Decision correction, reassessment, supersession, or any mutation after
  recording. A Decision is immutable once recorded. (Reassessment is a
  *new* Decision — Q-008's own concern when it eventually implements
  `DECISION_DRIVEN` re-intake; Q-012 does not need to link a new Decision
  back to a prior one, since Q-008 §12 already states Automated and
  manual Decisions "coexist as separate upstream Decision records" and
  the *case*, not Decision itself, tracks its own Decision history.)
- Rule Engine, automated/`SERVICE`-sourced Decisions, rule versioning,
  rule evaluation, or any decisioning automation.
- Decision outcome taxonomy, severity, confidence scoring, or approval
  workflow — Q-008 explicitly defers these, and Q-012 must not invent
  them either.
- An "eligibility for new case association" state machine analogous to
  Q-010's (see §5.3 for why this is a deliberate omission, not a gap).
- Any Q-008 code, wiring, or `RiskCase` type. Q-012 does not implement or
  touch Risk Case.
- Any Action, ActionOutcome, Alert, or Rule Hit concept.
- Deleting a Decision. No delete use case, port, or SQL exists.
- Any AI/LLM-generated Decision content.

### 5.3 Why no eligibility-for-case-association service (unlike Q-010)

Q-010 needed `validateForNewRiskCaseAssociation` because a trading
account's *own* lifecycle state (e.g., closed, suspended) can change over
time independent of any case, and that state genuinely affects whether a
new association should be allowed. Q-008 §12's "a formal Decision has at
most one primary case association" is a different kind of rule: it is
about whether *another case* already claims a Decision as primary — that
fact lives entirely in Risk Case's own data, not in Decision's. Decision
itself has no lifecycle state that changes after recording (it is
immutable). Requiring Decision to track "already claimed by a case" would
duplicate state Q-008 already owns and must enforce itself (e.g. via a
uniqueness constraint on its own schema). Q-012 therefore provides only a
"recognized or not" read, not a stateful eligibility check — a
deliberately smaller contract than Q-010's, justified by an actual
difference in the underlying domain, not an oversight to be caught later.

## 6. Domain Definitions

- **Decision** — the Core Domain's explainable risk conclusion. Recorded
  once, immutable thereafter, attributable to a subject
  (`TradingAccountRef`) and at least one supporting `EvidenceRef`, authored
  by a `HUMAN` actor at a point in time, and carrying a free-text
  conclusion.
- **DecisionRef** — canonical identity, `dec-<uuidv4>`.
- **DecisionSource** — enumerated set of how a Decision was produced.
  This Requirement authorizes exactly one value: `MANUAL`. No other
  value may be added without a separate approved Requirement (mirrors
  Q-011's `EvidenceSource` pattern exactly).
- **Decision conclusion** — free-text explainable rationale. Not a
  structured outcome/severity/confidence field; those remain deferred to
  a future Rule Engine Requirement.

## 7. Functional Requirements

- **Q012-FR-001:** The system shall support recording a Decision given an
  actor context, a `TradingAccountRef` subject, one or more
  `EvidenceRef`s, and a conclusion.
- **Q012-FR-002:** Recording shall require `ActorType.HUMAN` and the
  `decision:record` capability under Q-009's `AuthorizationGuard`.
- **Q012-FR-003:** Recording shall validate the subject as "recognized"
  via Q-010's existing eligibility service, accepting
  `ELIGIBLE_FOR_NEW_ASSOCIATION` or `RECOGNIZED_NOT_ELIGIBLE`, rejecting
  only `NOT_RECOGNIZED`.
- **Q012-FR-004:** Recording shall validate every referenced `EvidenceRef`
  as "recognized" via Q-011's existing narrow provenance-read contract,
  accepting either `ACTIVE` or `SUPERSEDED` Evidence status, rejecting
  only a not-found reference. At least one `EvidenceRef` is required; the
  system shall reject a Decision with zero evidentiary references.
  Referenced Evidence need not share the same subject as the Decision —
  Q-012 does not enforce subject-consistency across evidence, since doing
  so is not required by Q-008 and would be an invented constraint.
- **Q012-FR-005:** Recording shall be idempotent by operation identity and
  semantic content fingerprint, matching Q-011's replay pattern exactly:
  an exact replay returns the original outcome without a new mutation; a
  changed request under the same operation identity is rejected as a
  conflict.
- **Q012-FR-006:** A Decision, once recorded, shall never be corrected,
  superseded, or deleted through any use case this Requirement
  authorizes.
- **Q012-FR-007:** The system shall provide a narrow, in-process
  provenance/confirmation read: given a `DecisionRef`, return whether it
  is recognized, and if so, its subject, evidentiary `EvidenceRef` set,
  recording actor reference, and record time. This contract shall never
  return the conclusion text. It requires the `decision:read` capability
  but no specific `ActorType`.
- **Q012-FR-008:** The system shall provide a full-detail read (including
  the conclusion text) requiring the `decision:read` capability, with no
  specific `ActorType` restriction, that commits an access-audit record
  before returning content — mirroring Q-011's full-detail read exactly,
  including that a failed audit write must prevent content disclosure.
- **Q012-FR-009:** No HTTP endpoint shall expose the narrow provenance
  contract; it exists for future in-process consumption only (mirrors
  Q-011 §10.3 — Q-008 does not yet exist to consume it, but the shape must
  be ready). The full-detail read may be exposed over HTTP for
  authorized human/service review, matching Q-011's controller pattern.

## 8. Security Requirements

- All use cases call Q-009's `AuthorizationGuard.requireAllowed` before
  any Decision lookup or mutation. Default-deny; no implicit grant.
- Capabilities: `decision:record`, `decision:read`. No other capability
  is introduced.
- Recording requires `ActorType.HUMAN`; both read use cases permit any
  authorized `ActorType`, including `SERVICE` — the same asymmetry Q-011
  established and for the same reason (there is no automated Decision
  producer yet, but automated *readers*, e.g. a future Q-008 background
  process, are legitimate).
- The narrow provenance contract must be structurally incapable of
  returning the conclusion text — enforced by the return type having no
  such field, not by convention, mirroring `EvidenceProvenanceView`'s
  compact-constructor invariant.
- No conclusion text, actor identity, or subject reference may appear in
  logs or metric tags — mirrors Q-011's observability constraints
  exactly.

## 9. Data Integrity and Provenance Requirements

- Every Decision is permanently attributable to its recording actor,
  time, subject, and evidentiary basis; none of these may be altered
  after recording.
- Referential integrity to `EvidenceRef`: a join table for the
  one-to-many Decision-to-Evidence relationship, with a validated
  `EvidenceRef` value per row. **Corrected at the Architecture stage**
  (see `docs/architecture/q-012-decision-provenance-foundation-architecture.md`
  §8/Document Status): Q-011's own already-committed schema does not use
  a real cross-module SQL foreign key for its own subject reference to
  Q-010 — cross-module validity is enforced by a live application-layer
  service call instead, keeping each module's schema independently
  ownable. Q-012 follows that same established pattern rather than the
  hard-FK approach this line originally guessed before checking the
  actual precedent.
- Full-detail access must be audited before content is returned, in a
  dedicated, non-database-read-only transaction — mirroring Q-011's
  `evidence_access_log` pattern.
- Idempotency: an exact replay (same operation id, same semantic
  fingerprint) never creates a second Decision row; a changed replay
  under the same operation id is rejected, never silently applied.

## 10. Acceptance Criteria

1. `DecisionRef`/operation identity use the canonical lowercase UUIDv4
   forms established by Q-010/Q-011.
2. Recording requires `HUMAN` and `decision:record`; both reads require
   only `decision:read`, with no `ActorType` restriction.
3. Recording accepts a subject in either recognized eligibility state
   from Q-010 and rejects only `NOT_RECOGNIZED`.
4. Recording accepts Evidence in `ACTIVE` or `SUPERSEDED` status from
   Q-011 and rejects only a not-found `EvidenceRef`; at least one
   `EvidenceRef` is mandatory.
5. A Decision is immutable after recording — no correction, supersession,
   or delete use case exists anywhere in the implementation.
6. The narrow provenance/confirmation read never returns the conclusion
   text, structurally (not by convention).
7. The full-detail read commits an access-audit record before returning
   the conclusion text; a failed audit write returns no content.
8. Idempotent replay (same operation id + unchanged content) returns the
   original outcome without a new database row; a changed replay under
   the same operation id is rejected.
9. No Q-008, Action, ActionOutcome, Alert, Rule Hit, or Rule Engine code
   exists anywhere in the change.
10. No existing Q-009/Q-010/Q-011 file is modified.
11. All mandatory tests, including real MySQL 8.4 integration/persistence/
    concurrency/security tests, pass; no mandatory test is skipped.

## 11. Technical Constraints

- Java 21 / Spring Boot modular monolith, `com.brokeros.risk.decision`
  package, matching the existing module layout convention
  (domain/application/application.port/infrastructure.persistence/
  infrastructure.configuration/infrastructure.observability/
  interfaces.rest).
- Additive Flyway migration only (next available version number); no
  existing migration (V1–V4) may be modified.
- Reuse the single shared `Clock` bean
  (`SecurityModuleConfiguration.securityClock()`) — do not introduce a
  second Clock bean.
- MySQL-compatible constraints only; apply the `IS NOT NULL`-guarded
  CHECK pattern documented in `docs/skills/development-standards.md` for
  any nullable-column constraint.

## 12. Dependencies

- Q-009 (`ActorContext`, `Capability`, `AuthorizationGuard`) — reused
  unchanged.
- Q-010 (`TradingAccountReferenceEligibilityService`) — reused unchanged.
- Q-011 (`EvidenceRef`, its narrow provenance-read contract) — reused
  unchanged. Q-012 is the second consumer of that contract (the first,
  Q-008, still does not exist yet); this Requirement is also the first
  real-world validation that Q-011's narrow contract is actually reusable
  by something other than Q-008, which was its originally stated purpose.
- Does not depend on and does not implement any part of Q-008.

## 13. Verification Plan

- Real disposable MySQL 8.4 migration, persistence, concurrency
  (duplicate/replay), and security integration tests, matching the rigor
  and structure of Q-011's `Q011MySqlMigrationTests`/
  `Q011MySqlPersistenceTests`/`Q011SecurityMySqlIntegrationTests`.
- Architecture tests proving the narrow provenance type cannot expose the
  conclusion text (mirrors `EvidenceArchitectureTests`).
- Full repository-wide real-MySQL gate (`Q009`/`Q010`/`Q011`/`Q012` all
  enabled) must pass with zero unexplained failures.
- Static verification (`scripts/verify-static.sh`) extended with a
  Q-012-specific section mirroring the existing Q-010/Q-011 pattern.

## 14. Risks and Architecture Inputs

- Risk: a future Rule Engine Requirement may need to reopen this
  Requirement to add `DecisionSource.AUTOMATED` — anticipated, not
  prevented; the `DecisionSource` enum pattern already supports
  controlled extension the same way `EvidenceSource` does.
- Risk: if Q-008 later needs a Decision-history-per-case query that this
  narrow contract cannot serve, that is Q-008's own aggregate's concern
  (per §5.3) and should not retroactively expand Q-012's contract without
  a new approved Requirement.
- Architecture input needed: exact join-table shape for the
  Decision-to-Evidence many-relationship, and confirmation that a
  Decision-to-Evidence join table (rather than a bounded array column) is
  the right persistence shape — left to the Architecture stage.

## 15. Deliverables

- Approved `docs/requirements/Q-012-Decision-Provenance-Foundation.md`.
- Architecture document, ADR, and Implementation Design, following the
  same gate sequence as Q-009/Q-010/Q-011.
- `com.brokeros.risk.decision` module implementation, additive migration,
  full test suite.
- Lessons Learned entries for any reusable pattern.
- Non-overwriting, timestamped review packages under `review/q-012/` at
  each gate.

## 16. Review Checklist

- [ ] Requirement self-reviewed by Claude Code (external Architect role).
- [ ] Product Owner Gate Decision recorded (§17).
- [ ] No Q-008/Q-009/Q-010/Q-011 file referenced for modification.
- [ ] Decision immutability (no correction/supersede) stated unambiguously
      throughout, not just in one section.
- [ ] Narrow-vs-full-detail read split stated unambiguously, matching
      Q-011's pattern.
- [ ] §5.3's reasoning for omitting an eligibility service is either
      accepted or explicitly overridden by the Product Owner before
      Architecture begins.

## 17. Current Gate

Q-012 Requirement status: **APPROVED — V1 — 2026-08-31 — Product Owner.**
Gate Decision: **PASS**, including §5.3's reasoning for omitting a
Q-010-style eligibility service (discussed and accepted, not silently
carried over).

Q-012 Implementation Allowed: **NO — Requirement approval is not
Architecture, ADR, Design, or Implementation authorization.**

Q-012 Architecture status: **APPROVED — V1 — 2026-08-31 — Product Owner.**
See `docs/architecture/q-012-decision-provenance-foundation-architecture.md`
§24. Includes a corrected Requirement §9 technical aside (no cross-module
foreign key; see that section).

Q-012 ADR: **AUTHORIZED to be drafted — 2026-08-31 — Product Owner**
(ADR-014).

ADR-014 status: **ACCEPTED — 2026-08-31 — Product Owner.** See
`docs/adr/ADR-014-decision-provenance-foundation.md`.

Q-012 Implementation Design status: **DRAFTED, self-reviewed — V1 —
2026-08-31 — Claude Code. Not yet approved.** See
`docs/architecture/q-012-decision-provenance-foundation-implementation-design.md`.

Q-012 Implementation Design: **APPROVED — V1 — 2026-08-31 — Product
Owner.**

Q-012 Implementation: **AUTHORIZED — 2026-08-31 — Product Owner**, against
Requirement V1 / Architecture V1 / ADR-014 (Accepted) / Implementation
Design V1.

Q-012 Implementation Allowed: **YES.**

Q-012 Implementation: **SUBMITTED — 2026-08-31 — Codex.** Review package
`review/q-012/review-q-012-v5-implementation-20260831-214033/`. Codex's
own Gate Decision: BLOCKED — Q-012's 19 mandatory MySQL tests pass, but
the mandatory all-module (Q-009/Q-010/Q-011/Q-012) real-MySQL gate has one
failure: `Q011MySqlMigrationTests.migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart`,
an unchanged Q-011 test whose hard-coded post-V3 migration count went
stale the moment V5 was added, mirroring AC15's earlier `Q009MySqlIntegrationTests`
pattern exactly. Codex correctly did not modify the Q-011 file to fix it,
honoring the Q-012 Prompt's hard boundary.

Q-012 Implementation Independent Review: **BLOCKED — 2026-08-31 — Claude
Code.** Package
`review/q-012/review-q-012-v6-claude-code-independent-review-20260831-224500/`.
Verified by direct code inspection (§11.1 execution order,
§8.5 constraint-to-test mapping, structural narrow-contract guarantee,
HTTP surface, access-log-before-disclosure transaction propagation) and
by independently re-executing the full test suite in Docker/Java 21/MySQL
8.4: 165 tests, 1 failure — the same `Q011MySqlMigrationTests` failure
Codex reported, confirmed genuine and confirmed unrelated to Q-012's own
correctness. **No defect found in Q-012's own implementation.**
Recommendation: accept Q-012's implementation; separately authorize the
same dynamic-migration-count fix pattern already used for AC15, applied
this time to `Q011MySqlMigrationTests.java` only.

Q-012 AC 11 Fix: **AUTHORIZED — 2026-08-31 — Product Owner** ("接受修复"),
narrowly scoped to `Q011MySqlMigrationTests.java` only (same dynamic
-count pattern already used for AC15). See
`prompts/Q-011-Migration-Count-Test-Fix-Prompt.md`. Not yet executed.

Q-012 AC 11 Fix: **VERIFIED — 2026-08-31 — Claude Code.** Package
`review/q-012/review-q-012-v7-full-gate-closure-20260831-225443/` (Codex)
+ `review/q-012/review-q-012-v6-claude-code-independent-review-20260831-224500/V7ClosureIndependentReview.md`
(Claude Code independent re-verification: diff confirmed exact and
narrowly scoped; full Q-009/Q-010/Q-011/Q-012 real-MySQL gate
independently re-executed in a second environment, 165/165, 0
failures/errors/skips). **All 11 Q-012 Acceptance Criteria now PASS.**

Noted, not acted on: Codex's own recurrence scan (confirmed independently)
found `Q012MySqlMigrationTests.java` carries the identical latent
hard-coded-migration-count pattern, currently dormant (only fails once a
migration after V5 exists). Tracked as a minor follow-up, not a Q-012
blocker.

Q-012 Implementation: **ACCEPTED — 2026-08-31 — Product Owner** ("接受"),
based on Codex's v5/v7 implementation packages and Claude Code's
independent review (v6 + its V7ClosureIndependentReview.md addendum),
including independently re-executed tests confirming all 11 Acceptance
Criteria PASS.

Q-012 Ready for Git Commit: **NOT YET** — acceptance of the implementation
is a separate decision from staging/commit/push, which requires its own
explicit Product Owner instruction.

Q-012 Git Commit / Push: **NOT PERFORMED.**

Next gate: Product Owner decision on staging/commit (and separately,
push). The tracked `Q012MySqlMigrationTests` latent-pattern follow-up
(see above) remains open and unscheduled.
