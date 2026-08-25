# Q-008 Architect Review V2 Architecture Review

## Review Result

REVISED — AWAITING ARCHITECT APPROVAL

The V2 direction is internally coherent and compatible with ADR-009 when
manual intake is treated as investigation chronology rather than a transfer of
Core Domain ownership. This review does not declare Requirement PASS,
Architecture PASS, Design Approved, or ADR-010 Accepted.

## Evidence Reviewed

- root `AGENTS.md`;
- Q-007 and revised Q-008 Requirements;
- Accepted ADR-009 and revised Draft ADR-010;
- Q-007 Architecture Design and Core Domain Skill;
- development-standards Skill and latest Q-007 Lessons Learned;
- the complete Q-008 V1 Requirement Review and current Review conventions;
- current application/adapters/deployment inventory and bounded Git status.

## Core-domain Compatibility

ADR-009 states that Evidence → Decision → Action → Risk Case is a domain
dependency/ownership baseline, not a universal runtime pipeline. V2 therefore
supports two case intake chronologies:

```text
Decision-driven: Evidence → Decision → Risk Case → optional Action coordination
Manual: Manual suspicion → Risk Case → Evidence gathering → Decision → optional Action
```

The manual path does not make Risk Case the owner, producer, or approver of
Evidence/Decision. Before a manual case becomes `ACTION_REQUIRED` or
`RESOLVED`, a formal Core Domain Decision must exist. Every Action still
originates from a Decision. The canonical ownership model is unchanged.

## Aggregate Boundary

`RiskCase` is the Aggregate Root for case-owned identity, CaseNumber, primary
SubjectReference, intake metadata, status, assignment, priority, reference
associations, investigation notes, Resolution History, reopen cycles, and
invariants. It does not own full Evidence content, Decision lifecycle, Action
Execution, vendor DTOs, or mutable Audit Log children.

This bounded ownership avoids both an anemic state record and an unbounded
aggregate. Historical association/resolution records are case-owned and
immutable, but exact loading/persistence mechanics remain implementation
design.

## Intake Architecture

`MANUAL` and `DECISION_DRIVEN` are the only current CaseIntakeSource values.
`DECISION_DRIVEN` is more stable than `AUTOMATED`: it identifies the required
domain fact while future Rule Engine, stream detection, scoring, anomaly
detection, or external alert mechanisms remain unimplemented details.

Manual cases require meaningful subject, reason, actor, time, priority, number,
and status. They require no fabricated Evidence/Decision/Rule Hit/Alert. Intake
reason remains case metadata and cannot serve as proof or risk conclusion.

## Subject Model

The first supported subject is `TRADING_ACCOUNT`. SubjectReference is a stable,
typed opaque reference mapped through the owning boundary/adapters. Customer,
IB/Agent, Order, Position, Device, IP, Payment, and Strategy require explicit
future type semantics. Arbitrary JSON, a generic entity table, unrestricted
strings, and polymorphic object graphs are rejected.

## Lifecycle and Resolution History

The lifecycle is explicit and small enough for named domain operations:

```text
OPEN → IN_REVIEW → ACTION_REQUIRED → RESOLVED → CLOSED
          └────────────────────────→ RESOLVED
```

`CANCELLED` is a reasoned terminal transition for a case that should not exist;
it is not `NO_RISK`. `ACTION_REQUIRED` is optional and does not prove execution.
`RESOLVED` captures a business outcome; `CLOSED` captures later administrative
finalization.

`CLOSED → IN_REVIEW` requires actor, reason, UTC time, and Audit Record and
starts a new cycle. All earlier Evidence, Decisions, Actions, resolutions,
close facts, and audits remain immutable. An ordered immutable Resolution
History record/entity per cycle is preferred over one mutable field. No
workflow engine, scheduler, SLA, timer, or Event Sourcing is introduced.

## Evidence Boundary

Risk Case stores Evidence references and append-only association metadata, not
Evidence content. Superseded, invalidated, and withdrawn dispositions explain
association history without editing or physically deleting upstream Evidence.
This preserves Q-007 provenance and prevents silent historical rewriting.

## Decision Boundary and Cardinality

Decision remains the Core Domain and owns Decision lifecycle. A case may link
successive Decision versions; a new Decision never overwrites an earlier one.
The default foundation rule is at most one primary-case association per formal
Decision. Related-case associations remain architecturally possible but are
deferred to avoid ambiguous ownership, duplicate Actions, inconsistent case
resolution, and audit confusion.

## Action and Execution Boundary

Risk Case may associate proposed/approved Action references and available
outcome/execution-outcome references. Action still originates from Decision and
is not proof of execution. MT4/MT5 Manager, Bridge, dealer plugin, leverage,
forced close, restriction execution, and other Account Control behavior remain
future adapter capabilities.

## Assignment, Priority, and Case Number

Minimum Assignment is assignee, assigned-at, and assigned-by. Team ownership is
reserved/deferred; Q-008 creates no IAM, RBAC, hierarchy, or workflow engine.
Assignment changes are audited.

Case Priority uses `LOW`, `NORMAL`, `HIGH`, and `CRITICAL`. Severity/Risk Level
remain Decision-owned Source of Truth. A future read projection may denormalize
them but cannot become a second independently mutable source.

CaseNumber is a separate immutable Value Object generated through a boundary,
not the database primary key. A sequential daily format leaks volume and adds
allocation contention. Exact opaque generation remains a final decision; Q-008
does not design a distributed ID platform.

## Audit and Atomicity

Audit is independently owned, append-only, and immutable. Domain state is not
rebuilt by replaying Audit; Event Sourcing is rejected. Every important case
mutation and its Audit Record commit in one application-owned database
transaction inside the Phase 1 modular monolith. Kafka-only audit, distributed
transaction, 2PC, and Saga are excluded.

## Architecture Impact

| Area | V2 result |
| --- | --- |
| Product boundary | Broker/CRM/platform neutrality preserved |
| Core Domain | Decision remains Core Domain; manual intake changes chronology only |
| Modular monolith | One repository/deployable; no service split |
| API | No endpoint/DTO/ResultCode/OpenAPI change |
| Database/Flyway | No current schema/migration; future relational design deferred |
| Redis | No key/cache/source-of-truth change |
| Kafka | No topic/event/producer/consumer change |
| Audit | Independent ownership and same-database-transaction principle only |
| Security | Sensitive note/evidence access/retention/redaction remains open |
| Account Control | Reference boundary only; no execution |
| External systems | No MT4/MT5/Bridge/LP/CRM integration |
| Operations | No dependency/configuration/CI/Docker/Kubernetes change |

## Development Standards Compliance

### AGENTS.md compliance

Inspected product boundary, Phase 1 stack, Requirement/ADR discipline,
implementation prohibition, auditability, Review Package, and Git rules. The
change is limited to Q-008 Requirement/ADR Draft and a new V2 Review/ZIP. It
preserves V1/Q-007/history, introduces no implementation, and claims no
approval.

### Architecture compliance

Compared Q-007 Requirement/architecture/Skill, ADR-009, revised Q-008, and
Draft ADR-010. Manual intake is explicitly an operational chronology and does
not change Decision ownership. Action/Execution and external adapter boundaries
remain intact. No microservice, workflow framework, Event Sourcing, or generic
entity framework is added.

### ADR compliance

Reviewed Accepted ADR-001 through ADR-009 as applicable. ADR-010 remains Draft
and records aggregate, intake, lifecycle, references, audit ownership,
transaction consistency, and resolution-history proposals with alternatives
and consequences. No Accepted ADR was modified or silently superseded.

### API standard compliance

No controller, DTO, endpoint, `ApiResponse`, ResultCode, exception, validation,
OpenAPI, or Actuator path changed. No speculative API contract was added.

### Database standard compliance

No entity, repository, SQL, Flyway migration, table, column, index, DDL, or DML
was created. Future requirements retain Flyway, `snake_case`, `BIGINT id`,
separate CaseNumber, UTC, stable codes, optimistic concurrency, uniqueness, and
append-only history rules.

### Security standard compliance

No credential, token, secret, KYC document, personal record, or new data
exposure was introduced. Intake/assignment actor references are not auth
tokens. Sensitive investigation-note/evidence access, retention, and redaction
remain explicit final-review questions rather than invented policy.

### Auditability compliance

The revised Requirement enumerates creation, assignment, priority, status,
Evidence/Decision/Action association, notes, resolution, close, reopen, and
cancellation audit facts. Audit remains independently owned and atomic with
case mutation through one application database transaction, without Event
Sourcing or best-effort Kafka-only durability.

### Skill compliance

Applied the development-standards and Core Domain Skills. The Core Domain Skill
caused manual intake to be framed as chronology rather than ownership and kept
Action separate from execution. No Skill or Lessons Learned update is created
because Q-008 remains unapproved design-only work with no verified
implementation lesson.

## Conclusion

No standards violation is identified in the revised documentation scope. The
five remaining domain questions and Draft ADR-010 still require final Architect
review.

Implementation Allowed: **NO**

Ready for Architect Review: **YES**
