# ADR-016: Action Outcome Provenance Foundation

- Status: **Accepted — 2026-09-01 — Product Owner** (accepted together
  with the Q-014 Architecture/Design bundle at the implementation
  -authorization gate, per Decision Authority §16.5-B)
- Date: 2026-09-01
- Approval origin: drafted under the §16.5-B connected-chain authorization
  granted by Requirement V1 approval (2026-09-01). Prepared by Claude
  Code, external Architect role. Self-review artifact.
- Requirement: Q-014 — Action Outcome Provenance Foundation V1, APPROVED —
  2026-09-01 — Product Owner
- Architecture: `docs/architecture/q-014-action-outcome-provenance-foundation-architecture.md`
  (see its own Document Status for the current version)
- Depends on: ADR-009 (core domain model, Action/Execution boundary),
  ADR-011 (Q-009 trust), ADR-015 (Q-013 Action provenance, and the
  cross-module no-hard-FK pattern reused here)
- Supersedes: None

## Context

Q-008 (Risk Case) cannot begin implementation until Decision, Action, and
ActionOutcome providers all exist (Q-008 §26). ADR-014 resolved Decision,
ADR-015 resolved Action; ActionOutcome is the last.

ADR-009 draws a hard line: Action is business intent; Execution is the
downstream fulfillment through adapters (MT4/MT5/CRM/…) and is **outside**
the Core Domain. Q-008 §13 needs to reference "an available Action
outcome/execution-outcome reference" as case context, while not owning the
execution record and not claiming any external operation succeeded. Phase
1 has no real execution adapter and `AGENTS.md` forbids inventing one.

Q-014 Requirement V1 (approved 2026-09-01, with three business-scope
questions confirmed by the Product Owner) resolves the boundary:
ActionOutcome records a `HUMAN`-entered outcome **fact** about one Action,
`MANUAL`-source only, with a free-text outcome description (no result
taxonomy), immutable once recorded, and a many-to-one relationship to
Action with no one-outcome-per-Action constraint. Real automated execution
is deferred to a future Account Control / Execution Requirement.

## Decision

### Bounded capability and identity ownership

Q-014 introduces `com.brokeros.risk.actionoutcome`, a module in the Phase 1
modular monolith. It owns `ActionOutcomeRef` identity
(`aoc-<canonical-lowercase-UUIDv4>`); bounded, immutable outcome-fact
content (one pertaining `ActionRef`, a free-text outcome description,
recording actor, record time); the recording use case and its idempotency
ledger; a narrow in-process provenance/confirmation read; and an audited
full-detail read. It owns no execution, execution attempt/retry, Account
Control adapter, or result taxonomy.

### An outcome *fact*, not an execution record

Per ADR-009's Action/Execution separation and the Phase 1 no-SDK
constraint, ActionOutcome is a human-recorded fact *about* an outcome, not
the execution record itself and not a claim that any external operation
occurred. This preserves ADR-009: the Core Domain records what a human
observed as the outcome; the real execution against external systems
remains a future, separately-approved capability outside the Core Domain.
This is not a scope reduction invented here — it is forced by ADR-009 plus
`AGENTS.md`, and confirmed by the Product Owner at the Requirement gate.

### No status column: ActionOutcome follows Decision's shape, not Action's

Action carries a `PROPOSED` status column (ADR-015) because Action has a
genuine proposed→approved lifecycle a future Requirement will extend. An
outcome fact has **no lifecycle** — once recorded, it does not transition.
Q-014 therefore follows **Decision's** shape (ADR-014: immutable, no status
column, no correction), not Action's. Adding a status column now would be
speculative schema for a lifecycle that does not exist. A future result
*taxonomy* (if ever required) is a separate additive nullable column, not
a lifecycle status — so nothing about the future taxonomy argues for a
status column now.

### Immutability, no correction

ActionOutcome is immutable once recorded — no correction, supersession, or
delete use case — by the same reasoning as Decision. Consequently no status
column, no supersession chain, and no `*_history` table exist. A future
Requirement, not a silent change, would be needed to add mutability.

### Free-text outcome, result taxonomy deferred as known debt

The outcome is free text. A structured `SUCCEEDED`/`FAILED`/`PARTIAL`/
`NO_OP` classification is deferred (matching Decision's own deferred
outcome taxonomy, Q-008 §12). This is recorded as **known** technical debt
with a clear migration path: a future Requirement adds a nullable
classification column (additive, non-breaking; old rows predate it and
need no backfill). Not Unknown debt (Principles §20).

### Pertaining-Action validation: reuse, not reimplementation

Recording validates the pertaining `ActionRef` via Q-013's existing narrow
provenance-read contract, actor's own context, accepting `RECOGNIZED` and
rejecting only `NOT_FOUND`. No Q-009…Q-013 file is modified. This is the
fourth use of the "recognized" reuse pattern across the Core-Domain chain
(Q-011→Q-010, Q-012→Q-011, Q-013→Q-012, now Q-014→Q-013).

### Cross-module reference: no hard foreign key

`action_outcome_record.action_ref` is a validated `CHAR(40)` value, no SQL
foreign key to Q-013's `action_record` — the pattern ADR-014/ADR-015
established. Validity enforced by the live application-layer call at write
time; each module's schema stays independently ownable.

### Many-to-one, no one-per-Action constraint

Each ActionOutcome names exactly one Action; an Action may accumulate more
than one recorded outcome fact over time. No uniqueness constraint on
`action_ref` — the extensibility-first choice the Product Owner confirmed
(a constraint can be added later but not cheaply removed once relied upon).

### Durable storage and schema

Three tables (additive migration): `action_outcome_record`,
`action_outcome_operation` (idempotency ledger), `action_outcome_access_log`
(full-detail-read audit). No join table (single reference), no history
table (no correction).

### Recording and read exposure; idempotency

One mutating use case (`POST /api/action-outcomes`). Narrow provenance
contract in-process only. Full-detail read over HTTP, `action-outcome:read`,
audit before disclosure, dedicated non-read-only transaction. Idempotent by
`operationId` + SHA-256 fingerprint; canonical order authorize → require
`HUMAN` → fingerprint → replay check → content validation → Q-013 call →
commit.

## Alternatives Considered

### Alternative 1: Model ActionOutcome as a real execution attempt/result

Rejected. There is no execution adapter in Phase 1 and inventing one is
forbidden; Execution is outside the Core Domain (ADR-009). Modelling a real
execution record now would either fabricate SDK semantics or blur the
Core-Domain boundary. Deferred to a future Account Control Requirement.

### Alternative 2: Give ActionOutcome a status column (mirror Action for consistency)

Rejected. An outcome fact has no lifecycle; a status column would be
speculative. Consistency with Action's *shape* is not a reason to model a
lifecycle that does not exist — ActionOutcome's correct analogue is
Decision (immutable, no status), and the self-review verified the analogy
applies rather than assuming it.

### Alternative 3: Add a structured result taxonomy now

Rejected for the Foundation. Q-008 needs only to *reference* an outcome,
not classify it; Decision's own taxonomy was deferred on the same basis.
Deferred as known debt with an additive-column migration path.

### Alternative 4: Enforce one outcome per Action (unique `action_ref`)

Rejected. Whether an Action's outcome may be re-recorded is a business rule
the Product Owner chose to leave open; a uniqueness constraint is easy to
add later and hard to remove once relied upon. Extensibility-first.

### Alternative 5: Cross-module hard FK for `action_ref`

Rejected, as in ADR-014/ADR-015 — would couple module schemas and resist
future reorganization.

## Consequences

Positive:

- Completes Q-008's provider prerequisites — after Q-014, Decision, Action,
  and ActionOutcome all exist, and Q-008 is unblocked on the provider side.
- Preserves ADR-009's Action/Execution boundary explicitly: the Core Domain
  records an outcome fact; real execution stays a future outside-Core
  capability.
- Fourth confirmation that the narrow-provenance reuse pattern generalizes
  cleanly across the whole Core-Domain chain.

Costs and constraints:

- A future Execution / Account Control Requirement must define real
  execution records without retrofitting ActionOutcome into that role
  absent its own Requirement/ADR.
- A future result-taxonomy Requirement adds a nullable classification
  column; until then the outcome is free text (known, documented debt).
- No cross-module hard FK means an application bug could theoretically
  reference an unvalidated `ActionRef` — mitigated by the write-time
  validation call, the same trust boundary ADR-014/ADR-015 accepted.

## Security Implications

`AuthorizationGuard.requireAllowed` before any lookup/mutation;
`action-outcome:record` requires `HUMAN` (checked before the replay
check); `action-outcome:read` no `ActorType` restriction. Narrow contract
cannot expose `outcomeText` structurally. No outcome text, actor identity,
or `ActionRef` in logs/metric tags.

## Data and Integrity Implications

Every ActionOutcome permanently attributable to actor, time, and pertaining
Action, none alterable after recording. `ON DELETE RESTRICT` on the one
intra-module FK (`action_outcome_operation.action_outcome_id`,
`action_outcome_access_log.action_outcome_id` → `action_outcome_record.id`).
No delete use case. Full-detail access audited before disclosure, in an
isolated transaction.

## Operational Implications

No new Kafka topic, Redis key, deployment manifest, or adapter. Reuses the
single shared `Clock` bean. Same disposable-MySQL-8.4 discipline, including
the dynamic-migration-count test pattern
(`docs/lessons/2026-08-31-q011-migration-count-test-fix.md`).

## Dependencies

Reuses Q-009 `ActorContext`/`AuthorizationGuard`/`Capability` and Q-013's
narrow provenance-read contract. Depends on ADR-009, ADR-011, ADR-015. No
new Maven dependency or external integration.

## Deferred Decisions

Implementation Design defines exact Java/SQL/endpoint/transaction/exception/
query/test mechanics without reopening this decision.

A future approved Requirement is required for: real automated execution /
Account Control adapter; a structured result taxonomy; a one-outcome-per
-Action constraint; any ActionOutcome correction/withdrawal; any additional
`ActionOutcomeSource` beyond `MANUAL`.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-09-01**, together
with acceptance of the Q-014 Architecture/Design bundle at the
implementation-authorization gate (Decision Authority §16.5-B). Drafting
under §16.5-B was not itself acceptance; this ADR governs from that
explicit acceptance. Implementation was authorized in the same decision
(see the Requirement §17 and the Architecture §24 / Implementation Design
§21 gate records).
