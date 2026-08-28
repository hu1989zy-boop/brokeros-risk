# Q-010 V3 Architecture Review

- Review result: `PASS — SUBMISSION READY`
- Architecture: `PROPOSED — AWAITING EXTERNAL ARCHITECT REVIEW`
- ADR-012: `PROPOSED — AWAITING EXTERNAL ARCHITECT REVIEW`
- Implementation Design: `NOT STARTED`
- Implementation Allowed: `NO`

The Architecture is semantically complete for independent review. It resolves
all 25 required review questions, traces Q010-FR-001 through Q010-FR-012, and
leaves only implementation mechanics or future-Requirement scope deferred.

## Development Standards Compliance

### AGENTS.md compliance

`AGENTS.md`, the approved Q-010 Requirement, Q-007/Q-008/Q-009 authorities,
accepted ADRs, development standards, applicable Skills/Lessons, Q-008
approval/prerequisite evidence, Q-009 V10 closure, current backend packages,
Flyway migrations, and Q-009 security contracts were inspected. Changes are
documentation/governance only; no implementation or prohibited Git operation
occurred.

### Q-007 / ADR-009 core-domain compliance

Q-010 remains supporting upstream context and owns only Trading Account
reference identity. It creates no Evidence, Decision, Action, execution, Rule
Engine, or case behavior. Decision remains Core Domain and the canonical
Evidence → Decision → Action → Risk Case direction is unchanged.

### Q-008 / ADR-010 consumer-boundary compliance

Q-008 can consume only protected read-only eligibility validation by
TradingAccountRef. It cannot register/mutate identity or see the external key,
source details, persistence ID, customer data, or vendor DTO. Q-008's approved
V4 design and historical approval record are unchanged; Implementation Allowed
remains NO.

### Q-009 / ADR-011 trusted-actor compliance

The non-web provisioner uses a purpose-specific registered SERVICE descriptor,
fresh Q-009 ActorContext, active MySQL mapping, and exact direct capability.
Every read/mutation authorizes before Q-010 access. No SYSTEM, actor header,
role/scope claim, cached permission, fabricated token, or inherited human
context is accepted.

### Q-010 approved Requirement compliance

Architecture preserves the exact scope + SourceNamespace + ExternalAccountKey
tuple, immutable one-to-one mapping, lifecycle/history, non-web attested
registration, exact three capabilities, same-transaction provenance, bounded
Q-008 disclosure, and fail-closed outcomes. Requirement semantics were not
reopened or weakened.

### Module and boundary compliance

The proposed `com.brokeros.risk.tradingaccount` logical capability remains in
the single Phase 1 Spring Boot deployable. It is explicitly not a full Trading
Account/customer/broker/tenant master and has no dependency on Risk Case or
vendor DTOs. No dumping-ground package or microservice is proposed.

### API boundary compliance

No REST endpoint, DTO, `ApiResponse`, ResultCode, exception mapping, OpenAPI
contract, or API version is created. Registration is non-web. The two proposed
reads are protected internal application contracts returning bounded values,
not entities.

### Database and Flyway compliance

No SQL or migration was created. Architecture selects the existing application
MySQL/Spring JDBC/Flyway stack and requires future additive Flyway, `BIGINT id`,
separate opaque refs, UTC, binary comparisons, two-direction uniqueness,
optimistic versions, restrict-delete FKs, append-only history, indexes, and
disposable MySQL 8.4 verification.

### Security compliance

Authorization precedes protected lookup. External keys/tuples, credentials,
tokens, claims, vendor payloads, and principal identifiers are excluded from
logs/errors/history disclosure. Unknown, conflicting, corrupt, denied, and
unavailable states fail closed and never auto-register or disclose existence.

### Auditability and history compliance

Registration/lifecycle state, durable idempotency outcome, and immutable
operation history share one local transaction. History contains actor, UTC
time, operation, target, attestation, reason, before/after state, and version.
History failure rolls back state. A general Audit module is not invented.

### External-system isolation compliance

Raw MT4/MT5/CRM IDs are not BrokerOS refs. Future adapters require their own
approved contracts and may only translate into protected canonical resolution.
Direct external database access, auto-discovery, polling, synchronization,
vendor SDK invention, and external writeback are prohibited.

### Skill and lessons compliance

`development-standards.md`, `brokeros-risk-core-domain.md`, and
`trusted-actor-authorization.md` were applied. A new Architecture Lessons
Learned document records actual identity, normalization, attestation,
idempotency, authorization, and MySQL analysis. No Skill was changed because
the proposed pattern is not yet approved, implemented, or runtime-verified.

## Impact Review

- Backend/source/test: no change.
- API/ResultCodes: no change.
- Database/Flyway: architecture proposal only; no migration.
- Security: consumes existing Q-009 contracts; no security code/config change.
- Q-008: future provider effect only; no implementation or authorization.
- Rule Engine/Account Control/Audit platform: no implementation.
- Kafka/Redis/MT4/MT5/BrokerPilot/oneZero/CRM: no runtime impact.
- Docker/Kubernetes/operations: no behavior change.

No unresolved standards violation prevents external Architecture review.
