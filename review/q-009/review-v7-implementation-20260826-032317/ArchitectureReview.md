# Q-009 Implementation Architecture Review

## Review Result

Architecture review: **BLOCKED — VERIFICATION INCOMPLETE**

The inspected implementation conforms structurally to Q-009 Architecture V2
and ADR-011. No architecture violation was identified. The review cannot be
marked PASS because mandatory MySQL 8.4 and deployment/infrastructure evidence
is missing.

## Architecture Impact

- Adds one internal `security` capability to the existing modular monolith.
- Adds a signed-JWT servlet adapter while keeping domain/application code
  framework-neutral.
- Adds application-owned ports for actor mapping, ActorContext access,
  capability decisions, and controlled provisioning.
- Adds three application-owned MySQL tables through additive Flyway V2.
- Adds no service split, Kafka topic, Redis key, external adapter, identity
  vendor, Q-008 code, role model, or Audit module.

## Development Standards Compliance

### AGENTS.md compliance

The implementation was traced to Q-009 Requirement V1, approved Architecture
V2, accepted ADR-011, and approved Implementation Design V1. Q-008 remains
unchanged and unauthorized. Package names use `com.brokeros.risk.security`
without a generic dumping-ground package. No Git staging, commit, push, reset,
clean, or stash operation was performed. Compliance is incomplete only because
the Definition of Done requires applicable real integration verification that
could not run.

### Architecture compliance

One Spring Boot modular monolith is preserved. `SecurityArchitectureTests`
prove that domain and application packages do not import Spring Security,
servlet, JWT, or JDBC types. Authentication, mapping, and authorization remain
separate. No concrete vendor or unrelated integration boundary was introduced.

### ADR compliance

ADR-011's pluggable-hybrid boundary, server-owned ActorRef mapping, immutable
ActorContext, explicit capability model, default deny, purpose-specific service
actors, application enforcement, fail-closed unavailability, and trusted Audit
attribution fields are implemented. Roles, gateway trust, SYSTEM, JIT mapping,
delegation, and caching remain absent.

### API standard compliance

No business REST endpoint was added. Security failures use existing
`ApiResponse` serialization and stable ResultCodes. Framework operational
health remains protocol-native/public; information and documentation routes are
authenticated, and documentation is disabled in production. Existing API,
exception, and correlation regression tests pass.

### Database standard compliance

The only schema change is forward-only Flyway V2. It uses `snake_case`, BIGINT
internal `id`, separate UUIDv4 `actor_ref`, UTC `DATETIME(6)`, stable readable
codes, foreign keys, uniqueness, CHECK constraints, and optimistic versions.
No migration seed data or destructive DDL exists. SQL inspection passes, but
runtime MySQL 8.4 validation is pending; database compliance cannot receive a
completed runtime PASS.

### Security standard compliance

Signed JWT signature/issuer/audience/time validation, server-side exact actor
mapping, explicit direct grants, safe errors, no actor header/DTO trust, no
wildcard/SYSTEM, no security-off path, context cleanup, and bounded logging are
covered by executable tests. The security gate remains FAIL until MySQL
constraints and authoritative persistence behavior execute on MySQL 8.4.

### Auditability compliance

ActorContext carries stable ActorRef/type, authentication method/time, optional
credential expiry, fresh execution ID, and bounded correlation. Authorization
decisions carry actor, exact capability, outcome/reason, evaluation time, and
actor/grant versions where available. Provisioning and lifecycle rows retain
source/reference, versions, and timestamps. No speculative Audit module or
table was created.

### Skill compliance

The configuration catalog follows the configuration-management skill; native
Spring resource-server properties remain native and only clock skew uses a
bounded Q-009-owned typed property. Correlation remains distinct from identity
per the observability skill. A new
`docs/skills/trusted-actor-authorization.md` records reusable, test-backed
security patterns and explicitly keeps real MySQL verification mandatory.

## Unresolved Compliance Gate

No confirmed standards violation is open. The unresolved item is verification:
the host lacks Docker/MySQL 8.4 and `kubectl`. This prevents the mandatory
database, infrastructure, and Kustomize evidence required for final PASS.
