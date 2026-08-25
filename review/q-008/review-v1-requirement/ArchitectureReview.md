# Q-008 Requirement Review V1 Architecture Review

## Review Result

READY FOR ARCHITECT REVIEW — NOT APPROVED

The Q-008 proposal is compatible with the existing architecture if its
downstream reference-only relationship to Evidence, Decision, and Action is
preserved. Draft ADR-010 is required before implementation because the proposed
aggregate, lifecycle, and audit consistency decisions are durable architecture.

## Evidence Reviewed

- repository-wide `AGENTS.md`;
- Q-001 through Q-007 Requirements and current Requirement conventions;
- all accepted ADRs ADR-001 through ADR-009;
- all active architecture documents through Q-007;
- `docs/skills/development-standards.md`;
- `docs/skills/brokeros-risk-core-domain.md`;
- current repository Skills index and Q-007 Lessons Learned;
- current root Review Package, archived Q-007 Design V1 convention, and the
  standalone Q-007 review convention;
- backend/adapters/deployment file inventory and searches for existing Risk
  Case, Evidence, Decision, Action, Rule Engine, assignment, and audit code;
- Git branch, HEAD/origin alignment, staged/unstaged state, and pre-existing
  untracked review paths.

The protected `review/review-history/` contents were not inspected or modified.
The pre-existing untracked `review/q-007/` path was inspected only to understand
the current standalone Review naming convention and was not modified or staged.

## Architecture Assessment

### Core-domain alignment

Q-008 retains ADR-009 as authoritative:

```text
Evidence → Decision → Action → Risk Case
```

- Decision stays the Core Domain.
- Risk Case is optional and downstream.
- A Decision and Action remain valid without a case.
- Case associations do not transfer ownership.
- Rule Engine remains a future Decision mechanism and cannot emit cases or
  Actions directly.
- Action remains intent; execution attempts/outcomes remain downstream.

The Draft proposal that a case starts from an existing Decision and
attributable Evidence is the safest interpretation of ADR-009. The Architect
must confirm it because allowing a case to create its own preliminary decision
would re-center the domain around case workflow.

### Aggregate boundary

Risk Case qualifies as an Aggregate Root for case-owned invariants: status,
assignment, priority, current associations, resolution, cancellation, close,
reopen, and concurrency. The boundary is deliberately limited. It does not
contain Evidence, Decisions, Actions, execution attempts, external DTOs, or an
unbounded audit log as owned entities.

Association records, comments, and resolution cycles are case-owned business
records. They retain immutable history, but persistence/query design must avoid
loading all history merely to validate a current mutation.

### Lifecycle architecture

The six explicit states are small enough for named domain behavior; no workflow
engine, BPMN, generic state-machine framework, or event-sourced aggregate is
justified. Transition validity belongs to the aggregate. The application layer
orchestrates authorization and persistence; controllers, repositories, and
adapters do not decide transition legality.

`RESOLVED` and `CLOSED` have distinct business and audit meanings. `CANCELLED`
is a retained invalid/duplicate/out-of-scope record, not a risk conclusion.
Exceptional reopening returns a resolved/closed case to `IN_REVIEW` and never
erases earlier cycles.

### Cross-context contracts

Risk Case needs typed stable references to Evidence, Decision, Action, subject,
actor, and team. Exact package, API, schema, and synchronous/event contract are
not selected. The architecture must prohibit direct cross-module mutation and
external vendor types. No Kafka event is proposed because no event contract,
delivery guarantee, ordering, or consumer requirement exists yet.

### Audit architecture

Audit Record should remain independently owned because audit spans multiple
capabilities and can grow under different retention/access rules. Keeping an
unbounded audit collection inside Risk Case would make aggregate loading and
ownership unsafe.

Strong audit requires the case mutation and its mandatory audit record to be
durable atomically. Best-effort Kafka-only audit is insufficient. The Phase 1
modular monolith can provide one application-owned consistency boundary without
introducing a microservice or event sourcing, but exact transaction/persistence
design requires approval and implementation design.

## Existing Capability / Gap Findings

### Already Exists

- Core-domain ownership, adapter isolation, Action/Execution separation, and
  Evidence provenance are authoritative.
- A single modular-monolith deployable and application-owned MySQL/Flyway
  source-of-truth direction exist.
- API/error/validation, tracing, logging, configuration, CI, and review
  foundations are reusable.
- Stable enums, UTC, named transitions, business IDs, and audit fact standards
  already constrain future implementation.

### Need Improvement

- Positive Risk Case semantics and lifecycle are missing.
- Existing audit obligation lacks case-level mutation consistency.
- Q-007 conceptual associations lack correction/reassessment/history rules.
- No current subject/actor/team reference convention exists.

### Need New Design

- Aggregate invariants and concurrency.
- Controlled lifecycle and reopening.
- Typed subject and identity references.
- Association/current-reference history.
- Assignment, notes, resolution, cancellation, and case number.
- Audit ownership and atomic durability.

### Conflict / Blocker

There is no direct conflict in the proposed resolution. The unresolved intake
question is an architecture blocker: case-before-decision behavior could
conflict with ADR-009. Draft ADR-010 and Q-008 must be approved together or
revised together. Implementation cannot begin while either remains Draft.

## ADR Assessment

**ADR Required: YES**

Reasons:

- `RiskCase` Aggregate Root and ownership are durable aggregate-boundary
  decisions.
- The controlled lifecycle establishes state-machine architecture without a
  workflow framework.
- Evidence/Decision/Action references define cross-context ownership.
- Independent Audit ownership plus atomic durability defines a major
  consistency boundary.
- These choices shape later schema, module contract, service orchestration,
  tests, and API behavior.

Draft ADR-010 includes Context, Proposed Decision, Alternatives, Consequences,
Approval Questions, and an explicit non-implementation gate. It is correctly
not marked Accepted.

## Architecture Impact

| Area | Pre-check result |
| --- | --- |
| Product boundary | Preserved; typed references remain broker/CRM/MT4/MT5 neutral |
| Core Domain | Preserved; Decision remains Core Domain |
| Modular monolith | Preserved; Risk Case is a logical capability, not a service split |
| API | No current impact; future application APIs must reuse `ApiResponse`, validation, exception handling, and approved paths |
| Database/Flyway | No current change; future case/audit schema requires a new immutable migration and full DDL/index/locking review |
| Redis | No current use; cannot become durable case/audit source of truth |
| Kafka | No topic/event proposed; event model requires separate approval |
| Audit | New architecture proposal only: independent ownership plus atomic durability |
| Security | Sensitive comments/evidence references, access, redaction, and retention remain open before implementation |
| Account Control | Conceptual downstream boundary only; no execution interface or vendor operation |
| MT4/MT5/Bridge/LP/CRM | No implementation; remain behind future verified adapters |
| Rule Engine/AI | No implementation; remain at Decision boundary |
| Operations/deployment | No current dependency, configuration, Docker, Kubernetes, or CI change |

## Development Standards Compliance

### AGENTS.md compliance

Inspected the repository-wide rules for product neutrality, Phase 1 stack,
Requirement discipline, adapter isolation, state/audit standards, Review
Package, Git restrictions, and Definition of Done. Q-008 has a stable
Requirement ID, changes documentation only, uses a dedicated Review directory,
preserves pre-existing Q-007/review-history artifacts, and explicitly prohibits
implementation, commit, and push. No business behavior was inferred directly
into code.

### Architecture compliance

Inspected all active architecture documents through Q-007. The Draft remains
inside one feature-first Spring Boot modular monolith and creates no service,
package, database, topic, cache, adapter, or deployment topology. Risk Case is
kept downstream; vendor details remain outside domain ownership; workflow
framework and event sourcing are rejected under YAGNI.

### ADR compliance

Inspected accepted ADR-001 through ADR-009. The Draft preserves the approved
technology roadmap, external isolation, engineering/API/data standards,
deployment, CI, tracing, configuration, and Q-007 Core Domain. Because Q-008
crosses the aggregate/lifecycle/audit consistency threshold, Draft ADR-010 is
created rather than silently encoding the choice in implementation. No
accepted ADR was modified.

### API standard compliance

Inspected Q-008 candidate paths and backend inventory. No controller, DTO,
endpoint, `ApiResponse`, ResultCode, exception, Bean Validation, OpenAPI, or
Actuator artifact is created or changed. Future API shape remains undecided and
requires approved design; no speculative ResultCode catalog exists.

### Database standard compliance

Inspected `backend/src/main/resources/db/migration` and Q-008 candidate paths.
No SQL, migration, entity, repository, table, column, index, key, DDL, or DML is
added. The Draft repeats the future Flyway, `snake_case`, `BIGINT id`, separate
business ID, UTC, stable enum, concurrency, uniqueness, and no-physical-delete
obligations without inventing schema.

### Security standard compliance

The candidate contains no credential, token, private key, personal record, KYC
document, external authentication material, or new data exposure. It identifies
sensitive comments/evidence references, access control, retention, and
redaction as blocking policy questions rather than assuming safe defaults.
Actor/team refs are not authentication, and Request/Trace IDs are correlation
only.

### Auditability compliance

The Draft enumerates every material mutation requiring audit and the mandatory
actor/time/operation/target/before/after/reason/source/correlation facts. It
separates business comments from Audit Records, rejects best-effort audit, and
keeps audit outside the unbounded aggregate while requiring atomic durability.
No speculative Audit module, table, or Kafka event is implemented.

### Skill compliance

Applied `docs/skills/development-standards.md` and
`docs/skills/brokeros-risk-core-domain.md`. The latter directly constrained the
case to downstream reference-only ownership and Action/Execution separation.
No new Skill or Lessons Learned entry is created because Q-008 is only a Draft
and no approved/verified reusable implementation lesson exists. Skill/Lessons
evaluation must be repeated after an approved implementation or material design
revision.

## Review Conclusion

No unresolved violation exists in the documentation-only Q-008 drafting scope.
The design itself is not approved: the Architect must resolve the intake,
subject, lifecycle, priority, number, cardinality, audit, and sensitive-content
questions and explicitly decide Draft ADR-010.

Implementation Allowed: **NO**

Ready for Architect Review: **YES**
