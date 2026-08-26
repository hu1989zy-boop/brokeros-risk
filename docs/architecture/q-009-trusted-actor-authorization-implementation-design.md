# Q-009 Trusted Actor and Authorization Foundation Implementation Design

## Document Status

- Requirement: Q-009 — Trusted Actor and Authorization Foundation
- Requirement version: V1 — Approved
- Architecture: V2 — Approved
- ADR: ADR-011 — Accepted
- Implementation Design version: V1
- Status: **APPROVED**
- Architect Decision: **APPROVED — external decision confirmed 2026-08-26**
- Implementation Design V2 Required: **NO**
- Approval Review: `review/q-009/review-v5-implementation-design-approved-20260826-021833/`
- Implementation: **COMPLETE — FINAL ARCHITECT APPROVED 2026-08-26**
- Implementation Authorized: **YES — explicit authorization received 2026-08-26**
- Mandatory Runtime Verification: **PASS — V9**
- Architect Implementation Review: **APPROVED — V10**
- Ready for Git Commit: **YES**
- Date: 2026-08-26

This document remains the approved design authority. The implementation named
below was subsequently authorized, verified on disposable MySQL 8.4.11 with all
mandatory tests executing and zero skips, and received final Architect approval
in V10. No Git commit is implied by this document status.

## 1. Overview

Q-009 will add the smallest coherent trusted-actor and capability-authorization
foundation to the existing Spring Boot modular monolith. The runtime trust path
is:

```text
signed human bearer JWT
        ↓ validate signature / issuer / audience / time
VerifiedPrincipal
        ↓ authoritative MySQL mapping
active ActorRef + ActorContext
        ↓ explicit MySQL-backed capability decision
protected application use case
        ↓
trusted Audit attribution

registered in-process service identity
        ↓ trusted factory + active MySQL mapping
fresh service ActorContext
        └─────────────── follows the same authorization path ───────────────┘
```

Human requests use Spring Security OAuth2 Resource Server with signed JWTs.
The concrete identity-provider vendor remains deployment-supplied and open;
BrokerOS depends only on the validated issuer/subject contract. Phase 1 internal
automation uses registered, purpose-specific in-process service identities and
does not fabricate tokens or use a generic `SYSTEM` actor. Actor mappings and
direct actor-to-capability grants are stored in application-owned MySQL tables.
There is no role model and no authorization cache in Phase 1.

The security framework ends at the inbound adapter. BrokerOS application and
domain code receive immutable BrokerOS types and never depend on Spring
Security, JWT claims, servlet objects, transport headers, or an identity vendor.

## 2. Approved Inputs

The design is governed by:

- `docs/requirements/Q-009-Requirement.md`, approved Requirement V1;
- `docs/architecture/q-009-trusted-actor-authorization-architecture.md`,
  approved Architecture V2;
- `docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md`,
  Accepted;
- Q-007 core-domain architecture and accepted ADR-009;
- Q-008 Requirement, accepted ADR-010, and approved V4 Implementation Design;
- ADR-007 request/trace correlation and ADR-008 configuration management;
- `AGENTS.md`, `docs/skills/development-standards.md`, the configuration and
  observability skills, and applicable Lessons Learned;
- current backend evidence: Java 21, Spring Boot 3.5.16, one modular-monolith
  deployable, MySQL/Flyway/JDBC, `ApiResponse`, `GlobalExceptionHandler`, and
  request/W3C trace correlation, with no current security dependency or tables;
- current database target: MySQL 8.4; and
- official Spring Security servlet architecture and JWT resource-server
  documentation:
  <https://docs.spring.io/spring-security/reference/6.5/servlet/architecture.html>
  and
  <https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html>.

Approved architecture remains authoritative if an implementation detail in
this draft is rejected. No approved input is modified by this design.

## 3. Design Goals

1. Establish a fail-closed trust chain for human and service actors.
2. Produce one stable, broker-neutral `ActorRef` from an authoritative mapping.
3. Keep the domain/application boundary independent of Spring Security and JWT.
4. Require an explicit capability decision before protected use-case work.
5. Make missing, disabled, indeterminate, and unavailable states deny safely.
6. Provide trusted attribution without storing or logging credentials or full
   provider payloads.
7. Preserve the Phase 1 modular monolith and reuse JDBC, Flyway, MySQL, API,
   exception, configuration, and correlation foundations.
8. Make Q-008 a future consumer without implementing or modifying Q-008.
9. Keep provisioning deterministic, controlled, idempotent, and auditable
   without building an IAM administration product.
10. Produce testable contracts for spoofing, token validation, lifecycle,
    concurrency, and context isolation.

## 4. Non Goals

This design does not authorize or design:

- an identity-provider vendor selection, company directory, customer login,
  password store, SSO portal, or permission-management UI;
- sessions, form login, HTTP Basic, API keys, forwarded identity headers, API
  Gateway trust, mTLS, service mesh, or external service-to-service identity;
- roles, groups, hierarchy, break-glass, delegation, impersonation, resource-
  scoped/attribute-based policy, approval workflow, or entitlement review;
- a generic privileged `SYSTEM` actor or always-allow local/test provider;
- Redis authorization caching, Kafka security events, SIEM, or a new Audit
  module;
- Q-008 Risk Case implementation or changes to its approved design;
- Trading Account, Evidence, Decision, Rule Engine, Action, ActionOutcome,
  MT4/MT5, CRM, broker, frontend, Docker, or Kubernetes work; or
- production Java, Maven, YAML, Flyway SQL, configuration, or deployment changes
  in this design phase.

## 5. Package Structure

Q-009 remains a feature module inside the existing deployable. The proposed
root is `com.brokeros.risk.security`:

```text
com.brokeros.risk.security
├── domain
│   ├── ActorRef / ActorType / ActorStatus
│   ├── ExternalPrincipalKey / VerifiedPrincipal / AuthenticationMethod
│   ├── ActorContext / Capability
│   └── AuthorizationDecision / AuthorizationOutcome / AuthorizationReason
├── application
│   ├── ActorMappingService / AuthorizationGuard
│   ├── ActorProvisioningService
│   └── ServiceActorContextFactory
├── application.port
│   ├── ActorMappingPort / AuthorizationPort
│   ├── ActorContextProvider
│   └── SecurityProvisioningPort
├── infrastructure.authentication
│   ├── JwtVerifiedPrincipalAdapter
│   ├── ActorContextAuthenticationFilter
│   ├── BrokerOsAuthentication
│   └── safe entry-point / access-denied response adapters
├── infrastructure.persistence
│   ├── JdbcActorMappingAdapter
│   ├── JdbcAuthorizationAdapter
│   ├── JdbcSecurityProvisioningAdapter
│   └── row mappers
├── infrastructure.configuration
│   ├── SecurityConfiguration
│   └── JwtTrustProperties
└── interfaces.bootstrap
    └── SecurityBootstrapCommand
```

Names are proposed, not implemented contracts. The final implementation may
refine a name without changing ownership. No `common`, `utils`, `manager`, or
other dumping-ground package is introduced. Domain and application packages
must have no dependency on `org.springframework.security`, `jakarta.servlet`,
JWT provider types, HTTP requests, JDBC, or Spring configuration.

## 6. Component Model

| Component | Responsibility | Must not do |
| --- | --- | --- |
| JWT decoder/resource server | Validate signed human credential | Map roles/scopes directly to BrokerOS authorization |
| `JwtVerifiedPrincipalAdapter` | Translate only validated claims to a bounded BrokerOS principal | Store token or copy arbitrary claims |
| `ActorMappingPort` | Resolve one active principal mapping to one active actor | Auto-provision unknown callers |
| `ActorContextAuthenticationFilter` | Replace framework-only authentication with trusted BrokerOS authentication for one request | Authorize business capabilities |
| `ActorContextProvider` | Expose the already-trusted context at the interface boundary | Let application/domain read `SecurityContextHolder` |
| `AuthorizationPort` | Return explicit allow/deny using authoritative data | Return implicit allow or hide dependency failure as allow |
| `AuthorizationGuard` | Enforce the decision before protected work | Read HTTP or actor fields from DTOs |
| `ServiceActorContextFactory` | Create a fresh context for a registered internal service | Accept arbitrary actor IDs or create `SYSTEM` |
| Provisioning service/command | Idempotently establish actors, mappings, and grants | Run as a public management API |
| JDBC adapters | Implement mapping, lifecycle, and authorization against MySQL | Leak persistence entities into the domain/API |

The HTTP adapter authenticates first, then maps the principal before Spring's
request authorization completes. Business authorization is enforced again at
the application use-case boundary. Route protection is defense in depth, not a
replacement for a use-case capability check.

## 7. VerifiedPrincipal Design

`VerifiedPrincipal` is an immutable BrokerOS value produced only after the
selected authentication boundary succeeds. Its proposed semantic fields are:

| Field | Meaning | Rules |
| --- | --- | --- |
| `externalPrincipalKey` | Authoritative external/internal identity key | Required; exact, case-sensitive tuple |
| `principalType` | `HUMAN` or `SERVICE` | Chosen by trusted adapter, never caller role/claim |
| `authenticationMethod` | `SIGNED_JWT` or `TRUSTED_IN_PROCESS` | Stable enum code |
| `authenticatedAt` | UTC time the execution trust was established | Required; server-derived |
| `credentialExpiresAt` | JWT expiry when applicable | Required for JWT; absent for an in-process invocation |

`ExternalPrincipalKey` consists of exact `(issuer, subject, principalType)`.
Issuer and subject are nonblank and at most 255 characters each. No trimming,
case folding, Unicode normalization, display-name substitution, or partial-key
fallback is allowed. Human JWT keys use validated `iss` and exact `sub`.
Internal services use issuer `urn:brokeros:risk:internal-service` and a
registered lowercase service code as subject.

The value excludes raw tokens, credentials, passwords, authorization headers,
full claims, provider object types, roles, groups, scopes, emails, display
names, mutable profiles, capability sets, and an unrestricted attributes map.
Adding authorization-relevant identity attributes later requires a reviewed
contract; it is not hidden in a generic map.

## 8. Human Authentication Design

### 8.1 Runtime decision

Phase 1 human authentication uses Spring Security OAuth2 Resource Server with a
signed JWT bearer token. It is stateless: no HTTP session, form login, HTTP
Basic, remember-me, or application-managed password is enabled.

The concrete identity-provider vendor is **OPEN**. Each deployment supplies one
trusted issuer contract and its audience. BrokerOS relies on standards-based
validated claims, not vendor-specific Java types or role semantics.

### 8.2 Required validation

The decoder must validate:

- cryptographic signature using trusted issuer keys;
- exact configured issuer;
- at least one exact configured BrokerOS audience;
- expiration and not-before time;
- nonblank subject and bounded issuer/subject lengths; and
- a 60-second clock skew, represented as a BrokerOS-owned bounded duration
  configurable from 0 through 300 seconds.

`jwk-set-uri` may be configured explicitly alongside `issuer-uri` when the
deployment must avoid issuer discovery during startup; issuer validation still
remains mandatory. Algorithm acceptance uses the framework's safe issuer/JWK
contract and may be narrowed by an approved deployment contract. A real issuer,
audience, key, token, client secret, or private key is never committed.

Roles, groups, scopes, authorities, display names, email addresses, and custom
claims are not BrokerOS capability proof. `jti` is neither persisted nor logged
by the foundation. Token replay controls beyond expiry/audience/issuer are
provider/deployment responsibilities until an approved requirement defines
stateful token revocation.

### 8.3 Fail-closed startup and request behavior

The production security runtime has no `enabled=false` or always-allow switch.
Missing/invalid required issuer, audience, or owned clock-skew configuration
fails startup. Key/discovery failure also fails startup when discovery is the
configured trust mechanism. A later runtime key-refresh failure follows Spring
Security's bounded decoder behavior and rejects unverifiable tokens.

## 9. Service Authentication Design

### 9.1 Alternatives

| Alternative | Phase 1 decision | Reason |
| --- | --- | --- |
| OAuth2/JWT service credential | Deferred | Appropriate for a future external/distributed workload; no such boundary exists now |
| mTLS/workload identity | Deferred | Requires deployment and trust decisions outside the modular monolith |
| Registered trusted in-process service identity | **Selected** | Fits current scheduled/internal execution without fake network authentication |
| Static shared API key | Prohibited | Weak provenance, rotation, and leakage properties |
| Generic `SYSTEM` identity | Prohibited | Destroys accountability and creates a bypass |

### 9.2 Selected mechanism

An internal operation obtains a context only through
`ServiceActorContextFactory` using a code-owned registered service descriptor.
The caller cannot pass an `ActorRef`, arbitrary subject, or capability set. The
factory verifies that the descriptor is in the compiled allow-list and that an
active `SERVICE` principal mapping and actor exist in MySQL. It creates a fresh
execution context, after which the same `AuthorizationPort` evaluates explicit
database grants.

The code-owned registry establishes which service identities the application
is capable of asserting; MySQL remains authoritative for activation and
capabilities. Both must agree. Examples such as a future risk scheduler are
illustrative only; Q-009 provisions no identity for a capability that lacks its
own approved Requirement.

No in-process job calls the HTTP endpoint, fabricates a bearer token, reuses a
human context, or bypasses authorization. External service invocation, Kafka
consumer identity, mTLS, workload identity, or service JWTs require a later
integration design.

## 10. Actor Model

`ActorRef` is the canonical implementation of the opaque Q-008/Q-009 semantic:
a lowercase UUIDv4 string in canonical 36-character form. It is generated by
BrokerOS, stable, non-meaningful, broker-neutral, never recycled, and distinct
from the internal database `BIGINT id`. No second Q-008 actor identifier is
created.

Foundation actor types are only:

- `HUMAN`; and
- `SERVICE`.

Actor lifecycle status is only `ACTIVE` or `DISABLED`. There is no `SYSTEM`,
admin subtype, identity-provider role, organization hierarchy, or customer/
employee taxonomy. Disabled records remain stored for historical attribution.

## 11. Actor Mapping Design

BrokerOS owns an authoritative MySQL mapping from exact
`ExternalPrincipalKey` to one `ActorRef`. Resolution must:

1. query the exact binary-collated issuer, subject, and principal type;
2. require mapping status `ACTIVE`;
3. join the actor and require actor status `ACTIVE`;
4. require mapping type and actor type to match; and
5. return exactly one immutable actor result or fail closed.

Unknown, ambiguous, type-mismatched, disabled, or unavailable mappings never
produce an `ActorContext`. Database uniqueness prevents ambiguity; application
code still treats more than one result as a security integrity failure.

Human mapping is pre-provisioned. Authentication does not create or reactivate
an actor. The mapping adapter has no CRM, employee table, MT4/MT5, or provider-
specific schema dependency.

## 12. Actor Lifecycle

| Transition | Allowed effect | Authorization behavior |
| --- | --- | --- |
| Provision new actor | Create actor plus initial mapping/grants atomically | Available after commit |
| Add principal mapping | Attach a unique exact principal to an existing same-type actor | Available after commit |
| Disable mapping | Preserve mapping and history | New context creation denied |
| Reactivate mapping | Explicit controlled operation with optimistic version check | Available after commit |
| Disable actor | Preserve actor, mappings, and grants | Mapping and authorization deny |
| Reactivate actor | Explicit controlled operation; grants retain their recorded states | Authorization follows active grants |
| Grant capability | Insert/re-grant explicit actor capability | Available after commit |
| Revoke capability | Mark grant `REVOKED`; retain history | New decisions deny |

Hard deletion and cascading deletion are not supported. Actor/mapping/capability
changes use optimistic versions and controlled provisioning operations. A
future administrative API requires a separate Requirement and Audit design.

## 13. Actor Provisioning

Phase 1 selects **pre-provisioning**, not just-in-time provisioning. An
authenticated but unmapped human receives a generic forbidden response and no
database mutation.

Actors, principal mappings, and initial grants are established through a
controlled, offline, one-shot bootstrap interface that invokes the same
application provisioning service used by future trusted administration. It
accepts a deployment-supplied versioned manifest containing only external
principal keys, actor type, explicit capabilities, and an operator-controlled
provisioning reference. It contains no token, password, private key, client
secret, or credential.

The command:

- runs as a non-web application mode and is unavailable from normal HTTP;
- requires explicit invocation and an external manifest path;
- is not authorized merely by a Spring profile;
- validates the complete manifest before writing;
- uses one local transaction for an actor, mappings, and grants;
- is idempotent only when an existing record exactly matches the manifest;
- refuses conflicting actor types, mappings, identities, or capabilities;
- records bounded `provisioning_source` and `provisioning_ref` metadata; and
- reports safe counts/references without printing principal subjects.

Deployment authorization to run the command is an operational control. A
future online actor/policy-management capability needs its own Requirement,
authentication, authorization, Audit, approval, and rollback design.

## 14. ActorContext Design

`ActorContext` is immutable and scoped to one execution. Proposed fields are:

| Field | Rule |
| --- | --- |
| `actorRef` | Required trusted BrokerOS actor |
| `actorType` | `HUMAN` or `SERVICE` |
| `externalPrincipalKey` | Bounded authentication provenance; never exposed in API/log output |
| `authenticationMethod` | `SIGNED_JWT` or `TRUSTED_IN_PROCESS` |
| `authenticatedAt` | UTC server time |
| `credentialExpiresAt` | JWT expiry when present |
| `executionId` | New server UUID for each HTTP/internal execution |
| `requestId` | Optional HTTP correlation only |
| `traceId` | Optional active tracing correlation only |

It carries **no capability set**. Capabilities are resolved at each protected
use-case check so a context cannot retain stale authorization. It also excludes
credentials, raw claims, framework authentication, servlet requests, mutable
profiles, DTO actor fields, and delegation data.

For HTTP, a Spring adapter places a BrokerOS authentication containing the
trusted context in the request `SecurityContext`. An interface adapter obtains
it through `ActorContextProvider` and passes it explicitly to the application
use case. Application/domain code never reads `SecurityContextHolder` or a
ThreadLocal.

For background work, the service factory creates and returns a fresh context
that is passed explicitly. Context is never silently inherited by executors,
`CompletableFuture`, Kafka, Flink, or another thread. A new boundary must create
a new trusted execution context under an approved design.

## 15. AuthorizationPort Design

The application-owned `AuthorizationPort` has the semantic operation:

```text
decide(ActorContext actorContext, Capability capability)
    -> AuthorizationDecision
```

This is design notation, not Java implementation. The decision is immutable
and contains:

- `ALLOW` or `DENY`;
- ActorRef and capability evaluated;
- a safe stable reason code;
- UTC evaluation time; and
- the observed actor/grant version needed for security/Audit context.

Suggested safe reasons include `EXPLICIT_GRANT`, `ACTOR_INACTIVE`,
`CAPABILITY_NOT_GRANTED`, and `CAPABILITY_REVOKED`. Unknown capability and no
row both resolve to deny without revealing policy internals to callers.

An authorization dependency failure is not mislabeled as a normal decision.
It throws a distinct `SecurityDependencyUnavailableException`, which fails the
operation with a safe 503 response. `AuthorizationGuard.requireAllowed` turns a
`DENY` into an expected access-denied exception before protected repository or
mutation work.

Every protected HTTP and internal use case calls this port. Controller route
rules or UI visibility cannot substitute for it. Resource-scoped or ABAC input
is deferred; a later approved `AuthorizationRequest` may extend the port
without adding claims or policies to `ActorContext`.

## 16. Capability Model

A capability is an immutable exact string with format `<module>:<action>`.

- lower-case ASCII only;
- each segment starts with a letter and then uses letters, digits, or hyphens;
- each segment is 1–63 characters;
- total maximum is 127 characters; and
- validation regex:
  `^[a-z][a-z0-9-]{0,62}:[a-z][a-z0-9-]{0,62}$`.

Capabilities compare case-sensitively. Each owning module defines its own
constants next to its use cases. Q-009 validates and evaluates values but does
not create a central speculative catalog. External roles/scopes are never
treated as capabilities.

There is no role model in Phase 1. Direct actor grants are the minimum model.
A future role-to-capability policy may be added behind the resolver through a
separate Requirement without changing `Capability` or `ActorContext`.

## 17. Capability Persistence

Direct grants live in `security_actor_capability`. One `(actor_id, capability)`
row records the current `GRANTED` or `REVOKED` state and retains lifecycle
timestamps and provisioning provenance. Revocation updates the row; it does not
delete it. Re-grant is an explicit optimistic update.

Authorization executes an indexed join requiring both an active actor and a
granted assignment. Missing actor, disabled actor, missing row, revoked row,
unknown capability, or inconsistent data denies. Database unavailability is a
distinct unavailable failure and never an implicit allow.

## 18. Bootstrap Strategy

Flyway owns schema only. It creates no real actor, principal, or capability
data. After migration, the offline bootstrap command may provision deployment-
specific data through application services.

An empty database is valid and fail closed: health remains available, but no
protected operation can obtain a trusted actor. The deployment sequence is:

1. apply and validate Flyway migration;
2. configure issuer/audience/JWK trust externally;
3. prepare and approve a non-secret provisioning manifest;
4. invoke bootstrap explicitly under deployment controls;
5. verify safe counts and mapping/grant states; and
6. start/roll the protected web runtime.

The manifest is not a tracked application default. Replaying the identical
manifest is a no-op; drift or conflicting ownership fails without partial
writes.

## 19. Database Schema

The target is MySQL 8.4 with UTC `DATETIME(6)`, `BIGINT` internal primary keys,
snake-case names, stable readable enum codes, and no ordinal persistence.

### 19.1 `security_actor`

| Column | Proposed type | Constraints/purpose |
| --- | --- | --- |
| `id` | `BIGINT` | PK, auto increment, internal only |
| `actor_ref` | `CHAR(36)` ASCII binary | Not null, canonical UUIDv4, unique |
| `actor_type` | `VARCHAR(16)` ASCII binary | Not null, `HUMAN`/`SERVICE` check |
| `status` | `VARCHAR(16)` ASCII binary | Not null, `ACTIVE`/`DISABLED` check |
| `version` | `BIGINT` | Not null, default 0, optimistic concurrency |
| `provisioning_source` | `VARCHAR(32)` ASCII binary | Not null, controlled source code |
| `provisioning_ref` | `VARCHAR(128)` ASCII binary | Not null, bounded operation/manifest reference |
| `created_at` | `DATETIME(6)` | Not null, UTC |
| `updated_at` | `DATETIME(6)` | Not null, UTC |

### 19.2 `security_principal_mapping`

| Column | Proposed type | Constraints/purpose |
| --- | --- | --- |
| `id` | `BIGINT` | PK, auto increment |
| `actor_id` | `BIGINT` | Not null, FK to actor, delete restricted |
| `issuer` | `VARCHAR(255)` UTF-8 binary | Not null, exact external authority |
| `subject` | `VARCHAR(255)` UTF-8 binary | Not null, exact principal subject |
| `principal_type` | `VARCHAR(16)` ASCII binary | Not null, `HUMAN`/`SERVICE` |
| `status` | `VARCHAR(16)` ASCII binary | Not null, `ACTIVE`/`DISABLED` |
| `version` | `BIGINT` | Not null, default 0 |
| `provisioning_source` | `VARCHAR(32)` ASCII binary | Not null |
| `provisioning_ref` | `VARCHAR(128)` ASCII binary | Not null |
| `created_at` | `DATETIME(6)` | Not null, UTC |
| `updated_at` | `DATETIME(6)` | Not null, UTC |

### 19.3 `security_actor_capability`

| Column | Proposed type | Constraints/purpose |
| --- | --- | --- |
| `id` | `BIGINT` | PK, auto increment |
| `actor_id` | `BIGINT` | Not null, FK to actor, delete restricted |
| `capability` | `VARCHAR(127)` ASCII binary | Not null, exact validated value |
| `status` | `VARCHAR(16)` ASCII binary | Not null, `GRANTED`/`REVOKED` |
| `version` | `BIGINT` | Not null, default 0 |
| `provisioning_source` | `VARCHAR(32)` ASCII binary | Not null |
| `provisioning_ref` | `VARCHAR(128)` ASCII binary | Not null |
| `granted_at` | `DATETIME(6)` | Not null, UTC |
| `revoked_at` | `DATETIME(6)` | Null when granted; set when revoked |
| `updated_at` | `DATETIME(6)` | Not null, UTC |

No token, claim document, password, secret, email, display name, role, session,
authorization header, or framework authentication is persisted. The schema is
additive and application-owned.

## 20. Indexes and Constraints

The migration must define at least:

- primary keys on all `id` columns;
- unique `security_actor(actor_ref)`;
- unique `security_principal_mapping(issuer, subject, principal_type)`;
- unique `security_actor_capability(actor_id, capability)`;
- FK mapping/capability `actor_id` to actor `id` with delete restricted;
- index `security_actor(actor_type, status)`;
- index `security_principal_mapping(actor_id, status)`;
- index `security_actor_capability(actor_id, status)`; and
- optionally `security_actor_capability(capability, status)` only if the actual
  provisioning/query plan uses reverse lookup; otherwise omit it.

Use binary collations (`ascii_bin` for controlled ASCII fields and
`utf8mb4_bin` for external issuer/subject) so exact keys are not case-folded.
The migration must include enforced check constraints for allowed enum codes,
nonnegative versions, actor-ref shape, capability shape, and consistent
grant/revoke timestamps where MySQL 8.4 supports the expression.

The composite principal unique key fits the MySQL 8.4 InnoDB index limit with
the proposed lengths. Repository integration tests must prove this against the
real disposable MySQL target instead of relying on an in-memory substitute.

Concurrency uses database uniqueness plus optimistic updates of the form
`WHERE id = ? AND version = ?`, incrementing `version`. Duplicate mappings and
grants resolve deterministically through the unique constraint; concurrent
disable/grant/revoke detects zero updated rows and returns a safe conflict for
the controlled operation. No distributed lock is required.

## 21. Spring Security Adapter

The future servlet chain is stateless and default-deny:

1. existing server observation establishes trace context;
2. existing `RequestCorrelationFilter` establishes safe Request ID inside the
   observation and before Spring Security;
3. Spring Security bearer filter extracts and validates a JWT;
4. `ActorContextAuthenticationFilter`, ordered after bearer authentication and
   before authorization, creates `VerifiedPrincipal`, resolves an active actor,
   and installs `BrokerOsAuthentication` containing `ActorContext`;
5. request authorization permits only explicitly public endpoints and requires
   BrokerOS authentication otherwise; and
6. interface adapters pass the context explicitly to protected use cases,
   which perform capability authorization.

Public/operational route policy:

| Route | Policy |
| --- | --- |
| `/api/health` | Public; no actor mapping |
| `/actuator/health`, `/actuator/health/**` | Public for liveness/readiness; details remain hidden |
| `/actuator/info` | Authenticated active actor when exposed; production should prefer exposing health only |
| `/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**` | Never public in production; authenticated in an explicitly enabled non-production profile |
| `/error` | Available for safe framework error dispatch only |
| every other request | Authenticated by default; business use cases also require capability |

CSRF may be disabled only because this boundary is a stateless bearer API and
does not use cookie authentication. Request cache and session creation are
disabled. CORS is not invented under Q-009.

Authentication and access-denied handlers must write the existing
`ApiResponse<ErrorResponse>` shape because filter-chain failures do not pass
through MVC `GlobalExceptionHandler`. They preserve `X-Request-ID`, safe path,
and HTTP/result code but never include token/claim/policy detail.

## 22. Configuration Model

ADR-008 ownership rules apply:

### 22.1 Framework-owned native properties

- `spring.security.oauth2.resourceserver.jwt.issuer-uri` — required external
  trusted issuer;
- `spring.security.oauth2.resourceserver.jwt.audiences` — required BrokerOS
  audience list; and
- `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` — optional explicit
  key-set location when deployment avoids discovery.

These stay in the native Spring namespace and are not wrapped.

### 22.2 BrokerOS-owned property

`brokeros.risk.security.jwt.clock-skew` is one immutable `Duration`, default
`60s`, minimum `0s`, maximum `300s`, startup-bound and restart-required. It is
owned by Q-009 because it controls BrokerOS trust tolerance not exposed as the
selected native Boot property. It belongs in a cohesive immutable validated
Q-009 properties object; no broad `BrokerProperties` is created.

Configuration catalog entries must record owner, canonical name, environment
alias, type/unit, default/requiredness, profiles, sensitivity, validation,
source, restart behavior, and compatibility. Issuer, audience, and JWK URL are
externalized configuration; token/private-key/client-secret values are never
application settings or tracked defaults.

Profiles are not authorization. Test configuration uses a real test-only signed
JWT trust boundary; it does not disable security. Actuator `env` and
`configprops` remain unexposed.

## 23. Failure Semantics

New ResultCodes are stable external contracts and are added only with the
implementation approval. Proposed behavior:

| Condition | HTTP | Safe result | Disclosure rule |
| --- | ---: | --- | --- |
| Missing bearer credential | 401 | `AUTHENTICATION_REQUIRED` | No provider detail |
| Forged, malformed, expired, early, wrong issuer/audience token | 401 | `AUTHENTICATION_INVALID` | Same generic message |
| Authenticated but unknown/disabled/type-invalid mapping or actor | 403 | `ACTOR_ACCESS_DENIED` | Do not reveal which state |
| Explicit capability deny/missing/revoked | 403 | `AUTHORIZATION_DENIED` | Do not reveal target existence or policy internals |
| Mapping/authorization database unavailable | 503 | `SECURITY_DEPENDENCY_UNAVAILABLE` | Safe retry-neutral message; never allow |
| Unexpected security integrity error | 500 | `INTERNAL_ERROR` | Existing generic response |

Authorization occurs before loading a protected target when feasible, so 403
does not reveal whether the target exists. Normal authentication/authorization
failures do not produce client stack traces. Framework handlers and MVC
exceptions share the same `ApiResponse` contract.

## 24. Audit Integration

Q-009 does not create an Audit module. It supplies immutable trusted inputs for
future business Audit:

- ActorRef and actor type;
- authentication method/source reference;
- capability evaluated and allow/deny outcome;
- safe decision reason and evaluation time;
- request ID/trace ID as separate optional correlation; and
- target/business context owned by the consuming use case.

Successful business Audit records derive the actor only from `ActorContext`,
never from request DTO/header values. A service operation records its purpose-
specific service ActorRef. Mapping and capability administration remains
controlled through bootstrap provenance until a separately approved Audit/admin
capability exists.

Raw credentials, full claims, issuer/subject values, display data, and policy
internals are not copied into business Audit records. If a later Audit schema
needs bounded authentication provenance, its owner must approve that contract.

## 25. Security Logging

Phase 1 uses structured application security logs; it adds no SIEM, exporter,
or Prometheus metric requirement. Minimum event concepts are:

- authentication failure;
- actor mapping miss/disabled result;
- authorization denial;
- security dependency unavailable; and
- controlled actor/mapping/grant provisioning outcome.

Safe fields are event code, outcome, ActorRef only after mapping, capability,
safe reason, bounded provisioning reference, request ID, trace ID, and UTC
time. Before mapping, use at most a non-reversible keyed fingerprint if an
approved operational need and key lifecycle exist; otherwise log no subject.
The Phase 1 default is no principal fingerprint.

Never log bearer tokens, authorization/cookie headers, JWT payloads, full
claims, issuer/subject, email, display name, secrets, keys, manifest contents,
or protected response/body data. Repeated failures may be counted by log
aggregation; application metrics can be added only when concrete operational
requirements define cardinality and alerting.

## 26. Transaction Model

- Provisioning one actor with mappings and grants is one local MySQL
  transaction. Validation precedes writes; any conflict rolls back all writes.
- HTTP actor mapping is a short read-only operation before the business use
  case. It does not join a remote transaction.
- A capability decision is the first protected data operation inside the
  application use-case transaction when that use case has a transaction. A
  read-only use case uses a short read-only transaction.
- Authorization queries recheck actor status and grant status; `ActorContext`
  does not cache either state.
- Once an explicit allow is obtained, the current local transaction proceeds on
  that decision snapshot. A concurrent disable/revoke affects new decisions;
  long-running or external action execution must re-authorize at its own future
  execution boundary.
- No distributed transaction, distributed lock, retry-all policy, or
  cross-system write is introduced.

## 27. Caching Decision

**No security cache is required in Phase 1.**

Actor mapping and capability decisions read authoritative MySQL state. This
maximizes disable/revoke freshness and avoids inventing Redis key, TTL,
invalidation, negative-cache, outage, and multi-instance consistency rules.
Expected initial load does not justify a cache.

Metrics and query evidence must demonstrate a need before adding caching. A
future cache requires its own approved design for source of truth, bounded TTL,
revocation invalidation, stampede behavior, outage semantics, Redis key format,
and fail-closed behavior. No previous allow may be reused during an
authorization-provider outage under this foundation.

## 28. Q-008 Integration

Q-008 remains unchanged and unauthorized. When Q-008 and its upstream provider
prerequisites are separately authorized:

1. its interface/controller obtains Q-009 `ActorContext` server-side and does
   not accept actor fields;
2. it passes the context explicitly to the Risk Case application use case;
3. the use case calls Q-009 `AuthorizationPort` before case repository access
   or mutation;
4. the case domain receives only the canonical `ActorRef` needed for provenance
   and Audit; and
5. Q-008 never depends on Spring Security, JWT, external principal keys, or
   provider claims.

Q-009 implements the single canonical ActorRef semantic already reserved by
Q-008; Q-008 must not define a duplicate. No Core Domain compatibility change
is required.

Approved Q-008 capability examples (`risk-case:create`, `risk-case:assign`,
`risk-case:review`, `risk-case:resolve`, `risk-case:reopen`) remain owned by
Q-008. Before Q-008 implementation, a Q-008 integration-design follow-up must
map each final use case to one exact capability without changing Q-009. This
design does not expand the Q-008 catalog or authorize Risk Case work.

## 29. Dependency Plan

The later implementation adds only Spring Boot-managed dependencies:

- `spring-boot-starter-security`;
- `spring-boot-starter-oauth2-resource-server`; and
- `spring-security-test` in test scope.

The resource-server starter supplies the managed JWT/JOSE implementation. Do
not pin Nimbus or add a second JWT library directly. Existing JDBC, Flyway,
MySQL, validation, web, actuator, Micrometer tracing, and test foundations are
reused. No session store, identity-provider SDK, LDAP, Redis security library,
Kafka, gateway, mTLS, Testcontainers, or external IAM dependency is selected.

Implementation verification must inspect the Maven dependency tree for a
single Spring Security/JWT stack, managed versions, no duplicate JWT parser,
and no unexpected server/runtime dependency.

## 30. Flyway Plan

The next migration is:

`V2__create_security_actor_foundation.sql`

It is forward-only and additive. It creates exactly the three Q-009 tables,
keys, constraints, and indexes described above and inserts no identity or
authorization data. It does not edit `V1__initial_schema.sql`.

Verification must run against disposable MySQL 8.4 and prove:

- clean migration from empty schema;
- upgrade from current V1 baseline;
- Flyway validation/checksum and application restart;
- expected binary uniqueness and check/FK enforcement;
- safe query plans for mapping and authorization; and
- no destructive DDL/DML, broad lock/data movement, or unrelated schema.

Application rollback is compatible because the migration adds unused tables.
After any Q-009 data exists, rollback leaves tables/data intact. A correction
uses a later migration; V2 is never edited after application. Provisioning is
an application operation after migration, not Flyway seed data.

## 31. Testing Strategy

### 31.1 Unit tests

- exact/case-sensitive `ExternalPrincipalKey` validation;
- canonical ActorRef UUIDv4 validation and generation;
- actor/status/method enum codes;
- capability syntax and comparison;
- immutable `VerifiedPrincipal`, `ActorContext`, and decision invariants;
- authorization guard allow/deny/unavailable distinction; and
- service registry rejection of arbitrary identity and `SYSTEM` attempts.

### 31.2 Persistence integration tests

Use disposable MySQL 8.4, not H2, for migration/repository behavior:

- principal/grant uniqueness and exact binary collation;
- actor/mapping/type/status joins;
- grant/revoke/reactivate and actor disable behavior;
- optimistic version conflicts;
- concurrent duplicate mapping and grant/revoke/disable behavior;
- FK delete restriction and lifecycle history retention; and
- mapping/authorization dependency failure translation.

### 31.3 Authentication/security integration tests

Primary boundary tests generate an ephemeral test RSA key pair and signed JWTs,
configure the real resource-server decoder for the test public key, and send
bearer requests through the complete filter chain. Test actors/grants are
provisioned through controlled fixtures after Flyway migration. Tests vary
signature, issuer, audience, expiry, not-before, subject, and mapping state.

`spring-security-test` may support focused adapter/controller tests, but
disabling filters or installing an always-allow production bean is not the
primary strategy. No credential-like fixed production value is committed.

### 31.4 Regression tests

Preserve and extend the existing suite for:

- `/api/health` and Actuator health/probes;
- standardized `ApiResponse`/error bodies;
- `X-Request-ID` validation, response propagation, and MDC cleanup;
- W3C trace continuation and isolation;
- production configuration validation and actuator exposure; and
- Flyway V1→V2 and application context startup.

Protected not-found/method/error regression tests use a valid mapped signed JWT
where security now correctly precedes MVC routing.

## 32. Security Test Matrix

| Case | Setup/action | Expected result |
| --- | --- | --- |
| Spoof `X-Actor-Id` | Valid or no JWT plus forged header | Header ignored; never establishes actor |
| Spoof username/body actor | Caller-selected field | Ignored/rejected by DTO; never maps actor |
| Forged token | Invalid signature | 401 generic authentication error |
| Expired token | `exp` outside skew | 401 |
| Not-yet-valid token | `nbf` outside skew | 401 |
| Wrong issuer | Signed by otherwise valid key | 401 |
| Wrong audience | Valid signature/issuer | 401 |
| Missing subject | Valid signature otherwise | 401 |
| Unknown mapping | Valid human JWT | 403 generic actor access denied; no JIT row |
| Disabled mapping | Valid JWT, disabled mapping | 403 |
| Disabled actor | Valid mapping, disabled actor | 403 and authorization deny |
| Missing capability | Active context, no grant | 403; target not loaded |
| Revoked capability | Active context, revoked grant | 403 |
| Mapping DB unavailable | Valid JWT | 503; no context/allow |
| Authorization DB unavailable | Active context | 503; no protected work |
| Service identity misuse | Arbitrary descriptor/string/actor ref | Factory rejects; no context |
| Generic `SYSTEM` attempt | Type/subject/value request | Validation/provisioning rejects |
| Human context reused as service | Background call attempts reuse | Rejected by explicit factory contract |
| Context/capability mutation | Caller retains object/reference | Immutable; fresh evaluation required |
| Thread/context leakage | Sequential/concurrent HTTP and executor work | No SecurityContext/MDC/ActorContext leakage |
| Request/trace as identity | Valid correlation only | Never maps or authorizes actor |
| Duplicate mapping race | Concurrent same external key | One commit; other deterministic conflict |
| Disable versus mapping | Concurrent actor disable/request | no later request authorized after disable commit |
| Grant versus revoke | Concurrent same capability version | one update; stale writer conflict; no duplicate row |
| Public health | No JWT | health/probes work with hidden details |
| Protected default route | No JWT | 401 before application work |
| Non-production API docs | Valid/invalid JWT | available only under explicit profile and authentication |

## 33. Migration / Rollout Plan

1. Merge only after Architect approval and implementation authorization.
2. Implement and test domain/application contracts without activating routes.
3. Apply additive V2 schema and verify current application remains compatible.
4. Add provisioning command and validate empty-baseline fail-closed behavior.
5. Add resource-server dependencies/configuration and security adapters.
6. Supply issuer/audience/JWK configuration in each target environment.
7. Approve and run the environment bootstrap manifest.
8. Run authentication, mapping, authorization, regression, migration, and
   concurrency suites against MySQL 8.4.
9. Roll instances using normal deployment controls; health remains public and
   protected routes fail closed.
10. Verify safe security events, no sensitive logs, and no unknown mapping
    before enabling dependent protected features.

Rollback removes the application version/configuration but retains additive
tables and data. A bad migration is corrected forward. A bad grant/mapping is
disabled/revoked through the controlled provisioning operation; it is not
deleted or repaired by manual DDL.

## 34. Implementation Sequence

The later authorized implementation should use bounded tasks/commits:

1. `feat: add Q-009 security domain and application contracts`
   — values, ports, decisions, invariants, unit tests.
2. `feat: add Q-009 security persistence foundation`
   — V2 migration, JDBC adapters, MySQL integration tests.
3. `feat: add controlled Q-009 actor provisioning`
   — provisioning service/command, idempotence and conflict tests.
4. `build: add Spring Security resource server foundation`
   — managed dependencies and configuration contracts only.
5. `feat: map authenticated principals to actor context`
   — JWT adapter, filter, BrokerOS authentication/provider, safe failures.
6. `feat: enforce Q-009 capability authorization`
   — JDBC authorization, guard, use-case boundary test fixture.
7. `feat: add trusted internal service actor contexts`
   — code registry/factory and misuse tests.
8. `test: verify Q-009 trust and failure boundaries`
   — signed-JWT, concurrency, leakage, regression, dependency checks.
9. `docs: record Q-009 implementation evidence`
   — configuration catalog, applicable skill/lesson, final Review Package.

Commits are recommendations, not actions in this phase. Each must compile and
pass its focused tests; migration and security activation must not be hidden in
one unreviewable commit. Q-008 integration remains a later separately approved
change.

## 35. Acceptance Traceability

| Requirement / acceptance area | Approved architecture decision | Planned component | Verification |
| --- | --- | --- | --- |
| Q009-FR-001/TR-002 trusted context | Server-created immutable context | mapping filter, ActorContextProvider/factory | HTTP/internal context tests; spoof tests |
| Q009-FR-002/SR-002 authenticate authority | Pluggable hybrid inbound adapter | signed JWT Resource Server | forged/issuer/audience/time tests |
| Q009-FR-003/TR-003 stable mapping | BrokerOS authoritative mapping | actor/mapping tables and JDBC port | exact mapping, uniqueness, disabled tests |
| Q009-FR-004/AZ-001 explicit capability | Capability port at use-case boundary | AuthorizationPort/Guard | allow/deny and target-not-loaded tests |
| Q009-FR-005/AZ-004 default deny | No implicit allow | decision model/JDBC query | missing/unknown/revoked tests |
| Q009-FR-006/SR-001 reject caller identity | Identity originates at adapter | DTO/header-independent mapping | actor/header/username/request/trace spoof tests |
| Q009-FR-007 actor types | HUMAN/SERVICE only | enums and DB checks | invalid/SYSTEM persistence tests |
| Q009-FR-008/AZ-007 accountable services | Purpose-specific registered service | ServiceActorContextFactory | registry, grant, misuse, fresh-context tests |
| Q009-FR-009 same internal boundary | Internal callers use same port | explicit context + AuthorizationGuard | background allow/deny tests |
| Q009-FR-010/AA-001..005 Audit context | Trusted bounded attribution | context/decision output | field/content and sensitive-data tests |
| Q009-FR-011/TR-007 fail closed | unavailable is never allow | distinct unavailable exception | DB outage tests |
| Q009-FR-012 Q-008 dependency | Framework-neutral ActorRef/context | Q-009 ports; later Q-008 adapter | compile-architecture and future contract tests |
| Q009-SR-003 forwarded identity | No approved gateway contract | no header adapter | bypass/header negative tests |
| Q009-SR-004 server enforcement | use-case boundary enforcement | AuthorizationGuard | controller/internal-path tests |
| Q009-SR-005 least privilege | direct explicit grants, no superuser | capability table | missing/grant isolation tests |
| Q009-SR-006 safe errors | standardized generic responses | entry point/denied handler | response/no-disclosure tests |
| Q009-SR-007 secret safety | bounded principal/context/logging | adapters and log policy | static/log capture tests |
| Q009-SR-008 no permissive test path | real signed test trust | ephemeral test JWT strategy | production context/missing-config tests |
| Q009-SR-009 controlled policy change | offline provisioner/versioning | provisioning service/schema | idempotence/conflict/provenance tests |
| Q009-SR-010 immutable per decision | immutable values, fresh evaluation | records/value objects/port | mutation and concurrent revoke tests |
| Q009-AZ-002 roles as upstream only | no Phase 1 roles | direct actor grants | schema/dependency/static inspection |
| Q009-AZ-008 advanced auth deferred | no break-glass/delegation | absent from model | static/package/schema inspection |
| Operational/regression acceptance | preserve health/API/correlation/Flyway | route policy and safe handlers | existing plus extended integration suite |

All Q-009 functional, security, trust-boundary, actor, authorization, Audit, and
future behavior acceptance areas are represented. Detailed test names are
finalized during implementation without weakening the mapped assertions.

The following ID-level coverage closes the grouped-table shorthand and proves
that every normative Requirement identifier has an implementation and test
home:

| Requirement ID | Planned enforcement | Primary verification |
| --- | --- | --- |
| Q009-FR-001 | server ActorContext before protected use case | HTTP/internal boundary tests |
| Q009-FR-002 | signed JWT or trusted service factory before context | authentication/factory tests |
| Q009-FR-003 | exact authoritative MySQL mapping | mapping repository tests |
| Q009-FR-004 | explicit AuthorizationPort decision | guard/use-case tests |
| Q009-FR-005 | only `ALLOW` proceeds | missing/indeterminate tests |
| Q009-FR-006 | adapters ignore caller identity/correlation | spoof tests |
| Q009-FR-007 | HUMAN/SERVICE checks only | value and migration checks |
| Q009-FR-008 | registered purpose-specific service actor | service misuse tests |
| Q009-FR-009 | same port for HTTP/internal work | paired path tests |
| Q009-FR-010 | bounded context and decision provenance | Audit-contract tests |
| Q009-FR-011 | unavailable becomes safe failure | DB outage tests |
| Q009-FR-012 | future Q-008 consumes trusted context only | architecture/static contract tests |
| Q009-SR-001 | no actor identity header/DTO source | header/body spoof tests |
| Q009-SR-002 | signature/issuer/audience/time validation | signed JWT negative matrix |
| Q009-SR-003 | no forwarded-identity adapter | bypass/static inspection |
| Q009-SR-004 | AuthorizationGuard at application boundary | UI/controller bypass tests |
| Q009-SR-005 | direct least-privilege grants, no superuser | grant isolation tests |
| Q009-SR-006 | generic standardized security responses | disclosure response tests |
| Q009-SR-007 | bounded values/log fields only | static/log capture tests |
| Q009-SR-008 | no security-off/always-allow provider | prod startup/test wiring tests |
| Q009-SR-009 | controlled versioned provisioning | idempotence/conflict/provenance tests |
| Q009-SR-010 | immutable context/decision and reevaluation | mutation/revoke tests |
| Q009-TR-001 | bearer input untrusted until decoder succeeds | forged-token tests |
| Q009-TR-002 | only mapping filter/service factory creates context | construction/package tests |
| Q009-TR-003 | BrokerOS tables and ports only | dependency/static inspection |
| Q009-TR-004 | roles/groups/scopes excluded from authorization | claim-injection tests |
| Q009-TR-005 | request/trace remain optional correlation | correlation-as-identity tests |
| Q009-TR-006 | framework types stay in infrastructure | architecture dependency test |
| Q009-TR-007 | timeout/error/unknown/indeterminate deny | failure matrix |
| Q009-AZ-001 | exact module capability per use case | allow/deny tests |
| Q009-AZ-002 | no Phase 1 role schema/resolver | schema/static inspection |
| Q009-AZ-003 | bounded explicit decision fields | decision invariant tests |
| Q009-AZ-004 | default deny query/guard behavior | unknown/missing/outage tests |
| Q009-AZ-005 | one application authorization owner | architecture/static tests |
| Q009-AZ-006 | reads and mutations both guarded | paired use-case tests |
| Q009-AZ-007 | service has direct explicit grants only | service grant tests |
| Q009-AZ-008 | advanced authorization absent | API/schema/package inspection |
| Q009-AA-001 | ActorRef/type only from ActorContext | spoof/Audit-contract tests |
| Q009-AA-002 | bounded decision and correlation output | context/decision field tests |
| Q009-AA-003 | safe failure event facts only | denial/log capture tests |
| Q009-AA-004 | stable service ActorRef | service attribution tests |
| Q009-AA-005 | delegation absent until separately approved | model/static inspection |
| Q009-AA-006 | no Audit module/table/API | package/schema/API inspection |

## 36. Risks

| Risk | Mitigation |
| --- | --- |
| Vendor claims leak into domain | Translate only validated issuer/subject into bounded BrokerOS values |
| Broad or stale privilege | Direct explicit grants, no role expansion, no cache, recheck per use case |
| Unknown principal auto-enrollment | Pre-provision only; generic 403, no JIT writes |
| Service identity becomes bypass | Compiled registry plus active DB mapping/grants; no arbitrary IDs or SYSTEM |
| Security filter replaces application enforcement | Keep mandatory AuthorizationPort at each protected use case |
| Disable/revoke race | DB transaction snapshot, optimistic administration, fresh decision for new work |
| Provider/key outage | Fail startup/request closed; no cached implicit allow |
| Sensitive identity logging | Bounded safe event fields; no token, claim payload, issuer/subject |
| Configuration bypass | No security-off property; profiles are not authorization; fail-fast validation |
| Schema overgrowth | Three additive tables only; no role/session/token/admin schema |
| Bootstrap misuse | Non-web explicit command, deployment authorization, exact idempotence/conflict refusal |
| Q-008 coupling/duplication | One Q-009 ActorRef and framework-neutral ports; Q-008 remains unchanged |
| Test false confidence | Real signed JWT/filter chain and MySQL 8.4, not disabled security/H2 |

## 37. Open Decisions

The following are intentionally open without blocking foundation implementation
after approval:

1. Concrete human identity-provider vendor and environment-specific issuer,
   audience, and JWK locations.
2. Actual first human/service principals and grants in each deployment
   bootstrap manifest.
3. Future external service authentication (service JWT, mTLS, or workload
   identity) if a distributed boundary is approved.
4. Future online administration, role mapping, resource-scoped authorization,
   delegation, break-glass, or entitlement governance.
5. Future security caching or metrics/alerting, subject to measured need and
   separate freshness/cardinality decisions.
6. Final Q-008 exact capability-to-use-case matrix in its later authorized
   integration follow-up.

These values/features are outside the Phase 1 foundation contract. The runtime
mechanism, persistence, service identity strategy, dependency set, migration,
and fail-closed semantics are decided in this design.

## 38. Implementation Blockers

Implementation Design Complete: **YES**

The design resolves the previously open implementation-critical decisions:

- human runtime: signed JWT OAuth2 Resource Server;
- service runtime: registered trusted in-process identity plus active mapping;
- actor mapping persistence: BrokerOS MySQL tables;
- authorization persistence: direct actor-to-capability MySQL grants;
- provisioning: controlled pre-provisioning through an offline one-shot
  command;
- dependencies: Boot-managed Spring Security/resource-server/test support; and
- migration: additive Flyway V2.

No unresolved technical design blocker remains after Architect approval.
Concrete provider values and bootstrap data are deployment inputs required
before runtime rollout, not missing architecture.

Architect approval of Implementation Design V1, the approved Design Git
baseline, and explicit implementation authorization are complete. The
implementation is present in the worktree. V9 completed disposable MySQL 8.4.11,
zero-skip Maven, Docker Compose, Flyway, Kustomize, and Security verification;
V10 records final Architect implementation approval.

Q-009 Implementation Design Architect Decision: **APPROVED**

Q-009 Implementation Design V2 Required: **NO**

Q-009 Implementation Ready for Authorization: **COMPLETE**

Q-009 Implementation Authorized: **YES**

Q-009 Implementation Complete: **YES**

Q-009 Architect Implementation Review: **PASS / APPROVED — V10**

Q-009 Ready for Git Commit: **YES**

## 39. Final Required Decisions

| Decision | Result |
| --- | --- |
| Implementation Design Complete | **YES** |
| Human Authentication Runtime | Spring Security OAuth2 Resource Server with signed JWT, exact issuer/audience/time validation |
| Concrete Identity Provider Vendor | **OPEN** |
| Service Authentication | Registered trusted in-process service identity plus active DB mapping and explicit capabilities |
| Actor Mapping Persistence | Three-table BrokerOS-owned MySQL foundation; exact principal mapping to stable ActorRef |
| Actor Provisioning | Controlled pre-provisioning via offline idempotent one-shot command; no JIT |
| ActorContext Capability Strategy | No embedded capabilities; fresh AuthorizationPort evaluation per protected use case |
| Authorization Persistence | Direct actor-to-capability grants in MySQL with grant/revoke status |
| Role Model Required in Phase 1 | **NO** |
| Security Cache Required in Phase 1 | **NO** |
| Spring Security Dependencies | `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, test-scope `spring-security-test` |
| Database Migration Required | **YES — `V2__create_security_actor_foundation.sql` after approval** |
| Q-009 Implementation Design Architect Decision | **APPROVED** |
| Q-009 Implementation Design V2 Required | **NO** |
| Q-009 Implementation Ready for Authorization | **COMPLETE** |
| Q-009 Implementation Authorized | **YES — explicit authorization received 2026-08-26** |
| Q-009 Implementation Complete | **YES — V9 runtime verification PASS** |
| Q-009 Mandatory Runtime Verification | **PASS — MySQL 8.4.11; 58/58 Maven tests; zero skips; Compose/Flyway/Kustomize/Security PASS** |
| Q-009 Architect Implementation Review | **PASS / APPROVED — V10, 2026-08-26** |
| Q-009 Final Approval Review | `review/q-009/review-q-009-v10-final-architect-approval-20260826-144832/` |
| Q-009 Ready for Git Commit | **YES** |
