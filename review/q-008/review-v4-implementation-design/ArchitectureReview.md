# Q-008 Implementation Design Review V4

## Review Result

**READY FOR ARCHITECT DESIGN REVIEW — NOT APPROVED**

The design is internally coherent and preserves approved Q-008/ADR-010
architecture. It resolves the authorized CaseNumber and Resolution History
deferrals. Implementation remains prohibited because external Design approval,
real upstream reference providers, and authenticated Actor/authorization
provision are unresolved.

## Scope Reviewed

- approved Q-008 Requirement and Accepted ADR-009/ADR-010;
- Q-007 architecture, Core Domain Skill, and Lessons Learned;
- Q-008 V1/V2/V3 Review packages;
- formal Q-008 Implementation Design;
- existing backend/API/ResultCode/exception/JDBC/Flyway conventions;
- current source, migration, dependency, deployment, and Git scope.

## Design Assessment

### Domain and aggregate

RiskCase remains Aggregate Root only for case-owned state and invariants.
Current state is bounded; histories are append-only records loaded through
bounded queries. Evidence, Decision, Action execution, and Audit do not become
owned aggregate children. Decision remains Core Domain.

### Lifecycle

Every approved transition has a named operation. No generic `setStatus`, target
status request, workflow engine, or arbitrary transition exists. Reopen
preserves histories, increments the cycle, clears the current Decision pointer
deliberately, records reason/actor/time, and requires audit atomically.

### CaseNumber

UUIDv4 with an `RC-` prefix satisfies opacity, global collision resistance,
immutability, external safety, database-ID separation, and non-volume leakage
without a new library. UUIDv7, ULID, Snowflake, sequence, and custom Base32 were
compared and rejected for concrete reasons.

### Resolution and history

One immutable Resolution Record per `(case, cycle)` plus normalized
Evidence/Action reference snapshots preserves every cycle. Root `version`
orders commands and supplies optimistic concurrency. Administrative closure and
reopen are Transition Records, not overwritten resolution fields.

### Persistence

The proposal uses existing Spring JDBC and MySQL. It specifies tables, columns,
keys, unique/check constraints, indexes, UTC timestamps, restrict-delete FKs,
idempotency hashes, and optimistic locking. No JPA, SQL, migration, table, or
dependency is created during Design.

### Transaction and audit

One application service owns one Spring database transaction. CAS root update,
case-owned records, and independently owned Audit Record either all commit or
all roll back. Audit failure prevents success. `REQUIRES_NEW`, asynchronous
Kafka-only durability, distributed transaction, 2PC, Saga, and Event Sourcing
are excluded.

### API and application use cases

The design defines named use cases and endpoints for creation, assignment,
review, associations, action-required coordination, priority, notes,
resolution, close, cancel, resume/reopen, and bounded queries. All reuse
`ApiResponse`, Bean Validation, BusinessException/ResultCode, DTO separation,
create idempotency, and explicit expectedVersion.

### Security

The design requires authenticated ActorContext, authorization before access,
access/change audit, minimized output/logs, and no destructive deletion. It
does not invent roles, IAM/RBAC, teams, legal hold, or retention rules. The
absence of a real security provider is surfaced as a blocker.

### Concurrency

One root optimistic version serializes same-case material commands. Unique
CaseNumber, create idempotency, unique primary Decision association, unique
resolution-per-cycle, and rollback-after-conflict handle duplicate and race
scenarios without last-write-wins behavior.

## Dependency and Blocker Assessment

The design correctly refuses to fabricate upstream objects. Because Q-007
created no Evidence/Decision/Action runtime capability, decision-driven create,
association, action-required, and resolution require approved reference
providers before they can operate. Likewise, current backend code provides no
trusted actor/auth boundary. These are real implementation prerequisites; they
do not invalidate approved architecture or the design artifact.

## ADR Assessment

**New ADR required: NO**

**ADR-010 amendment required: NO**

The design preserves module/deployment/ownership/transaction architecture and
adds no framework/dependency. UUIDv4 and normalized tables are the exact
implementation details ADR-010 delegated. Recording them in this formal design
is sufficient unless external Design Review changes an architecture-level
boundary.

## Development Standards Compliance

### AGENTS.md compliance

Inspected the repository-wide product, Phase 1, Requirement, Git, architecture,
security, audit, and Review rules. Changes are limited to the approved
Requirement gate text, formal Design, V4 Review, and ZIP. No code, staging,
commit, push, reset, clean, or historical deletion occurs.

### Architecture compliance

ADR-009 remains authoritative: Decision is Core Domain and Risk Case remains a
downstream capability. The design keeps one Spring Boot modular monolith,
isolates external references, and separates Action intent from execution. No
microservice, workflow engine, or vendor coupling appears.

### ADR compliance

Accepted ADR-009/ADR-010 were inspected and are not modified. The design uses
independent Audit ownership and same application database transaction exactly
as ADR-010 requires. No new ADR threshold is crossed.

### API standard compliance

The API proposal uses `/api/risk-cases`, action-specific DTOs, Jakarta
Validation, `ApiResponse`, `BusinessException`, stable symbolic ResultCodes,
and `GlobalExceptionHandler`. It exposes no entity/internal ID and designs no
versioned `/api/v1` prefix.

### Database standard compliance

The design uses Flyway-only future schema change, `snake_case`, `BIGINT id`,
separate business identifiers, UTC, readable codes, explicit constraints and
indexes, and restrict-delete history. `backend/src/main/resources/db/migration`
remains unchanged; no DDL/DML exists in this task.

### Security standard compliance

No secret, credential, KYC document, production data, or authentication header
is added. The design rejects spoofable actor identity, minimizes sensitive
audit/log content, and explicitly blocks controller exposure until an approved
provider exists.

### Auditability compliance

Every material use case identifies its Audit operation. The design captures
actor, time, operation, target, safe before/after, reason, source, request, and
trace context. Audit append failure rolls back the case mutation. Sensitive
plaintext is excluded from audit payloads.

### Skill compliance

Applied `development-standards` and `brokeros-risk-core-domain`. No reusable
Skill change is justified because this phase produces a Q-008-specific design
and no verified implementation pattern. The required phase Lessons Learned is
added at
`docs/lessons/2026-08-25-q-008-risk-case-implementation-design.md`; it records
selected and rejected designs, the real upstream/security gaps, reusable
warnings, and future risks without inventing a runtime problem. Skill
evaluation is required again after implementation.

## Design Gate Conclusion

- Design artifact: COMPLETE
- Ready for Architect Design Review: YES
- Design approved: NO
- Unresolved implementation blockers: 3
- Implementation Allowed: **NO**
