# Q-009 Architecture Review

## Review Result

- Self-review result: PASS
- Ready for Architect Review: YES
- Architect decision: PENDING
- ADR-011 status: Proposed
- Implementation allowed: NO

PASS means that the proposed architecture package is internally complete and
ready for an Architect decision. It does not mean that Architect approval has
been granted.

## Architecture Fit

The architecture preserves one Phase 1 Spring Boot modular monolith. Identity
providers, token validation, and future external service authentication remain
replaceable infrastructure adapters. BrokerOS owns only its stable actor,
mapping, activation, and capability semantics.

Domain logic receives ActorRef where business/audit behavior requires it and
does not depend on HTTP, Spring Security, token claims, or provider roles. The
authoritative authorization decision is placed at the application use-case
boundary, so HTTP, background, and future transport adapters share the same
enforcement model.

## Important Boundaries Reviewed

- authentication is separate from actor mapping;
- actor mapping is separate from capability authorization;
- `VerifiedPrincipal` is authenticated but not yet a BrokerOS actor;
- `ActorContext` is immutable, bounded, and per execution;
- request/trace correlation is not identity;
- HUMAN and SERVICE identities cannot silently substitute for one another;
- background execution uses a purpose-specific service actor, not `SYSTEM`;
- external roles/claims are governed inputs, never direct permissions;
- application use cases authorize before protected access or mutation;
- all missing, ambiguous, inactive, unavailable, and indeterminate paths fail
  closed;
- framework types remain inside infrastructure.

## Options Review

### Identity ownership

The pluggable-hybrid model is preferred over BrokerOS-owned passwords because
Q-009 does not require a credential product. It is preferred over complete
provider ownership because BrokerOS must keep stable, vendor-neutral business
and audit semantics.

### Authorization enforcement

Application-use-case enforcement is preferred over controller/URL-only checks
because it covers schedulers and future adapters. Framework rules remain useful
as defense in depth.

### Background actor

Purpose-specific service identities are preferred over a generic `SYSTEM`
identity because they preserve least privilege and accountability.

## Q-008 Compatibility

The proposal is compatible with Q-008's approved ActorRef and
`risk-case:<action>` capability expectations. It does not revise Q-008 business
design or authorize its implementation. Trading Account, Evidence, Decision,
Action, and ActionOutcome provider contracts remain unresolved Q-008
prerequisites.

## Development Standards Compliance

### AGENTS.md compliance

Inspected the repository-wide `AGENTS.md`, the approved Q-009 Requirement,
relevant Q-007/Q-008 authorities, development standards, skills, and lessons
before drafting. The work stays in Architecture/ADR/Review documentation scope.
No source, dependency, configuration, schema, implementation design, or Git
history operation was performed.

### Architecture compliance

The proposal retains the Phase 1 modular monolith and isolates identity/token
providers behind adapters. It is broker-neutral, CRM-neutral, and
trading-platform-neutral. It does not introduce microservices, direct external
database access, Python, Flink, Kafka, Redis, Docker, or Kubernetes behavior.

### ADR compliance

The identity authority, actor boundary, capability policy, enforcement point,
and Spring Security boundary are important architecture choices. They are
recorded together in ADR-011 with Context, Decision, Alternatives, and
Consequences. The ADR is honestly marked Proposed and contains an explicit
approval boundary.

### API standard compliance

No API is added or changed. The architecture requires future failures to retain
`ApiResponse`, `GlobalExceptionHandler`, Bean Validation where applicable, and
safe non-leaking errors. Exact result codes and endpoints are deferred rather
than invented.

### Database standard compliance

No schema, table, SQL, entity, repository, or Flyway migration is created or
changed. Future actor/policy state is identified as application-owned durable
state, and its schema must be separately designed through versioned Flyway
migrations. No external-system database access is permitted.

### Security standard compliance

The proposal uses external credential authentication, bounded principal
translation, unique active actor mapping, default-deny capability authorization,
least privilege, purpose-specific service actors, fail-closed dependency
behavior, protected operational endpoints, and no raw credential/claim logging.
The threat review explicitly covers spoofing, forgery, stale credentials,
privilege escalation, service impersonation, authorization bypass, context
leakage, audit spoofing, and provider outage.

### Auditability compliance

Audit actor identity derives exclusively from the trusted ActorContext. The
architecture supplies ActorRef, actor type, authentication provenance,
capability decision, policy provenance/version, UTC evaluation time, and
separate correlation. It does not invent an Audit module or persist raw tokens
or full claims.

### Skill compliance

`docs/skills/development-standards.md`, the applicable core-domain skill, and
the observability correlation guidance were inspected. No skill was changed:
the proposal has not yet produced a verified implementation pattern. An honest
architecture Lessons Learned document records reusable findings and calls for a
skill reassessment after implementation.

## Violations

No unresolved standards violation was found in the Architecture/ADR/Review
scope. Open provider and implementation choices are recorded as open decisions,
not silently assumed.
