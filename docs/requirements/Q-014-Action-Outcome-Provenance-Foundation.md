# Q-014: Action Outcome Provenance Foundation

## Status

V1, by Claude Code holding the external Architect role, per
`docs/engineering/AI-Engineering-Execution-Protocol.md` and
`docs/engineering/Architecture-and-Design-Decision-Principles.md`.
**Status: APPROVED — 2026-09-01 — Product Owner, and implementation
AUTHORIZED (see §17, which is authoritative if this summary ever appears
to disagree).** This note records the drafting history: at the Requirement
stage the three §5.3 business-scope questions were surfaced for explicit
Product Owner confirmation (per Decision Authority §16.2) rather than
silently assumed, because ActionOutcome sits at the Action/Execution
boundary; all three were subsequently confirmed as recommended.

- Requirement ID: `Q-014`
- Architecture phase: Phase 1
- Authoritative domain model: ADR-009 (`Evidence → Decision → Action →
  Risk Case`; Execution is downstream of Action and **outside** the Core
  Domain)
- Depends on: Q-009 (Trusted Actor Authorization), Q-013 (Action
  Provenance Foundation) — approved, implemented, committed; Q-013 reused,
  not modified.
- Unblocks: Q-008 (Risk Case) implementation. Q-008 §26 blocks
  implementation until Decision (Q-012 ✓), Action (Q-013 ✓), and
  **ActionOutcome** providers all exist. Q-014 resolves the last one.
  After Q-014, Q-008's provider prerequisites are fully satisfied.

## 1. Background

ADR-009 / Q-007 draw a hard line: **Action** is business response intent
produced by a Decision; **Execution** is the downstream fulfillment of an
Action through adapters (MT4/MT5 Manager, CRM, Kafka, Email) and is
**outside the Core Domain**. Q-008 §13 needs to reference "an available
Action outcome/execution-outcome reference" as ordinary case context,
while explicitly *not* owning the execution record and *not* claiming any
MT4/MT5/CRM/bridge/LP operation succeeded.

Q-013 delivered Action (the intent). Q-014 must deliver the record of
what **outcome** followed that intent — the reference Q-008 will attach to
a case as context.

Critical Phase 1 constraint: the repository has **no real MT4/MT5/CRM
execution adapter**, and `AGENTS.md` forbids inventing Manager API
interfaces without the real SDK. Every prior Core-Domain provider
(Q-009…Q-013) is therefore `MANUAL`-source, `HUMAN`-recorded only. Q-014
follows the same constraint: it records a **human-entered outcome fact**,
not an automated execution result. Real automated execution against
external systems remains a future, separately-approved Account Control /
Execution Requirement — Q-014 does not build, design, or reserve it.

## 2. Existing Capability and Gap Analysis

| Need | Existing capability | Gap |
| --- | --- | --- |
| A record of the outcome that followed an Action's intent | None | No `ActionOutcome` type, storage, or recording use case exists |
| The Action the outcome pertains to | Q-013 Action Provenance Foundation (`ActionRef`, narrow provenance-read contract) | None — reusable as-is |
| Actor identity/authorization for recording an outcome | Q-009 Trusted Actor Authorization | None — reusable as-is |
| A reference contract Q-008 can trust for case-associated outcome display | None | This is what Q-014 must deliver |
| Real automated execution against MT4/MT5/CRM and its attempt/retry/partial-failure semantics | None; forbidden without real SDK, and Execution is outside the Core Domain | **Out of scope** — a future Account Control Requirement |
| Structured success/failure result taxonomy | None; Decision's own outcome taxonomy was explicitly deferred (Q-008 §12) | **Out of scope for the Foundation** — see §5.3 |

## 3. Problem Statement

Q-008 cannot begin implementation until it can reference a real,
trustworthy ActionOutcome. Without Q-014, Q-008's outcome-reference case
context would have to fabricate an outcome or block indefinitely.

Q-014 must provide exactly enough ActionOutcome capability — recording a
human-entered outcome fact that pertains to one Action, and a trustworthy
read contract — for Q-008 to consume, without inventing execution
semantics, a success/failure taxonomy, or any adapter capability that
belongs to a future Execution Requirement.

## 4. Goals

1. Establish `ActionOutcome` as a real, persisted, immutable fact:
   identity, the Action it pertains to, a free-text outcome description,
   and authorship — attributable and immutable once recorded.
2. Provide a two-tier read contract (narrow in-process provenance
   confirmation + separate audited full-detail read) a future Q-008 can
   consume, matching the Evidence/Decision/Action pattern.
3. Require `ActorType.HUMAN` for recording — the only source this
   Requirement authorizes, since no automated execution producer exists.
4. Reuse Q-009 and Q-013 exactly as they exist; modify none of them.
5. Explicitly do not implement execution, execution attempts, retries,
   a result taxonomy, or any Account Control / external adapter.

## 5. Scope and Non-Goals

### 5.1 In Scope

- `ActionOutcomeRef` identity type (`aoc-<uuidv4>`, lowercase canonical
  form, matching the `ta-`/`ev-`/`dec-`/`act-` convention).
- Recording an ActionOutcome: exactly one pertaining `ActionRef`
  (validated as "recognized" via Q-013's existing narrow provenance-read
  contract — rejecting only a not-found reference), a free-text outcome
  description (bounded length, non-blank, matching the
  `ConclusionText`/`IntentText` validation pattern), the recording actor,
  and the record time.
- Idempotent recording: a replayed request with the same operation
  identity and unchanged content returns the original outcome without
  creating a duplicate row, matching Q-011…Q-013's operation-ledger
  pattern.
- A narrow, in-process `confirmProvenance`-style contract: given an
  `ActionOutcomeRef`, returns whether it is recognized, and if so, its
  pertaining `ActionRef`, recording actor, and record time — never the
  outcome description.
- A separate, audited full-detail HTTP read returning the complete
  content including the outcome description, requiring `action-outcome:read`,
  committing an access-audit record before content is returned.
- Real MySQL 8.4 migration and test coverage, matching Q-011…Q-013's
  rigor, including the dynamic-migration-count test discipline
  (`docs/lessons/2026-08-31-q011-migration-count-test-fix.md`).

### 5.2 Non-Goals (explicitly deferred)

- Any real automated execution against MT4/MT5/CRM/bridge/LP or any
  external system; any execution attempt, retry, timeout, partial-failure,
  or duplicate-execution handling. These belong to a future Account
  Control / Execution Requirement and require real SDKs (ADR-009,
  `AGENTS.md`).
- Any structured success/failure/partial result taxonomy or classification
  enum. The outcome is free text in the Foundation (§5.3).
- ActionOutcome correction or deletion. Immutable once recorded, by direct
  analogy to Decision (§5.3).
- Automated/`SERVICE`-sourced ActionOutcomes; Rule Engine.
- Any Q-008 code, wiring, or `RiskCase` type.
- An eligibility-style service for ActionOutcome itself (same reasoning as
  Q-012 §5.3 / Q-013 — no mutable per-reference state after recording).

### 5.3 Business-scope decisions requiring Product Owner confirmation

These three are genuine WHAT/scope questions (Decision Authority §16.2).
Each carries a firm recommendation grounded in the established pattern and
Phase 1 constraints; the Product Owner should confirm or override before
Architecture begins.

**(1) ActionOutcome is a human-recorded outcome *fact*, not a real
execution result.** Because Phase 1 has no execution adapter and inventing
one is forbidden, and because Execution is outside the Core Domain
(ADR-009), Q-014's Foundation records a `HUMAN`-entered outcome fact
about an Action. Real automated execution (and its attempt/retry/partial
-failure semantics) is deferred to a future Account Control / Execution
Requirement. **Recommend: confirm this scope.** This is the only shape
consistent with every prior module and with ADR-009's Action/Execution
separation. (If the business intends ActionOutcome to eventually *become*
the automated execution record, that is a future evolution, not a change
to this Foundation.)

**(2) Free-text outcome description, no structured result taxonomy now.**
Whether the business needs a structured `SUCCEEDED`/`FAILED`/`PARTIAL`/
`NO_OP` classification is a product decision. Decision's own outcome
taxonomy was explicitly deferred (Q-008 §12), and Q-008 only needs to
*reference* an outcome, not classify it, to function. **Recommend:
free-text outcome description for the Foundation; defer any classification
taxonomy to a future Requirement**, which can add it as an additive,
nullable column (non-breaking migration) — recorded as known deferred
technical debt with a clear migration path, not Unknown debt. Confirm, or
tell me the business needs a real taxonomy now (which would materially
change §7).

**(3) ActionOutcome → Action is many-to-one; no "at most one outcome per
Action" constraint in the Foundation.** Each ActionOutcome names exactly
one Action; an Action may accumulate more than one recorded outcome fact
over time. Enforcing "one outcome per Action" would be a business rule
about whether an Action's outcome can be re-recorded. **Recommend: do not
enforce a one-outcome-per-Action uniqueness constraint** (extensibility
-first; the constraint can be added later if the business requires it, but
cannot be cheaply removed once relied upon). Confirm, or tell me the
business requires exactly one outcome per Action.

Separately (a HOW default, not requiring business confirmation, but
flagged for transparency): **ActionOutcome is immutable once recorded** —
no correction/supersession use case — by direct analogy to Decision. A
future Requirement, not a silent change, would be needed to add mutability.

## 6. Domain Definitions

- **ActionOutcome** — an immutable, human-recorded fact describing the
  outcome that followed an Action's intent. Recorded once, immutable
  thereafter, pertaining to exactly one Action, authored by a `HUMAN`
  actor at a point in time, carrying a free-text outcome description. It
  is **not** an execution attempt, an execution record, or a claim that
  any external operation succeeded.
- **ActionOutcomeRef** — canonical identity, `aoc-<uuidv4>`.
- **ActionOutcomeSource** — enumerated set of how it was produced. This
  Requirement authorizes exactly one value: `MANUAL`.
- **Outcome description** — free-text content. Not a structured
  success/failure classification.

## 7. Functional Requirements

- **Q014-FR-001:** The system shall support recording an ActionOutcome
  given an actor context, exactly one `ActionRef`, and an outcome
  description.
- **Q014-FR-002:** Recording shall require `ActorType.HUMAN` and the
  `action-outcome:record` capability under Q-009's `AuthorizationGuard`.
- **Q014-FR-003:** Recording shall validate the pertaining `ActionRef` as
  "recognized" via Q-013's existing narrow provenance-read contract,
  rejecting only a not-found reference.
- **Q014-FR-004:** Recording shall be idempotent by operation identity and
  semantic content fingerprint, matching Q-011…Q-013's replay pattern.
- **Q014-FR-005:** An ActionOutcome, once recorded, shall never be
  corrected or deleted through any use case this Requirement authorizes.
- **Q014-FR-006:** The system shall provide a narrow, in-process
  provenance/confirmation read: given an `ActionOutcomeRef`, return whether
  it is recognized, and if so, its pertaining `ActionRef`, recording actor
  reference, and record time. This contract shall never return the outcome
  description. It requires `action-outcome:read`, no `ActorType`
  restriction.
- **Q014-FR-007:** The system shall provide a full-detail read (including
  the outcome description) requiring `action-outcome:read`, no `ActorType`
  restriction, committing an access-audit record before returning content;
  a failed audit write must prevent content disclosure.
- **Q014-FR-008:** No execution semantics, execution attempt/retry model,
  external adapter, or structured result taxonomy shall be introduced.

## 8. Security Requirements

- All use cases call Q-009's `AuthorizationGuard.requireAllowed` before
  any lookup or mutation. Default-deny; no implicit grant.
- Capabilities: `action-outcome:record`, `action-outcome:read`. No other
  capability is introduced.
- Recording requires `ActorType.HUMAN`; the read use cases permit any
  authorized `ActorType`, including `SERVICE`.
- The narrow contract must be structurally incapable of returning the
  outcome description (no such field on the return type).
- No outcome description, actor identity, or `ActionRef` may appear in logs
  or metric tags.

## 9. Data Integrity and Provenance Requirements

- Every ActionOutcome is permanently attributable to its recording actor,
  time, and pertaining Action; none of these may be altered after
  recording.
- Cross-module reference to `ActionRef`: a validated `CHAR` column, not a
  SQL foreign key to Q-013's table — matching the established cross-module
  pattern (ADR-014/ADR-015; Architecture confirms the exact shape).
- Full-detail access must be audited before content is returned, in a
  dedicated, non-database-read-only transaction.
- Idempotency: an exact replay never creates a second row; a changed
  replay under the same operation id is rejected, never silently applied.

## 10. Acceptance Criteria

1. `ActionOutcomeRef`/operation identity use the canonical lowercase
   UUIDv4 forms established by Q-010…Q-013.
2. Recording requires `HUMAN` and `action-outcome:record`; reads require
   only `action-outcome:read`, no `ActorType` restriction.
3. Recording accepts any recognized `ActionRef` and rejects only a
   not-found reference.
4. An ActionOutcome is immutable after recording — no correction or delete
   use case exists.
5. The narrow provenance read never returns the outcome description,
   structurally.
6. The full-detail read commits an access-audit record before returning
   the outcome description; a failed audit write returns no content.
7. Idempotent replay returns the original outcome without a new row; a
   changed replay under the same operation id is rejected.
8. No Q-008, execution, external-adapter, result-taxonomy, Alert, Rule
   Hit, or Rule Engine code exists anywhere in the change.
9. No existing Q-009/Q-010/Q-011/Q-012/Q-013 file is modified.
10. All mandatory tests, including real MySQL 8.4 integration/persistence/
    concurrency/security tests, pass; no mandatory test is skipped; the new
    module's migration test uses the dynamic-count discipline.

## 11. Technical Constraints

- Java 21 / Spring Boot modular monolith, `com.brokeros.risk.actionoutcome`
  package, matching the existing module layout convention.
- Additive Flyway migration only (next available version number); no
  existing migration modified.
- Reuse the single shared `Clock` bean
  (`SecurityModuleConfiguration.securityClock()`).
- MySQL-compatible constraints only; apply the `IS NOT NULL`-guarded CHECK
  pattern, and the dynamic-migration-count test pattern, both already
  documented in `docs/lessons/` and `docs/skills/`.

## 12. Dependencies

- Q-009 (`ActorContext`, `Capability`, `AuthorizationGuard`) — reused
  unchanged.
- Q-013 (`ActionRef`, its narrow provenance-read contract) — reused
  unchanged. Q-014 is the second consumer of Q-013's narrow contract (the
  first being the still-unbuilt Q-008).
- Does not depend on and does not implement any part of Q-008, Q-010,
  Q-011, or Q-012 directly.

## 13. Verification Plan

- Real disposable MySQL 8.4 migration, persistence, concurrency, and
  security integration tests, matching Q-012/Q-013's structure.
- Architecture tests proving no execution/adapter vocabulary and no result
  taxonomy exists in the module, and that the narrow contract cannot
  expose the outcome description.
- Full repository-wide real-MySQL gate (`Q009`…`Q014` all enabled) must
  pass with zero unexplained failures; the new migration test must use
  `flyway.info().pending().length`, not a hard-coded count.
- Static verification (`scripts/verify-static.sh`) extended with a
  Q-014-specific section mirroring the existing pattern.

## 14. Risks and Architecture Inputs

- Risk: a future Execution / Account Control Requirement will introduce
  real execution records; ActionOutcome must not be retrofitted into that
  role without its own Requirement/ADR. Anticipated, not prevented.
- Risk: if §5.3(2) is overridden (business needs a result taxonomy now) or
  §5.3(3) is overridden (one-outcome-per-Action required), §7 and §10
  change materially — must be resolved before Architecture.
- Architecture input: exact `action_outcome_ref` column length (`aoc-` is
  4 characters, so `CHAR(40)` by the same arithmetic Q-012/Q-013 used —
  Architecture should verify rather than assume).

## 15. Deliverables

- Approved `docs/requirements/Q-014-Action-Outcome-Provenance-Foundation.md`.
- Architecture document, ADR, and Implementation Design, following the
  same gate sequence.
- `com.brokeros.risk.actionoutcome` module implementation, additive
  migration, full test suite.
- Lessons Learned entries for any reusable pattern.
- Non-overwriting, timestamped review packages under `review/q-014/`.

## 16. Review Checklist

- [x] Requirement self-reviewed by Claude Code (external Architect role).
- [ ] Product Owner Gate Decision recorded (§17).
- [ ] §5.3's three business-scope questions answered (outcome-fact scope;
      no-taxonomy-now; many-to-one cardinality) before Architecture.
- [ ] No Q-008/Q-009/Q-010/Q-011/Q-012/Q-013 file referenced for
      modification.
- [ ] No execution/adapter vocabulary or result taxonomy introduced.

## 17. Current Gate

Q-014 Requirement status: **APPROVED — V1 — 2026-09-01 — Product Owner.**
Gate Decision: **PASS.** §5.3's three business-scope questions are all
confirmed as recommended: (1) ActionOutcome is a human-recorded outcome
*fact*, not a real execution result (real execution deferred to a future
Account Control / Execution Requirement); (2) free-text outcome
description, no structured result taxonomy in the Foundation (deferred as
known debt with an additive-nullable-column migration path); (3)
ActionOutcome → Action is many-to-one, no one-outcome-per-Action
constraint. ActionOutcome is immutable once recorded.

Under Decision Authority §16.5-B, the Architecture, ADR-016, and
Implementation Design were then drafted and self-reviewed as one connected
chain (bundle self-review:
`review/q-014/review-q-014-v2-architecture-adr-design-bundle-20260901-121500/`)
and presented together at the single retained implementation-authorization
gate. Documents:
`docs/architecture/q-014-action-outcome-provenance-foundation-architecture.md`,
`docs/adr/ADR-016-action-outcome-provenance-foundation.md`,
`docs/architecture/q-014-action-outcome-provenance-foundation-implementation-design.md`.

Q-014 Architecture V1 / ADR-016 / Implementation Design V1: **APPROVED
(bundle) — 2026-09-01 — Product Owner.** ADR-016 is **Accepted**.

Q-014 Implementation: **AUTHORIZED — 2026-09-01 — Product Owner**, against
Requirement V1 / Architecture V1 / ADR-016 (Accepted) / Implementation
Design V1.

Q-014 Implementation Allowed: **YES.**

Q-014 Implementation: **SUBMITTED — 2026-09-02 — Codex.** Package
`review/q-014/review-q-014-v3-implementation-20260902-013321/`. Codex's own
Gate Decision: BLOCKED — Q-014's 42 tests pass, but the full Q-009…Q-014
gate had 3 failures in existing Q-013 tests (`Q013MySqlMigrationTests` ×2,
`ActionRestContractTests`) whose over-broad prefix ownership (`LIKE
'action_%'`, `startsWith("ACTION_")`) wrongly captured Q-014's legitimate
`action_outcome_*` tables/FKs and `ACTION_OUTCOME_*` codes. Codex correctly
did not modify Q-013.

Q-014 Implementation Independent Review: **PASS — 2026-09-02 — Claude
Code.** Package
`review/q-014/review-q-014-v4-claude-code-independent-review-20260902-014900/`.
Verified by direct code inspection (recording-service §11.1 order, V7
migration: 3 tables, no status column, no join/history table, no
cross-module FK, `action_ref` not unique) and by independently executing
the full gate in Docker/Java 21/MySQL 8.4. **No defect found in Q-014's own
implementation.** The 3 failures were a Q-013 test-scoping defect (a second
class of cross-module test-ownership fragility, after hard-coded migration
counts); under Decision Authority §16.5-A the three assertions were made
precise (exclude the `action_outcome` namespace — coverage unchanged), and
the full Q-009…Q-014 real-MySQL gate then ran green: **246 tests, 0
failures, 0 errors, 0 skipped.** A repository-wide sweep found the same
broad-prefix pattern in Q-011/Q-012/Decision tests but it is not currently
colliding (no sibling nests under `evidence_`/`decision_` yet) and was left
unchanged; a naming/ownership convention is recommended (see the v4
package).

Q-014 Implementation: **ACCEPTED — 2026-09-02 — Product Owner** ("接受
Q-014 实现"), based on Codex's v3 implementation package and Claude Code's
v4 independent review (code inspection + independently executed full gate,
246/0/0). With Q-014 accepted, **Q-008's three provider prerequisites
(Decision, Action, ActionOutcome) are all satisfied.**

Q-014 Ready for Git Commit: **NOT YET** — acceptance of the implementation
is separate from staging/commit/push, which requires its own explicit
Product Owner instruction. The §16.5-A Q-013 namespace-scoping test fix is
in the working tree, to be committed together with Q-014 at that gate.

Q-014 Git Commit / Push: **NOT PERFORMED.**

Next gate: Product Owner decision on staging/commit (and separately, push).
