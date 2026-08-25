# Q-008: Risk Case Foundation

## Status

Approved — Implementation Design V4 Approved / Implementation Authorization Blocked by Prerequisites

| Gate | Result |
| --- | --- |
| Requirement | PASS / APPROVED |
| Architecture | PASS / APPROVED |
| Final Architecture Approval | PASS — external Architect decision recorded 2026-08-24 |
| ADR-010 | Accepted — external Architect approval |
| Implementation Design V4 | APPROVED — external Architect decision recorded 2026-08-25 |
| Implementation | NOT STARTED |
| Implementation Authorization | BLOCKED BY PREREQUISITES |
| Implementation Allowed | NO |

- Requirement ID: `Q-008`
- Architecture phase: Phase 1
- Change type: Risk Case domain foundation
- Approved date: 2026-08-24
- Governing core-domain decision: ADR-009
- Authoritative Risk Case decision: ADR-010
- Implementation authorization: BLOCKED BY PREREQUISITES

This Requirement was approved through an explicit external Architect decision
on 2026-08-24. The approval covers intake, lifecycle, domain boundaries, audit
consistency, reopening, and the explicitly deferred decisions recorded below;
it is not Codex self-approval. Architecture approval does not authorize Java,
API, schema, migration, Kafka, Redis, adapter, or other business
implementation. The external Architect approved the exact Q-008 Implementation
Design V4 on 2026-08-25, including its CaseNumber, immutable Resolution History,
aggregate, concurrency, reference, persistence, audit, lifecycle, API, and
ADR-009/ADR-010 compatibility decisions. No Implementation Design V5 is
required. Implementation authorization remains blocked by prerequisites and is
not inferred from Design approval.

## 1. Background

Q-007 established the authoritative BrokerOS Risk model:

```text
Evidence → Decision → Action → Risk Case
```

Decision is the Core Domain. Evidence is the traceable basis for an explainable
Decision. Action is business response intent and is not external execution.
Risk Case is an optional downstream context for investigation and
collaboration. ADR-009 prohibits Risk Case from owning or controlling Evidence,
Decision, Action, or decisioning.

Q-008 builds on that approved baseline. It defines how a case coordinates the
lifecycle of a risk investigation without turning case workflow into the Core
Domain or inventing upstream decision behavior.

ADR-009 defines domain dependency and ownership; it explicitly does not define
a universal runtime pipeline. Q-008 therefore supports two valid intake
chronologies without changing the Q-007 ownership model:

```text
Decision-driven intake:
Evidence → Decision → Risk Case → optional Action coordination

Manual investigation intake:
Manual suspicion → Risk Case → Evidence gathering → Decision → optional Action
```

In both paths, Evidence and Decision retain their Q-007 ownership, and every
Action still originates from a Decision. A manually opened Risk Case is an
operational investigation entry, not the entry point or owner of risk
reasoning.

## 2. Existing Capability / Gap Analysis

### 2.1 Already Exists

| Capability | Repository evidence | Q-008 treatment |
| --- | --- | --- |
| Core-domain language and ownership | Q-007 Requirement, Q-007 architecture design, ADR-009, and `brokeros-risk-core-domain` Skill | Reuse unchanged: Evidence → Decision → Action → Risk Case; Decision remains Core Domain |
| Phase 1 modular monolith | AGENTS.md, ADR-001, ADR-004, and Phase architecture | Keep one repository and one Spring Boot deployable; Risk Case is a logical capability, not a microservice |
| External-system isolation | ADR-002 and adapter placeholders | Keep CRM, MT4, MT5, bridge, LP, and other vendor behavior behind future approved adapters |
| Engineering contracts | `ApiResponse`, `GlobalExceptionHandler`, Bean Validation, Flyway, MySQL, UTC/enums/audit standards | Reuse if a later implementation is approved; create no duplicate foundation |
| Verification and review foundation | Q-004 CI scripts, current Review Package convention, development-standards Skill | Reuse for future implementation and use a dedicated Q-008 review directory now |
| Correlation and configuration foundations | ADR-007, ADR-008 and their implemented capabilities | Preserve; Request ID is not an actor or business identifier and configuration is not case state |

### 2.2 Need Improvement

- The accepted language says what Risk Case must not own, but it does not yet
  define the positive business meaning, lifecycle, invariants, ownership, or
  reopening semantics of a case.
- Existing audit standards define who/when/what/target/before/after/reason/source
  expectations, but no Risk Case mutation-to-audit consistency boundary exists.
- No controlled subject-reference convention exists for associating a case
  without leaking CRM, broker, MT4, or MT5 schema into the domain.
- No case-number, assignment, priority, comment, resolution, cancellation, or
  concurrency contract exists.
- Evidence, Decision, and Action can be conceptually associated with Risk Case,
  but append, invalidation, reassessment, supersession, and history semantics
  are unspecified.

### 2.3 Need New Design

- Risk Case aggregate boundary and case-owned invariants.
- Minimal controlled case lifecycle and transition policy.
- Typed, extensible subject references without a universal entity framework.
- Append-only association history for Evidence, Decision, and Action.
- Minimal ownership/assignment model using actor/team references without IAM.
- Case priority semantics and the boundary with Decision-owned risk assessment.
- Public case-number contract separate from the database primary key.
- Strong audit boundary and consistency expectations without building a full
  Audit platform.

### 2.4 Conflict / Blocker

Architect Review V2 resolves the V1 intake ambiguity. Absence of a Decision
must not block legitimate manual investigation intake, and operators must not
fabricate Evidence, a Decision, Rule Hit, or Alert to open a case. This does not
conflict with ADR-009 because the manually opened case neither owns nor creates
the later Evidence/Decision; those remain in the Core Domain boundary.

No incompatible conflict or unresolved Architecture or Design Gate blocker
remains. The external Architect approved Implementation Design V4 on
2026-08-25. Implementation remains blocked because authoritative upstream
reference providers—including the typed Trading Account subject authority—are
absent, and no authenticated Actor/authorization provider exists.

## 3. Problem Statement

BrokerOS Risk has no durable business object that coordinates investigation of
an identified risk issue across responsible people, evolving Evidence,
reassessment, response intent, resolution, and closure. Without a defined Risk
Case foundation, future work could overwrite evidence, keep only the latest
decision, treat action intent as successful execution, permit arbitrary status
changes, lose assignment history, use database IDs as case numbers, or build an
unbounded case object containing audit and vendor data.

Q-008 must establish the smallest coherent case model that is auditable and
extensible while keeping Decision as the Core Domain.

## 4. Goals

1. Define Risk Case and its boundary from Evidence, Decision, Action, Alert,
   Rule Hit, and Audit Record.
2. Define a minimal controlled lifecycle with resolution, closure,
   cancellation, and exceptional reopening semantics.
3. Define broker-neutral subject, assignment, priority, case-number, comment,
   and investigation-history contracts.
4. Preserve all associated Evidence and Decision history without silent
   overwrite or physical deletion.
5. Keep Action intent separate from execution and establish the future Account
   Control boundary.
6. Require a complete, immutable audit trail for critical case changes.
7. Preserve the Phase 1 modular monolith, ADR-009, and existing engineering
   foundations.

## 5. Scope

### 5.1 Current Requirement phase

Requirement Discovery, Architecture approval, ADR-010 acceptance,
Implementation Design V4, and external Architect Design approval are complete
and are not repeated. The current phase is Architecture Governance and
Implementation Prerequisite Analysis. It does not authorize implementation or
create an Implementation Design V5.

### 5.2 Approved capability boundary

Q-008 governs the architecture of a minimal Risk Case capability that can:

- open a case through `MANUAL` or `DECISION_DRIVEN` intake;
- require a meaningful intake summary without misclassifying it as Evidence or
  a Decision;
- associate one primary typed risk subject;
- associate multiple Evidence, Decision, and Action references without taking
  ownership of those upstream objects;
- assign a case to an individual while reserving team ownership as a future
  extension;
- control status changes through named domain operations;
- record append-only investigation notes, resolution, cancellation, and reopen
  reasons;
- expose a stable case number distinct from internal persistence identity; and
- create auditable records for every material mutation.

Exact Java packages, API endpoints, persistence schema, transaction boundary,
security boundary, and verification strategy are approved in
`docs/architecture/q-008-risk-case-foundation-implementation-design.md` as
Implementation Design V4. Their approval does not satisfy or waive the
implementation prerequisites.

## 6. Non Goals

Q-008 does not authorize:

- Rule Engine, rule language, rule administration, scoring, streaming, or
  scheduling;
- Flink, Python, Python ML, AI decisioning, or AI-generated Evidence;
- MT4 Manager API, MT5 Manager API, Bridge API, LP API, dealer plugin, CRM
  writeback, or another real external integration;
- automatic trading intervention or direct execution of a Risk Action;
- frontend UI, notification system, workflow engine, BPMN, or orchestration
  framework;
- full IAM/RBAC, authentication design, organization directory, or permission
  administration;
- complex SLA, escalation, on-call, calendar, or shift-management capability;
- case search engine, Elasticsearch, reporting platform, dashboard, or data
  warehouse;
- event sourcing, a universal Entity/Subject framework, free-form `OTHER`
  subject semantics, or one table per possible external subject;
- case merge/split, bulk case operations, automated duplicate detection,
  appeal workflow, legal hold, or retention/purge implementation;
- evidence binary/blob storage, file upload, document management, or KYC
  document storage;
- Kafka topic/event, Redis business key, or separate Risk Case service;
- a full Audit module or independently deployed audit platform.

## 7. Domain Definitions

### 7.1 Risk Case

A **Risk Case** is a durable, auditable work record in the downstream Risk Case
capability that coordinates investigation and disposition of one identified
risk concern for one primary typed subject. A manual case may exist before
formal Evidence or a Decision, while a decision-driven case starts with an
existing Decision. In either path, the case holds case-owned lifecycle state
and references; it does not create, own, or rewrite Evidence, Decisions, or
Actions.

Risk Case is the approved **Aggregate Root** for the Risk Case Context because
case status, assignment, priority, resolution, cancellation, reopening, notes,
and association changes must satisfy invariants atomically and may not be
mutated independently. The aggregate boundary must remain bounded: it owns
references and case-owned records, not an unbounded in-memory audit log or
upstream domain objects.

### 7.2 Case Intake Source

**Case Intake Source** identifies how investigation work entered the Risk Case
capability. Q-008 defines two stable values:

- `MANUAL` — an authorized human starts investigation from a legitimate
  suspicion or concern before a formal Decision is required;
- `DECISION_DRIVEN` — an existing Core Domain Decision causes or supports case
  creation.

`DECISION_DRIVEN` is preferred over a generic `AUTOMATED` value because it
states the domain prerequisite rather than the current mechanism. Future Rule
Engine, stream detection, scoring, anomaly detection, or external-alert
mechanisms may supply Decisions without expanding the Q-008 enum prematurely.
Intake source is metadata about case creation; it is not Evidence, Decision,
Rule Hit, or Alert.

### 7.3 Intake Reason / Summary

An **Intake Reason / Summary** is the concise case-owned explanation of why an
investigation was started. It is mandatory for both intake paths. For manual
intake it records the suspicion reported by the actor; it does not assert that
the suspicion is true and is not Evidence or a Decision.

### 7.4 Evidence

**Evidence** is traceable information supporting or refuting a risk conclusion.
It is owned by the Decision capability established by Q-007. Risk Case stores
an association and provenance needed to explain why Evidence was attached; it
does not edit Evidence content.

### 7.5 Decision

**Decision** is the Core Domain’s explainable risk conclusion derived from
Evidence. A case may associate a history of Decisions and identify the current
case-relevant Decision reference, but Risk Case cannot produce, approve,
overwrite, or delete a Decision.

### 7.6 Action

**Action** is business response intent originating from a Decision. Case work
may propose or associate Actions, but external execution requests, attempts,
and outcomes remain separate downstream concepts.

### 7.7 Alert

An **Alert** is a delivery or attention signal indicating that something may
need review. It can lead approved upstream logic or a human to create Evidence
and a Decision, but it is not Evidence, a Decision, or a Risk Case and is not
implemented by Q-008.

### 7.8 Rule Hit

A **Rule Hit** is the traceable fact that a specific version of a Rule matched
input. It may contribute to Evidence and Decision provenance. It is not a case
status, Action, or audit record, and Q-008 does not implement a Rule Engine.

### 7.9 Audit Record

An **Audit Record** is an immutable compliance/operational fact describing a
material change: actor, time, operation, target, before/after state, reason,
source, and correlation context. It is not a case comment or the Risk Case
aggregate itself. Audit is an independent capability coordinated
atomically with case mutations.

### 7.10 Case Comment / Investigation Note

A **Case Comment** is case-owned investigation content written by an actor. It
is a business record, not a substitute for an Audit Record. Comments are
append-only; correction or permitted redaction must retain auditable history.

### 7.11 Resolution and Closure

**Resolution** records the substantive outcome of the risk issue and the
current Decision supporting it. **Closure** is the later administrative
finalization of a resolved case. Keeping them separate supports quality review
and exceptional reopening without erasing the earlier resolution.

### 7.12 Resolution Cycle

A **Resolution Cycle** is one ordered investigation-to-resolution period within
the same case. The first investigation is cycle 1. Returning a resolved or
closed case to `IN_REVIEW` starts the next cycle. Each completed cycle produces
an immutable case-owned Resolution Record, so a later resolution never
overwrites an earlier Decision, Evidence set, Action references, outcome,
actor, reason, or timestamps.

## 8. Functional Requirements

- **Q008-FR-001:** The system shall treat Risk Case as the only aggregate root
  for case-owned lifecycle mutations.
- **Q008-FR-002:** Every case shall have an immutable internal identity and a
  separate immutable, globally unique business case number.
- **Q008-FR-003:** Every case shall have exactly one primary typed subject
  reference. The initial supported subject type is `TRADING_ACCOUNT`;
  additional types require an approved extension.
- **Q008-FR-004:** Every case shall declare exactly one intake source:
  `MANUAL` or `DECISION_DRIVEN`.
- **Q008-FR-005:** A valid new case shall contain case number, primary subject,
  intake source, intake reason/summary, created-by actor reference, created-at
  UTC time, operational priority, and initial status `OPEN`.
- **Q008-FR-006:** `MANUAL` intake shall not require existing Evidence or a
  Decision and shall not fabricate either. `DECISION_DRIVEN` intake shall
  reference its existing Decision; that Decision remains attributable to
  Evidence under ADR-009.
- **Q008-FR-007:** A case shall support multiple historical Evidence, Decision,
  and Action associations while preserving upstream ownership.
- **Q008-FR-008:** A case shall retain current status, current operational
  priority, current assignment, creation provenance, and concurrency version.
- **Q008-FR-009:** Status, assignment, priority, subject association, Evidence,
  Decision, Action, comment, resolution, cancellation, close, and reopen
  changes shall occur through named use cases/domain operations, not arbitrary
  setters.
- **Q008-FR-010:** A case shall preserve every completed resolution/closure
  cycle after reopening; a new cycle shall not overwrite the previous one.
- **Q008-FR-011:** A case shall never be physically deleted through normal
  business operation.
- **Q008-FR-012:** A case shall reject stale concurrent mutations rather than
  silently overwriting another reviewer’s changes.

## 9. Lifecycle Requirements

### 9.1 Approved lifecycle direction

| State | Business meaning |
| --- | --- |
| `OPEN` | Case has been recorded and awaits ownership/triage. It is active but not yet under an accountable review assignment. |
| `IN_REVIEW` | An assigned actor is investigating, gathering Evidence, linking or reassessing Decisions, or conducting human review. |
| `ACTION_REQUIRED` | Sufficient Decision context exists and one or more Risk Actions must be executed or arranged. This is case coordination state, not proof of Action Execution. |
| `RESOLVED` | The case has a clear business outcome, such as confirmed risk with action completed, no risk found, false positive, monitoring only, or no action required. It is not yet administratively closed. |
| `CLOSED` | A resolved case has been administratively finalized. Normal mutations stop; only an explicit exceptional reopen may reactivate it. |
| `CANCELLED` | The case itself was invalid, duplicate, or created by mistake. It is retained for audit and is terminal; it is not a `NO_RISK` investigation outcome. |

`RESOLVED` and `CLOSED` remain distinct because risk conclusion and
administrative finalization are different acts, actors, and audit facts.
`CANCELLED` is not an alternative risk outcome and must require a reason. Not
every case passes through `ACTION_REQUIRED`; for example, an investigation may
move from `IN_REVIEW` directly to `RESOLVED` with a `NO_RISK`, false-positive,
monitoring-only, or no-action-required outcome.

### 9.2 Approved transitions

| From | Allowed target | Required business condition |
| --- | --- | --- |
| `OPEN` | `IN_REVIEW` | An assignee exists and formal investigation begins |
| `OPEN` | `CANCELLED` | Cancellation reason and actor are recorded; duplicate target is referenced when applicable |
| `IN_REVIEW` | `ACTION_REQUIRED` | A current Decision is linked and identifies required Action intent |
| `IN_REVIEW` | `RESOLVED` | A current Decision and immutable Resolution Record are captured; no Action is required or remaining |
| `IN_REVIEW` | `CANCELLED` | Invalid/duplicate/out-of-scope reason and actor are recorded |
| `ACTION_REQUIRED` | `IN_REVIEW` | New Evidence or changed circumstances require a new/reassessed Decision |
| `ACTION_REQUIRED` | `RESOLVED` | Required Action outcome/reference and immutable Resolution Record are captured |
| `ACTION_REQUIRED` | `CANCELLED` | The case itself is proven duplicate/invalid and the exceptional reason is recorded |
| `RESOLVED` | `CLOSED` | Administrative close actor, time, and reason are recorded |
| `RESOLVED` | `IN_REVIEW` | Review resumes with mandatory reason/actor/time; a new resolution cycle starts and prior resolution remains immutable |
| `CLOSED` | `IN_REVIEW` | Exceptional reopen records mandatory reason/actor/time/audit; a new resolution cycle starts and all prior history remains immutable |

No transition is allowed from `CANCELLED`. No arbitrary jump or generic
`setStatus` operation is allowed. A future authorization Requirement may
further restrict who can resolve, close, cancel, or reopen; Q-008 does not
invent RBAC.

### 9.3 Transition ownership

Transition legality and invariants belong to the Risk Case aggregate. The
application layer may authorize/orchestrate the use case and persist the
result, but a controller, repository, workflow engine, or external adapter may
not decide transition validity.

### 9.4 Reopen and resolution history

- Reopen targets `IN_REVIEW`, never `OPEN`, because the case already has
  investigation context.
- Reopen requires reason, actor, UTC timestamp, and immutable Audit Record.
- Historical resolutions, Decisions, Evidence references, Action references,
  close facts, and audit history remain preserved.
- Each return from `RESOLVED` or `CLOSED` to `IN_REVIEW` starts a new ordered
  Resolution Cycle.
- The approved domain shape is an immutable case-owned Resolution History
  record per cycle rather than one mutable resolution value. This
  preserves each outcome without requiring Event Sourcing or loading Audit as
  aggregate state.
- Future persistence must durably store ordered case-scoped cycle identity and
  immutable Resolution Records. The exact relational layout is deferred to
  Implementation Design.

## 10. Risk Subject Requirements

- Model a subject as a small typed reference, not a polymorphic universal
  entity. Its contract shall contain a controlled `subjectType` plus a stable
  opaque reference resolvable by the owning context/adapter.
- Q-008 supports `TRADING_ACCOUNT` as the only initial primary type because it
  is broker-neutral and directly relevant to Forex/CFD risk. `CUSTOMER`,
  `IB_AGENT`, `ORDER`, `POSITION`, `DEVICE`, `IP_ADDRESS`, `PAYMENT`, and
  `TRADING_STRATEGY` are extension candidates, not approved initial types.
- Do not use a free-form `OTHER` type. A new type requires explicit semantics,
  ownership, identity, security classification, and adapter mapping.
- External CRM, MT4, MT5, broker, or vendor DTOs/primary keys shall not become
  Risk Case domain types. Adapters translate them to the approved reference.
- Related/multiple subjects, subject snapshots, and subject-change rules remain
  future requirements. The foundation uses one immutable primary subject.

## 11. Evidence Requirements

- One Risk Case may associate multiple Evidence references.
- Adding Evidence creates a new append-only association containing attribution
  of who/what attached it, when, source, and reason.
- Risk Case never edits or silently replaces Evidence content.
- Evidence correction creates new upstream Evidence and a new association. The
  prior case association uses an explicit disposition such as `SUPERSEDED`,
  `INVALIDATED`, or `WITHDRAWN`, with reason, actor/source, timestamp, and
  replacement reference where applicable; the Evidence itself remains owned
  upstream and history remains visible.
- Normal business operations shall not physically delete Evidence or its case
  association. A future legal/security redaction process must retain an audit
  tombstone and requires its own approved policy.
- Invalid Evidence remains part of the historical basis so reviewers can
  explain why an earlier Decision changed.

## 12. Decision Requirements

- A Risk Case may associate multiple Decisions over time and shall preserve
  Decision history.
- A manual case may initially have no Decision. Before it enters
  `ACTION_REQUIRED` or `RESOLVED`, it must link a formal Decision produced by
  the Core Domain.
- Reassessment creates a new Decision in the Core Domain; it never overwrites
  or mutates the historical Decision through Risk Case.
- The case may identify one current case-relevant Decision reference. Changing
  that reference is auditable and does not change the Decision itself.
- Automated and manual Decisions coexist as separate upstream Decision records
  with their own source, actor/mechanism, Evidence provenance, and time. Risk
  Case must not collapse them into one mutable field.
- A future Rule Engine evaluates versioned Rules and Evidence to produce a
  Decision. A later decision-driven intake policy may use that Decision to
  request a case, but the Rule Engine shall not bypass Decision by directly
  creating a case or Action.
- By default, a formal Decision has at most one **primary case association**.
  A case may still link multiple successive Decisions as investigation evolves.
  Cross-account or other exceptional **related case associations** are
  architecturally possible but deferred from Q-008 Foundation to avoid
  ambiguous ownership, duplicate Actions, inconsistent resolutions, and audit
  confusion.
- Decision outcome taxonomy, confidence, rule metadata, approval semantics,
  and human override policy are outside Q-008 and require Core Domain design.

## 13. Action Requirements

- A Risk Case may associate zero or more Actions. Every associated Action must
  originate from an associated Decision.
- Case coordination may record proposed/approved Action references and an
  available Action outcome/execution-outcome reference. Risk Case does not own
  the Action lifecycle or the execution record.
- Action remains business intent. Case status and action association shall not
  claim that an MT4/MT5/CRM/bridge/LP operation succeeded.
- Examples such as restricting trading, changing leverage, disabling
  withdrawal, read-only mode, routing change, and monitoring are possible
  Action intents only; Q-008 defines no supported vendor operation.
- `NO_ACTION` is a Decision/resolution outcome, not an Action,
  because absence of intent is not an executable business response.
- Manual investigation is primarily case work/follow-up, not an external
  Action, unless a later approved Action Requirement gives it explicit intent
  semantics.
- Future Account Control shall receive an authorized execution request derived
  from an Action through an adapter boundary. That future contract must define
  authorization, target, idempotency, timeout, bounded selective retry,
  duplicate handling, partial failure, attempt/outcome, and audit semantics.
- Execution attempts/outcomes may later be referenced by the case for context,
  but they cannot rewrite the originating Decision or Action history.

## 14. Assignment Requirements

- `createdBy` is mandatory and immutable. It is an actor reference, not a
  display name or authentication token.
- `assignee`, `assignedAt`, and `assignedBy` form the minimum Assignment model.
  They may be absent while a case is `OPEN`; an assignee shall exist before
  entering `IN_REVIEW` or `ACTION_REQUIRED`.
- Q-008 Foundation adopts individual assignment only. Team ownership and queue
  semantics are explicitly deferred. Q-008 does not define team membership or
  organization hierarchy.
- Assignment and unassignment require actor, time, reason/source, before/after
  values, and audit record.
- `reviewedBy`, `resolvedBy`, `closedBy`, and `reopenedBy` shall be retained in
  the corresponding append-only history/audit fact rather than as freely
  mutable top-level fields. Derived read models may expose them later.
- Actor/team references are integration boundaries only. Q-008 does not create
  users, teams, authentication, IAM, or RBAC.

## 15. Priority, Severity, and Risk Level

- **Priority** is case-owned operational urgency and ordering. It may change as
  workload or exposure changes, with an audited reason.
- **Severity** and **Risk Level** both describe the assessed business risk and
  therefore overlap. They belong with the current Decision unless a future
  Core Domain Requirement establishes distinct semantics.
- Q-008 defines one case-owned `priority` using stable codes `LOW`, `NORMAL`,
  `HIGH`, and `CRITICAL`, and no independently mutable case
  `severity` or `riskLevel` field. The case may display Decision-owned risk
  assessment without copying it as a second source of truth.
- Priority codes are not severity codes and imply no SLA/escalation behavior.
  Enum ordinals and free-form priority strings are prohibited.

## 16. Case Number Requirements

- Every case shall have a separate immutable business number; the internal
  `BIGINT id` shall never be exposed as the case number.
- Model the identifier contract as a `CaseNumber` Value Object supplied by a
  `CaseNumberGenerator` boundary. This statement defines domain responsibility,
  not a Java type or implementation authorization.
- The business number shall be globally unique across application instances,
  case-insensitive where applicable, non-semantic beyond a stable `RC-`
  namespace, and safe to expose in API/log/audit references.
- A daily sequential suffix such as `RC-20260824-000001` is not recommended
  because it reveals daily case volume and creates distributed allocation and
  hot-sequence concerns.
- The preferred shape is `RC-<opaque-high-entropy-identifier>`. The concrete
  UUID/ULID/random Base32 strategy, length, checksum, and display grouping are
  deferred. If a date is included for readability, the suffix must remain
  non-sequential and collision-safe; date is not a uniqueness boundary.
- Case-number generation shall be idempotent for one creation request and
  protected by a database uniqueness constraint in any future persistence
  design.

## 17. Audit Requirements

The following changes must produce immutable audit records:

- case creation;
- assignment/team/assignee change;
- priority change;
- status transition;
- subject association establishment;
- Evidence addition, supersession, or invalidation;
- Decision association/current-reference change;
- Action proposal, approval/state-reference change, association, and any later
  execution-result association;
- comment creation, correction, or authorized redaction;
- resolution, close, cancel, and reopen.

Every audit record shall capture, where applicable: event/record identity,
actor, occurred time in UTC, operation, case target, affected child/reference,
before/after state, reason, source, and request/correlation identifiers. Request
ID and Trace ID provide correlation only and cannot serve as actor identity.

Audit Record is an independent append-only/immutable Audit capability, not an unbounded
child collection loaded inside the Risk Case aggregate. Case-owned records such
as comments and resolutions remain business data. A successful case mutation
and its required audit record must be committed in the same application-owned
database transaction in the Phase 1 modular monolith. Kafka-only best-effort
audit is not sufficient. Q-008 introduces no distributed transaction, 2PC,
Saga, Event Sourcing, Audit module, topic, or event contract in this phase.

## 18. Data Integrity Requirements

- Future application-owned schema changes use new immutable Flyway migrations,
  `snake_case`, `BIGINT id`, and separate business identifiers.
- All persisted timestamps use UTC. External broker/server/client time
  conversion is explicit and outside case status semantics.
- Finite states/types use stable readable codes, never Java enum ordinals.
- Optimistic concurrency or an equivalently explicit strategy shall prevent
  lost updates to assignment, priority, status, and current associations.
- Case, resolution-cycle, close, cancel, reopen, comment, and association
  history is append-only. Corrections supersede; they do not silently overwrite
  history.
- Resolution history shall persist an ordered immutable Resolution Record per
  completed cycle. A current-cycle indicator may support invariant checks, but
  it cannot replace historical records.
- Upstream references shall preserve their identifier and provenance even when
  the upstream object later becomes inactive or archived.
- A case mutation that requires audit shall not commit without its audit record.
- Redis shall not be the durable source of truth for cases, associations,
  history, or audit.
- Access to sensitive investigation content must be controlled; access and
  changes must be auditable; silent destructive deletion is prohibited.
- Exact retention periods, detailed permission matrices, legal hold,
  exceptional redaction workflow, and regulatory-retention implementation are
  deferred to later security/compliance Requirements and Implementation
  Design. Those details do not block the Q-008 Foundation Architecture Gate.

## 19. Acceptance Criteria

### 19.1 Requirement and design gate

1. The Existing Capability / Gap Analysis is based on repository evidence and
   does not claim that a Risk Case business module already exists.
2. The Requirement preserves ADR-009 exactly: Decision remains Core Domain;
   Risk Case remains an optional/downstream capability; manual intake changes
   chronology only, and Evidence/Decision/Action ownership does not move into
   the case.
3. Risk Case, Evidence, Decision, Action, Alert, Rule Hit, Audit Record, and Case
   Comment have non-overlapping definitions.
4. Both `MANUAL` and `DECISION_DRIVEN` intake are explicit; manual intake does
   not require or fabricate Evidence, Decision, Rule Hit, or Alert.
5. Aggregate ownership, lifecycle, subject, associations, assignment,
   priority, case number, audit, and data-integrity decisions are explicit.
6. ADR need is recorded as `YES`, and ADR-010 is Accepted based on explicit
   external Architect approval dated 2026-08-24.
7. All former open questions are resolved or explicitly accepted as deferred;
   no unresolved Architecture Gate blocker remains.
8. The dedicated Q-008 V3 Architecture Approved Review Package contains all
   mandatory files and a self-contained ZIP with approved Requirement and
   Accepted ADR-010 snapshots.
9. No business implementation, commit, or push occurs during this gate.

### 19.2 Future behavior gate after separate authorization

10. Case state changes are accepted only through the approved transition table
   and named domain operations, including tests for valid, invalid, repeated,
   and stale concurrent transitions.
11. Evidence, Decision, and Action associations preserve ownership and
    append-only history, with reassessment/supersession rather than overwrite.
12. Assignment, comments, resolution cycles, cancellation, close, and reopen retain
    actors, reasons, UTC times, and complete history.
13. The case number is unique, immutable, independent of the internal primary
    key, and safe for distributed generation without sequential volume leakage.
14. Priority is operational; Decision-owned risk assessment is not duplicated
    as mutable case severity/risk level.
15. Every required mutation creates its audit record in the same
    application-owned database transaction as the case change.
16. No external trading/account operation is executed or claimed successful by
    the Risk Case capability.
17. API, database, security, integration, and operational behavior—if later
    authorized—complies with AGENTS.md and accepted ADRs and passes the full
    Review Package gate.

## 20. Technical Constraints

- Preserve Java 21, Spring Boot 3.x, Maven, MySQL, Redis, Kafka, Docker, and
  Kubernetes as the approved stack; do not introduce Flink or Python.
- Preserve one repository, one Spring Boot deployable, and feature-first
  modular-monolith boundaries.
- Keep the root package `com.brokeros.risk`; exact Risk Case package/module
  design requires approval before creation.
- Keep external-system types and operations behind adapters and never write to
  an external database.
- Use Flyway exclusively for future application-owned schema changes.
- Reuse `ApiResponse`, validation, exception, OpenAPI, tracing, logging, and
  configuration foundations; do not create parallel contracts.
- Create no Kafka topic/event or Redis key without an approved Requirement and
  architecture decision including consistency, versioning, TTL/invalidation,
  and source-of-truth semantics.
- Never expose secrets, authentication material, KYC documents, or sensitive
  personal-document content in cases, logs, errors, or Review evidence.
- Do not invent vendor Manager API behavior or a generic workflow/state-machine
  framework.

## 21. Deliverables

Current authorized deliverables:

- approved `docs/requirements/Q-008-Requirement.md` with current Design and
  Implementation Authorization gates;
- unchanged Accepted `docs/adr/ADR-010-risk-case-foundation.md`;
- formal Implementation Design under `docs/architecture/`;
- dedicated V4 Review Package without changing V1, V2, or V3;
- self-contained timestamped V4 ZIP containing Requirement, ADR-010, Design,
  and all V4 Review files;
- independent Architect Approval and Prerequisite Analysis Review Package and
  timestamped ZIP without modifying the approved V4 artifact; and
- explicit prerequisite decisions, recommendation, and Implementation Gate.

Not current deliverables: Java, tests, API, entity, repository, service,
controller, migration, table, Kafka event/topic, Redis key, adapter, UI, Git
commit, or push.

## 22. Verification Plan

### 22.1 Current governance-analysis verification

- Verify the formal Implementation Design covers domain, lifecycle,
  persistence, transaction/audit, use cases, API, security, concurrency, and
  future test strategy without changing approved architecture.
- Verify ADR-009/ADR-010 and V1/V2/V3 Reviews remain unchanged.
- Verify manual intake has no Decision prerequisite and does not fabricate
  Evidence/Decision/Rule Hit/Alert.
- Verify `ACTION_REQUIRED`, `RESOLVED`, `CLOSED`, and `CANCELLED` have distinct
  approved semantics and reopen preserves every prior cycle.
- Verify external Architect approval is recorded without changing the approved
  V4 Design or its immutable Review/ZIP.
- Verify Q-008 changes are confined to Requirement gate synchronization, phase
  Lessons Learned, and a new approval/prerequisite Review plus ZIP.
- Run whitespace/static checks against Q-008 candidate files without modifying
  or staging protected pre-existing review artifacts.
- Confirm no Java, test, runtime configuration, migration, API, Kafka, Redis,
  adapter, CI, Docker, or Kubernetes path changed.
- Run existing Maven baseline tests without changing code; record Docker,
  Kubernetes, MySQL/Flyway, Redis, and Kafka runtime checks as `NOT APPLICABLE`.

### 22.2 Future implementation verification

- Unit-test aggregate creation and every valid/invalid/repeated transition.
- Test reopen history, cancellation terminal behavior, resolution invariants,
  and concurrent stale-write rejection.
- Test association append/supersede/invalidate history and upstream ownership.
- Test assignment requirements and actor/reason/time capture.
- Test case-number uniqueness/idempotency under concurrency.
- Integration-test atomic case mutation plus audit persistence.
- Run Maven test/package, static checks, database migration verification,
  Docker/Kubernetes rendering/runtime gates, and any approved API contract
  tests.

## 23. Risks

- Case intake may drift into upstream decision ownership and violate ADR-009.
- An unbounded aggregate containing notes, Evidence, Decisions, Actions, and
  audit history may become slow and difficult to keep consistent.
- `ACTION_REQUIRED` may be misread as proof that Action Execution succeeded.
- Reopening can erase prior resolution meaning unless resolution cycles are
  append-only.
- Duplicating Decision severity/risk level in Risk Case can create conflicting
  sources of truth.
- Sequential case numbers can leak business volume and create distributed
  contention.
- Separate case and audit writes can diverge without an explicit atomic
  consistency design.
- Generic subject references can become an unvalidated dumping ground if new
  types do not require explicit semantics.
- Comments and evidence may contain sensitive data; the approved access/audit/
  no-silent-deletion principles must be preserved while detailed policy remains
  explicitly deferred.

## 24. Implementation Design Decisions and Remaining Deferrals

The external Architect delegated the first two details to Implementation
Design. The formal design records them as follows:

1. **CaseNumber concrete algorithm — designed:** canonical lowercase UUIDv4
   with `RC-` prefix, standard Java generation, bounded collision retry, and a
   unique `CHAR(39)` business-key constraint. It remains independent from the
   `BIGINT` primary key and leaks no time, sequence, or case volume.
2. **Resolution History relational layout — designed:** the current root uses
   an optimistic `version` and cycle number; immutable resolution headers plus
   normalized Evidence/Action reference snapshots and transition history retain
   every ordered cycle. No mutable resolution field replaces history.

The following remain explicitly deferred and are not pulled into Q-008:

3. **Related-case Decision association — future Requirement/design:** one
   Decision may have at most one Primary Risk Case association in the
   Foundation. Related/cross-case associations are deferred.
4. **Team ownership — future Requirement/design:** individual assignment using
   `assignee`, `assignedBy`, and `assignedAt` is approved. Team ownership and
   queue semantics are deferred.
5. **Detailed sensitive-content policy — future security/compliance Requirement
   and Implementation Design:** exact retention, detailed permissions, legal
   hold, exceptional redaction workflow, and regulatory-retention
   implementation are deferred. Controlled and auditable access plus no silent
   destructive deletion remain mandatory.

## 25. Review Checklist

- [x] External Architect approved Q-008 Requirement scope and ADR-009 compatibility.
- [x] Architect Review V2 approved the Risk Case aggregate boundary.
- [x] Manual and decision-driven intake are first-class and remain distinct
      from Evidence/Decision.
- [x] External Architect explicitly resolved or deferred the five former Open Questions.
- [x] ADR-010 was explicitly approved for acceptance; acceptance is not Codex self-approval.
- [x] Risk Case remains downstream and does not own Decision creation.
- [x] Evidence, Decision, Action, Alert, Rule Hit, Audit, and comment semantics
      remain distinct.
- [x] State transitions use `OPEN`, `IN_REVIEW`, `ACTION_REQUIRED`, `RESOLVED`,
      `CLOSED`, and `CANCELLED`; arbitrary jumps are prohibited.
- [x] Resolution, closure, cancellation, and reopening retain history.
- [x] Subject design is typed and extensible without a universal entity model.
- [x] Evidence/Decision/Action associations are append-only and auditable.
- [x] Action and Execution remain separate; no vendor operation is invented.
- [x] Assignment references do not invent IAM/RBAC.
- [x] Priority uses `LOW`, `NORMAL`, `HIGH`, `CRITICAL` and does not duplicate
      Decision-owned risk semantics.
- [x] Case number is not the database primary key and does not expose volume.
- [x] Audit records are independent from comments and not an unbounded
      aggregate child collection; required audit and case mutation share one
      application database transaction.
- [x] API, database, Kafka, Redis, security, adapters, and operations impacts
      were reviewed explicitly.
- [x] No Java, test, migration, API, runtime, adapter, UI, commit, or push was
      performed in the Requirement phase.
- [x] Dedicated Q-008 V3 Architecture Approved Review Package and
      self-contained ZIP are complete and current.
- [x] Formal Q-008 Implementation Design resolves CaseNumber and Resolution
      History deferrals without creating implementation.
- [x] External Architect Design Review approved the exact V4 design on
      2026-08-25; no Design V5 is required.
- [ ] Trusted Actor/authorization and authoritative Trading Account, Evidence,
      Decision, Action, and ActionOutcome providers are approved, implemented,
      wired, and verified without fake fallbacks.

## 26. Implementation Gate

**Implementation Allowed: NO**

Reason: the Requirement and Architecture are approved, ADR-010 is Accepted,
and Implementation Design V4 is externally approved. Implementation
authorization remains blocked solely by prerequisites: real authoritative
Trading Account/Evidence/Decision/Action/ActionOutcome providers and an
authenticated Actor/authorization provider are unresolved. Do not create a
`RiskCase` Java type, entity, repository, service, controller, DTO, mapper,
migration, ResultCode, API, Kafka event/topic, Redis key, Account Control port,
or external adapter until a separate approved implementation prompt is issued.
