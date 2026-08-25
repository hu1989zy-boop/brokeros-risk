# ADR-010: Risk Case Aggregate, Lifecycle, and Audit Boundary

- Status: Accepted
- Date: 2026-08-24
- Architect approval date: 2026-08-24
- Approval origin: Explicit external Architect Review decision
- Requirement: Q-008
- Supersedes: None
- Depends on: ADR-009

This ADR was accepted from the explicit external Architect Review decision
recorded on 2026-08-24. Acceptance is not Codex self-approval and grants no
implementation permission.

## Context

ADR-009 establishes Decision as the Core Domain and Risk Case as an optional
downstream context that associates but does not own Evidence, Decision, or
Action. Q-008 needs a durable boundary for case lifecycle, assignment,
investigation history, reopening, and auditability.

Q-008 Requirement Review V1 proposed that every case require an existing
Decision. Architect Review V2 rejected that prerequisite because legitimate
manual suspicion may need an investigation case before formal Evidence is
complete or a Decision exists. The approved design supports manual intake
without making Risk Case the owner or producer of Evidence/Decision.

These concerns cross the ordinary field-modeling threshold. If state changes
are controlled only by services or database setters, invariants can diverge. If
Evidence, Decisions, Actions, comments, and audit records are loaded as one
unbounded object, the aggregate becomes an ownership and performance problem.
If audit is asynchronous best effort, a critical case change can exist without
its audit fact. A durable architecture decision is therefore needed before
implementation.

## Decision

### Risk Case aggregate

- Treat `RiskCase` as the Aggregate Root of the Risk Case Context inside the
  existing Phase 1 modular monolith.
- The aggregate owns only case invariants and case-owned state: case identity
  and number, primary subject reference, lifecycle status, operational
  priority, current assignment, association metadata, investigation notes, and
  resolution/cancellation/closure/reopen records.
- Evidence, Decision, and Action remain owned by their Q-007 contexts. Risk Case
  stores stable references and append-only association history; it cannot
  mutate or delete upstream objects.

### Intake model

- Support two first-class case intake sources: `MANUAL` and
  `DECISION_DRIVEN`.
- `MANUAL` intake may create a meaningful `OPEN` case without existing Evidence
  or Decision. The required intake reason/summary states why investigation
  starts; it is not Evidence, Decision, Rule Hit, or Alert.
- `DECISION_DRIVEN` intake requires an existing Decision, which remains
  attributable to Evidence under ADR-009.
- Use `DECISION_DRIVEN` rather than `AUTOMATED` because the former states a
  stable domain prerequisite while Rule Engine, stream detection, risk scoring,
  anomaly detection, or external alert mechanisms remain deferred.
- A valid case requires case number, `TRADING_ACCOUNT` subject reference,
  intake source, intake reason/summary, created-by actor, created-at UTC time,
  operational priority, and initial `OPEN` state.
- Manual intake changes chronology, not ownership. Risk Case is an operational
  investigation entry and never becomes the Core Domain or producer of a
  Decision.

### Controlled lifecycle

- Use the minimal states `OPEN`, `IN_REVIEW`, `ACTION_REQUIRED`, `RESOLVED`,
  `CLOSED`, and `CANCELLED` with the transition table in Q-008.
- Transition legality and invariants belong to named Risk Case domain
  operations. Controllers, repositories, adapters, and workflow engines do not
  decide whether a transition is legal.
- `ACTION_REQUIRED` means a Decision exists and business Action must be
  executed or arranged. It does not mean external execution succeeded, and not
  every case passes through it.
- `RESOLVED` records a substantive outcome, including confirmed-risk handling,
  no risk, false positive, monitoring only, or no action required. `CLOSED`
  records later administrative finalization.
- `CANCELLED` records that the case itself was invalid, duplicate, or created
  by mistake. It is terminal and is not equivalent to a `NO_RISK` resolution.
- Reopening `CLOSED` returns the case to `IN_REVIEW`, never `OPEN`, with
  mandatory reason, actor, UTC time, and Audit Record. Resuming a `RESOLVED`
  case follows the same history-preserving principle.
- Each return from `RESOLVED` or `CLOSED` to `IN_REVIEW` starts a new ordered
  Resolution Cycle. Prior Evidence, Decisions, Actions, resolutions, close
  facts, and audit history remain immutable.
- Represent completed cycles as immutable case-owned Resolution History
  records rather than one mutable resolution value. Exact relational
  persistence layout is deferred to Implementation Design.
- Do not introduce a workflow/state-machine framework or BPMN engine. Explicit
  domain behavior is sufficient for the approved foundation.

### Upstream associations

- A case can associate multiple Evidence, Decisions, and Actions over time.
- Evidence correction creates new upstream Evidence and an append-only case
  association. `SUPERSEDED`, `INVALIDATED`, or `WITHDRAWN` disposition belongs
  to the association/history and does not let Risk Case edit Evidence content.
- Decision reassessment creates a new Core Domain Decision; history is never
  silently overwritten.
- One current case-relevant Decision reference may support resolution, but all
  historical Decision associations remain retained.
- A Decision has at most one primary-case association in Q-008 Foundation.
  Related-case associations for cross-account scenarios remain architecturally
  possible but are deferred to avoid ownership, duplicate-Action, resolution,
  and audit ambiguity.
- `NO_ACTION` is a resolution/Decision outcome rather than an Action. External
  execution attempts/outcomes remain outside Action and Risk Case ownership.

### Subject and ownership references

- Use one typed primary subject reference rather than a universal entity model.
  The initial approved type is `TRADING_ACCOUNT`; each future Customer,
  IB/Agent, Order, Position, Device, IP, Payment, or Strategy type requires
  explicit semantics and mapping.
- Use opaque actor references for creation/assignment/history. The minimum
  Assignment consists of assignee, assigned-at, and assigned-by. Team ownership
  and queue semantics are explicitly deferred. Do not introduce IAM/RBAC or
  embed authentication tokens/display data as identity.

### Priority and case number

- Risk Case owns operational priority using `LOW`, `NORMAL`, `HIGH`, and
  `CRITICAL`. Severity/risk level remain part of Decision-owned risk assessment
  until explicitly distinguished by a future Core Domain decision.
- Use a globally unique immutable business case number separate from the
  internal `BIGINT id`. Prefer `RC-<opaque-high-entropy-identifier>` over a
  daily sequential counter that leaks volume and creates distributed
  contention. Define a CaseNumber Value Object and generator contract while
  deferring exact encoding.

### Audit boundary and consistency

- Keep Audit Record in an independent Audit capability. Do not load an
  unbounded audit collection inside the Risk Case aggregate.
- Case comments, resolution records, and association metadata remain case
  business data and are not substitutes for Audit Records.
- Every material case mutation must durably create its required Audit Record in
  the same application-owned database transaction as the case mutation.
  Kafka-only or best-effort audit is insufficient.
- The Phase 1 modular monolith coordinates this atomicity without distributed
  transaction, 2PC, Saga, Event Sourcing, a new service, or Kafka topic. Exact
  persistence/module contract requires separate Implementation Design.

### Sensitive investigation content

- Access to sensitive investigation content must be controlled; access and
  changes must be auditable; silent destructive deletion is prohibited.
- Exact retention periods, detailed permission matrices, legal hold,
  exceptional redaction workflow, and regulatory-retention implementation are
  deferred to later security/compliance Requirements and Implementation
  Design. These details do not block the Q-008 Foundation Architecture Gate.

## Alternatives

### Keep Risk Case as an anemic record controlled by services

Rejected because transition legality, assignment prerequisites,
resolution requirements, and reopening history could be bypassed by different
services or persistence callers.

### Make Risk Case own Evidence, Decisions, and Actions

Rejected because it conflicts with ADR-009, makes case management the system
center, and prevents upstream reasoning from existing independently of a case.

### Require an existing Decision for every case

Rejected by Architect Review V2 because it would block legitimate human
investigation and encourage operators to fabricate Evidence or Decisions.
Manual intake may precede formal reasoning while preserving Core Domain
ownership.

### Use `MANUAL` and `AUTOMATED` intake sources

Not selected for the foundation. `AUTOMATED` describes mechanism rather than
the stable prerequisite. `DECISION_DRIVEN` remains accurate whether the future
Decision came from a Rule Engine, stream detector, score, anomaly detector, or
human decisioning process.

### Store all history and audit as aggregate child collections

Rejected because audit and investigation history can grow without bound,
creating aggregate load, locking, retention, and ownership problems. The root
enforces the mutation while append-only records are persisted/queryable without
being an unbounded invariant-bearing collection.

### Use a workflow/BPMN/state-machine framework

Rejected for the foundation because the proposed state graph is small and
explicit. A framework would create irreversible dependency and operational
complexity without current evidence.

### Use event sourcing

Rejected because Q-008 needs auditable history, not replay-based aggregate
storage. Event sourcing would add event versioning, projection, migration,
ordering, replay, and operational requirements outside the current need.

### Persist audit asynchronously through Kafka only

Rejected because a case mutation could commit while publication or audit
consumption fails. Critical audit evidence must not be best effort. Kafka may
later distribute approved facts, but it is not the sole durability guarantee.

### Use a sequential daily case number

Rejected as the default because it exposes business volume, introduces a
distributed sequence/hotspot, and couples external identity to allocation
infrastructure. A date may be a display segment only if uniqueness remains
opaque and distributed-safe.

## Consequences

- Risk Case has a clear aggregate boundary without replacing Decision as Core
  Domain.
- Legitimate manual suspicion can start investigation without fabricated
  Evidence/Decision, while decision-driven intake remains explicit.
- Lifecycle rules become explicit, testable, and resistant to arbitrary state
  mutation.
- Evidence/Decision/Action history remains explainable and independently owned.
- Resolution and closure have separate audit semantics, while exceptional
  reopening preserves earlier cycles through immutable Resolution History.
- The aggregate stays bounded even as notes and audit history grow.
- Atomic case/audit persistence requires explicit transaction and failure
  design before implementation.
- Typed references reduce vendor coupling but require each new subject/actor
  type to define identity and mapping semantics.
- No workflow engine, event sourcing, Kafka topic, Redis key, microservice, IAM,
  or vendor execution capability is introduced.

## Implementation Details Deferred

The external Architect explicitly approved the architecture and the following
deferrals. They are not unresolved Architecture Gate questions:

1. The concrete opaque CaseNumber algorithm is deferred to Implementation
   Design; globally unique, immutable, non-volume-leaking identity plus the
   `CaseNumber` Value Object and `CaseNumberGenerator` contract are required.
2. The relational persistence layout for immutable case-owned Resolution
   History and ordered Resolution Cycles is deferred to Implementation Design.
3. Related/cross-case Decision associations are deferred; one Decision may have
   at most one Primary Risk Case association in the Foundation.
4. Team ownership and queue semantics are deferred; individual assignment is
   the Foundation scope.
5. Exact retention, detailed permissions, legal hold, exceptional redaction,
   and regulatory-retention implementation are deferred; controlled/auditable
   access and no silent destructive deletion remain mandatory.

## Status and Implementation Gate

ADR Required: **YES** because Q-008 proposes a durable aggregate boundary,
controlled workflow/state model, cross-capability ownership rules, and audit
consistency model.

ADR Accepted: **YES — explicit external Architect approval dated 2026-08-24**

Implementation Allowed: **NO**

The Architecture Gate is complete, but Q-008 Implementation Design and Design
Review have not started. Architecture acceptance does not authorize code,
schema, API, messaging, cache, UI, or integration work. A separate approved
prompt is required before implementation.
