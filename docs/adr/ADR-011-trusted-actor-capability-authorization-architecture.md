# ADR-011: Trusted Actor and Capability Authorization Architecture

- Status: Accepted
- Date: 2026-08-26
- Architect approval date: 2026-08-26
- Approval origin: Explicit external Architect Review decision
- Requirement: Q-009 — Trusted Actor and Authorization Foundation
- Supersedes: None
- Depends on: ADR-009 and ADR-010

This ADR was accepted through the explicit external Architect Review decision
recorded on 2026-08-26. Q-009 Implementation Design V1 was subsequently
approved through a separate external Architect decision. Q-009 implementation
was explicitly authorized on 2026-08-26, verified on disposable MySQL 8.4.11
with the complete zero-skip regression and infrastructure gates in V9, and
received final Architect implementation approval in V10 on 2026-08-26.

## Context

BrokerOS Risk needs a trustworthy answer to two different questions: who is
acting, and whether that actor may perform a protected use case. The current
backend establishes request and trace correlation but does not authenticate
human or service identities, map them to a stable BrokerOS actor, or evaluate
business capabilities.

Q-008 requires stable actor references and capabilities such as
`risk-case:create`, but caller-supplied identities, framework principals,
external roles, correlation IDs, and raw token claims are not safe domain or
audit authorities. The product must also remain broker-neutral,
CRM-neutral, trading-platform-neutral, and replaceable at external integration
boundaries.

The architecture needs to support HTTP callers and internal scheduled work in
one Phase 1 modular monolith without inventing a universal `SYSTEM` bypass,
coupling the domain to Spring Security, or making BrokerOS a password platform.

## Decision

### Identity authority

Adopt a pluggable-hybrid identity model:

- trusted external authorities authenticate human credentials;
- BrokerOS does not own human passwords under Q-009;
- BrokerOS owns stable `ActorRef` identity, actor mapping and activation, and
  capability-policy semantics;
- provider integrations remain replaceable adapters;
- the concrete identity provider remains open.

### Trust pipeline

Authentication adapters translate successfully validated human or service
credentials into a BrokerOS-owned `VerifiedPrincipal`. That principal is not
yet a BrokerOS actor.

The Q-009 platform security capability maps the verified authority, subject,
and principal type to one stable, active `ActorRef`. A missing, ambiguous, or
inactive mapping fails closed.

Q-009 then creates an immutable, bounded `ActorContext` for that execution. The
context contains ActorRef, actor type, bounded authentication provenance and
approved policy attributes, with request/trace correlation kept semantically
separate. It contains no raw credential, complete claims, HTTP request,
framework object, mutable session, or durable cached effective permission set.

### Human and service identities

Human HTTP callers use externally issued credentials validated by an inbound
adapter. The application is stateless with respect to human login sessions.
JWT versus opaque/introspected tokens and OIDC login remain open.

Externally invoked services use a separately typed, purpose-specific service
credential or platform attestation. Internal schedulers use a server-controlled
bootstrap to resolve a pre-provisioned active SERVICE ActorRef and create a
fresh ActorContext per operation. They do not fabricate HTTP tokens, reuse a
human context, or use a generic `SYSTEM` identity.

### Authorization

Application authorization is capability-based, default-deny, explicit-allow,
and least-privilege. External roles or claims may become validated inputs to a
governed BrokerOS mapping, but are never direct application permissions.

Capabilities use canonical lowercase ASCII names in
`<capability>:<action>` form. Each application module owns its capability
catalog; Q-009 owns the syntax and decision boundary.

Every protected application use case explicitly invokes an authorization port
before protected data access or mutation. HTTP rules, controller checks, and
method annotations are defense in depth and are not the sole authority.

The decision returns explicit ALLOW or DENY plus safe reason category, policy
provenance/version, and UTC evaluation time. Missing or indeterminate evidence
never produces ALLOW.

### Framework boundary

Recommend Spring Security as the Servlet/HTTP authentication and coarse
security infrastructure adapter. Framework `Authentication`, `Principal`,
token claims, and `SecurityContext` are translated at the boundary and do not
enter application/domain contracts. Background work does not depend on a
Servlet security context.

Future implementation is expected to require
`spring-boot-starter-security` and likely
`spring-boot-starter-oauth2-resource-server`; the exact token-specific
dependency awaits the provider/token decision. No dependency is added by this
ADR.

### Context and audit rules

HTTP context is per request and is cleared at completion. Background context is
fresh per operation and passed explicitly. Raw ThreadLocal, executor, Kafka, or
cross-process propagation is not approved; asynchronous identity provenance
requires a later Requirement and decision.

Protected behavior and audit obtain ActorRef only from the trusted context.
Caller-supplied actor fields can neither override identity nor populate audit
actor. Raw credentials and full claims are not logged or persisted.

### Failure and operational rules

All authentication, mapping, and authorization paths fail closed. Invalid
credentials are unauthenticated; unmapped/inactive principals and insufficient
capability are denied; provider or policy unavailability is reported as a safe
dependency-unavailable outcome rather than as implicit allow.

Only minimum non-sensitive liveness/readiness may be public in production.
Sensitive Actuator and OpenAPI/Swagger surfaces are protected or disabled.

## Alternatives

### BrokerOS-owned password identity platform

Rejected. It expands Q-009 into password storage, recovery, MFA, credential
security, and identity lifecycle, and creates a new high-risk product boundary.

### Provider-specific identity and roles used directly by the application

Rejected. It couples business authorization and audit identity to a vendor,
CRM, token form, or organization model and makes provider replacement unsafe.

### Caller-provided actor identity

Rejected. Headers, request fields, query parameters, and correlation IDs are
spoofable and cannot establish trusted identity.

### Controller-only or URL-only authorization

Rejected. Internal callers, schedulers, and future adapters could bypass a
transport-only check. The application use-case boundary is the authoritative
enforcement point.

### Role checks as the application contract

Rejected. Role meaning varies by provider and organization. Stable business
capabilities create a narrower, testable contract; roles may only be governed
inputs to capability mapping.

### Generic SYSTEM superuser for background jobs

Rejected. It hides responsibility, defeats least privilege, and creates an
authorization bypass. Purpose-specific service actors use the normal decision
path.

### Spring Security types throughout the application/domain

Rejected. It creates framework coupling and cannot represent non-HTTP execution
cleanly. Spring Security stays an infrastructure adapter.

### Stateful BrokerOS server sessions as the default

Not selected for Phase 1. They add distributed session lifecycle and cache
concerns. Any later need for a browser/BFF session requires explicit scope and
architecture review.

## Consequences

### Positive

- BrokerOS business and audit identity remain stable across identity-provider
  changes.
- Credential security is delegated without delegating application capability
  semantics.
- Human, service, HTTP, and background paths share one downstream actor and
  authorization model.
- Application-level enforcement prevents controller-only bypass.
- Default deny, purpose-specific service actors, and trusted audit identity
  improve least privilege and accountability.
- Domain and application code remain independent of Spring Security and token
  formats.

### Costs and constraints

- BrokerOS must own durable actor mapping and capability policy state and their
  audited lifecycles.
- External identity availability, token freshness, revocation, and key rotation
  become explicit operational concerns.
- A future implementation needs security dependencies and careful integration
  tests.
- Mapping and policy unavailability must fail closed and can reduce service
  availability.
- Context lifecycle and asynchronous propagation require explicit discipline.

### Deferred decisions

- concrete provider and token/validation model;
- service credential mechanism;
- mapping and policy persistence/API design;
- broker/tenant/organization modeling;
- policy caching and invalidation;
- exact result codes and endpoint rules;
- asynchronous delegation/provenance.

## Compatibility and Impact

- Q-007 core domain remains unchanged.
- Q-008's approved ActorRef and capability expectations are supported
  conceptually but Q-008 remains parked and unauthorized for implementation.
- No REST contract, database schema, Kafka topic, Redis key, deployment object,
  source code, dependency, or configuration is changed by this ADR.

## Approval Boundary

ADR-011 is **Accepted**. No Q-009 Architecture V3 is required. Q-009
Implementation Design V1 is **APPROVED**, and Implementation Design V2 is not
required. Q-009 Implementation is authorized and implemented in the worktree,
with mandatory runtime verification and final Architect Implementation Review
complete. Q-009 is technically ready for Git commit; no commit is recorded by
this ADR. The concrete identity provider remains an open deployment/environment
input; the approved Implementation Design V1 records the runtime, service
identity, actor/policy persistence, provisioning, cache, and wiring decisions.
