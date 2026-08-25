# Q-009 Requirement Boundary and Standards Review

## Architecture review scope

This is not a Q-009 Architecture proposal or approval. It checks whether the
Draft Requirement respects the currently accepted architecture and leaves
durable architecture decisions for the required post-approval ADR gate.

## Boundary findings

- The Requirement preserves the Phase 1 modular monolith and broker-neutral
  adapter boundary.
- It does not bind identity to a CRM, broker, employee database, MT4/MT5, token
  type, identity vendor, gateway, or proxy.
- It separates security context from ActorRef/domain semantics and from
  Request/Trace correlation.
- It does not create an Audit module or collapse Q-008 provider ownership into
  Q-009.
- A future decision about identity authority, trust integration, ActorContext
  ownership, authorization model, runtime enforcement, and Audit attribution
  crosses module/runtime boundaries and therefore requires an ADR.

## ADR determination

- ADR Required: **YES**
- Recommended topic: **Trusted Identity Boundary, ActorContext Ownership, and
  Capability Authorization Model**
- ADR created or accepted in this gate: **NO**

## Development Standards Compliance

### AGENTS.md compliance

The Review inspected the root authority, approved Q-003/Q-005/Q-007/Q-008
requirements, applicable architecture, ADR-007/009/010, development standards,
observability/Core Domain Skills, and the latest Q-008 prerequisite review.
The change remains in the required Requirement-first workflow and does not
silently invent missing identity authority or implementation behavior.

### Architecture compliance

The Draft preserves one Phase 1 modular-monolith deployable, external-system
adapter isolation, broker/platform neutrality, separation of detection/action,
and independently owned systems. No microservice, gateway deployment, vendor
coupling, Flink, or Python is introduced.

### ADR compliance

No accepted ADR is modified. ADR-007 correlation separation, ADR-009 ActorRef
semantics, and ADR-010/Q-008 prerequisites are preserved. The new durable
security decisions are explicitly deferred to a required ADR after Requirement
approval; no ADR is prematurely created or accepted.

### API standard compliance

No endpoint, DTO, ResultCode, or API contract is implemented. The Requirement
preserves future `ApiResponse`, validation, and safe exception conventions and
prohibits caller-selected Audit identity. There is no speculative ResultCode.

### Database standard compliance

No schema, table, column, entity, repository, DDL/DML, or Flyway migration is
added or changed. Identity/permission/user persistence is an open Architecture
decision rather than an invented database model.

### Security standard compliance

The Draft requires fail-closed/default-deny behavior, least privilege,
server-side enforcement, verified trust boundaries, purpose-specific service
identity, and protection of secrets/tokens/claims. It explicitly rejects
caller-supplied actor fields, correlation-as-identity, generic privileged
`SYSTEM`, permissive production fallbacks, and direct external-database access.

### Auditability compliance

Trusted ActorRef, actor type, authentication source, capability, outcome,
target/context, UTC time, and separate Request/Trace correlation are required
for future attribution. Credentials and raw assertions are prohibited. The
Requirement does not invent the Audit module ahead of its owner.

### Skill compliance

The development-standards, observability-correlation, and Core Domain Skills
were applied. The new Lessons Learned records the discovery. No Skill update is
claimed because no verified reusable security implementation pattern exists at
this gate.

## Result

No unresolved standards violation was found in the Requirement Discovery
artifacts. This result does not approve the Requirement or permit Architecture,
ADR, Design, or Implementation.
