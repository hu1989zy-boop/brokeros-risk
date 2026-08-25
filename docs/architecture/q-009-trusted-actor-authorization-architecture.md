# Q-009 Trusted Actor and Authorization Foundation Architecture

## Document Status

- Requirement: Q-009 — Trusted Actor and Authorization Foundation
- Architecture version: V2
- Architecture status: APPROVED
- Architect decision: APPROVED — external decision confirmed 2026-08-26
- ADR: ADR-011 — Accepted
- Architecture V3 required: No
- Implementation Design: V1 — APPROVED — external decision confirmed 2026-08-26
- Implementation Design V2 required: No
- Implementation authorized: No
- Last updated: 2026-08-26

This document defines the approved Architecture V2 boundary for Q-009. It does not
select a concrete identity provider, define implementation classes or database
schema, or authorize implementation.

## 1. Purpose

Q-009 must ensure that application behavior uses only a server-established,
trusted actor identity and that protected use cases enforce explicit
capabilities. The foundation must support human and service actors without
coupling the BrokerOS Risk core to an identity vendor, an HTTP framework
principal, an external CRM, or caller-supplied identity fields.

The architecture separates five concerns:

1. an external or platform authority authenticates credentials;
2. an inbound adapter translates successful authentication to a
   `VerifiedPrincipal`;
3. BrokerOS maps the principal to a stable, active `ActorRef`;
4. BrokerOS creates an immutable per-execution `ActorContext`;
5. the application use-case boundary asks an authorization port for a
   capability decision before protected work.

## 2. Governing Inputs

The architecture was derived from:

- `docs/requirements/Q-009-Requirement.md`;
- the Q-007 core-domain architecture and ADR-009;
- the Q-008 Requirement, ADR-010, and approved V4 implementation design;
- `docs/skills/development-standards.md` and applicable repository skills;
- the Phase 1 modular-monolith, adapter, auditability, and security rules in
  `AGENTS.md`;
- the current backend baseline, which has request/trace correlation but no
  authentication, trusted-actor, authorization, or Spring Security foundation.

Q-008 remains parked. This architecture supplies a prerequisite architecture but
does not authorize or implement Q-008.

## 3. Scope

### 3.1 In scope

- identity-authority ownership and integration boundary;
- human and service authentication semantics;
- the conceptual `VerifiedPrincipal`, `ActorRef`, and `ActorContext` boundaries;
- actor mapping and lifecycle ownership;
- capability-based authorization semantics and enforcement point;
- Spring Security's permitted architectural role;
- failure, audit, operational-endpoint, context-propagation, and threat rules;
- future implementation and verification obligations.

### 3.2 Out of scope

- choosing or deploying Keycloak, Auth0, Okta, Azure AD, or another provider;
- password storage, login UI, MFA ceremony, account recovery, or user
  provisioning workflow;
- fixing JWT versus opaque tokens, OIDC login, mTLS, OAuth client credentials,
  or Kubernetes workload identity;
- Java types, Spring configuration, dependency edits, tables, migrations, REST
  endpoints, Kafka topics, Redis keys, Docker, or Kubernetes changes;
- tenant/broker organization modeling;
- the Audit module or an audit persistence implementation;
- Q-008 or Q-009 Implementation Design or Implementation;
- asynchronous message identity propagation.

## 4. Current-State Evidence and Gap

The backend is a Java 21 Spring Boot modular monolith. Its current request
boundary establishes request and trace correlation through servlet filtering
and MDC. Correlation answers which execution is being observed; it does not
answer who is trusted or what that actor may do.

Repository inspection found no Spring Security dependency or configuration, no
OAuth2 resource-server support, no authentication principal adapter, no actor
mapping, no `ActorContext`, and no capability decision boundary. Existing
`HttpServletRequest`, request-ID, trace-ID, and MDC data therefore cannot be
promoted into trusted identity.

This gap must be closed before Q-008 can trust an actor or authorize risk-case
mutations.

## 5. Architectural Model

```text
credential / platform attestation
               |
               v
    Inbound Authentication Adapter
               |
               v
       VerifiedPrincipal
               |
               v
     Actor Mapping Boundary ------> active stable ActorRef
               |
               v
          ActorContext
               |
               v
    Application Use-Case Boundary
               |
               +------> AuthorizationPort(capability, resource context)
               |                         |
               |                    ALLOW / DENY
               v                         |
      protected work only after ALLOW <--+
```

Authentication, actor mapping, and authorization are distinct decisions. A
valid external credential is not automatically a BrokerOS actor, and a mapped
actor is not automatically authorized.

## 6. Decision 1 — Identity Authority

### Decision

Use a **pluggable-hybrid identity model**.

- A trusted external identity authority authenticates human credentials.
- BrokerOS does not own human passwords under Q-009.
- BrokerOS owns its stable actor reference, identity mapping, actor activation
  state, and capability-policy boundary.
- Authentication providers are integrated through replaceable adapters.
- The specific provider remains open.

### Options considered

#### A. BrokerOS-owned identity authority

Rejected for Q-009. It would make BrokerOS responsible for credential storage,
password policy, recovery, MFA, account security, and identity lifecycle. That
is a materially larger product and security boundary than the Requirement.

#### B. Provider-owned identity and authorization semantics

Rejected as the whole model. It would couple BrokerOS business permissions and
stable actor identity to provider-specific subjects, roles, claims, or product
behavior.

#### C. Pluggable-hybrid

Selected. It delegates credential authentication while preserving
provider-neutral BrokerOS actor and capability semantics.

## 7. Decision 2 — Concrete Identity Provider

The concrete identity provider is **OPEN**. No provider, CRM, employee
directory, broker system, or trading platform is selected or implied. Provider
selection requires evidence for availability, issuer and audience validation,
revocation/freshness, machine identity, operations, and broker deployment
constraints.

## 8. Decision 3 — Human Authentication Model

Human callers present an externally issued access credential to an inbound
authentication adapter at the HTTP boundary. The adapter validates the
credential and produces a `VerifiedPrincipal`. Direct identity headers,
request-body actor fields, query parameters, and correlation IDs are never an
authentication source.

The Phase 1 application should remain stateless with respect to human login
sessions. Statelessness supports horizontal scaling, but it does not eliminate
credential revocation or freshness requirements. The token format remains
open:

- locally validated JWT can reduce synchronous provider calls but requires
  strict issuer, audience, signature, lifetime, and key-rotation handling;
- opaque-token introspection can provide central freshness/revocation semantics
  but adds provider availability and latency dependencies;
- an authority may introspect a JWT when immediate revocation is required.

OIDC interactive login, a browser session, and any BFF/UI flow are outside the
current architecture scope. BrokerOS Risk is presently treated as a protected
application API, not as the owner of the human login ceremony.

## 9. Decision 4 — Service Authentication Model

Human and service identities share the same downstream trust pipeline but use
distinct principal types and authentication adapters.

### 9.1 Externally invoked services

An external service must present a purpose-specific credential or platform
attestation validated by a service authentication adapter. The eventual
mechanism—mTLS, OAuth client credentials, workload identity, or another
approved method—remains open. A human token cannot silently become a service
identity, and a service credential cannot be treated as a human.

### 9.2 In-process scheduler and background execution

An in-process scheduler must not fabricate an HTTP token, reuse a caller
context, or claim a generic `SYSTEM` identity. A server-controlled trusted
service bootstrap boundary must:

1. resolve a pre-provisioned, purpose-specific service identity;
2. verify that its mapping is unique and active;
3. create a fresh `ActorContext` for each job operation;
4. request the required capability through the same authorization port;
5. fail closed if mapping, activation, or authorization cannot be established.

The bootstrap is infrastructure-controlled and inaccessible to caller-supplied
input. There is no universal superuser or implicit system bypass. Kubernetes
workload identity is deferred.

## 10. Decision 5 — VerifiedPrincipal Boundary

`VerifiedPrincipal` is a required BrokerOS-owned architectural contract at the
security/application boundary. It represents an identity that has passed an
authentication adapter but has not yet been mapped to a BrokerOS actor.

Its bounded conceptual data is:

- authority/issuer identifier;
- external subject identifier;
- principal type: `HUMAN` or `SERVICE`;
- authentication method, strength, and provenance;
- authentication and credential-validity/freshness metadata;
- only validated attributes explicitly approved as policy inputs.

It excludes raw credentials, full token claims, passwords, display profiles,
HTTP requests, provider SDK objects, and framework objects.

`VerifiedPrincipal` is not Spring Security `Authentication`, Java
`Principal`, a JWT claims map, or an HTTP header. An adapter translates those
protocol/framework forms into the BrokerOS contract. Application and domain
logic never depend directly on those forms.

## 11. Decision 6 — Actor Mapping Ownership

Q-009's platform security capability owns the authoritative mapping between an
external verified principal and a stable BrokerOS `ActorRef`. The mapping is
application-owned durable state and must not be derived through direct reads of
an external CRM, IdP, employee, broker, or trading-platform database.

The external identity key is conceptually the combination of authority,
subject, and principal type. Any future broker/tenant/entity dimension requires
an approved organization model and must not be invented by Q-009.

The mapping boundary must support:

- a stable, opaque BrokerOS actor identifier;
- unique and unambiguous mapping;
- explicit active/deactivated lifecycle;
- versioned, auditable mapping and policy changes;
- fail-closed behavior for missing, ambiguous, or inactive mappings.

No table, column, index, API, or migration is decided in this architecture
phase.

## 12. Decision 7 — ActorContext Ownership and Lifecycle

Q-009 owns the immutable per-execution `ActorContext`. It is created only after
a `VerifiedPrincipal` maps uniquely to an active `ActorRef`.

The context conceptually contains:

- stable `ActorRef` and actor type;
- bounded authentication method and provenance reference;
- approved validated policy attributes;
- request/trace correlation stored separately from identity semantics;
- an execution-context identifier.

It must not contain raw credentials, raw or complete claims, passwords, an HTTP
request, Spring beans, database connections, a mutable session, or a durable
cached set of all effective capabilities.

### 12.1 HTTP lifecycle

The infrastructure boundary creates one context for an authenticated request.
An application-facing provider exposes the trusted context to the transport
adapter, which passes it explicitly to the use case. The context is cleared at
request completion. Thread-local or framework security context is an
infrastructure mechanism, not an application/domain contract.

### 12.2 Background lifecycle

A background operation creates a fresh context through the trusted service
bootstrap and passes it explicitly to the use case. It does not inherit context
from a previously used thread.

### 12.3 Async propagation

Q-009 does not define propagation through executors, Kafka, Flink, or other
process boundaries. Raw thread-local copying and raw credential propagation are
prohibited. A later asynchronous contract must define provenance,
re-authentication or trusted reconstruction, expiry, replay protection, and
audit semantics under its own Requirement and architecture decision.

Domain models receive an `ActorRef` only where business/audit semantics require
it; they do not receive the security context.

## 13. Decision 8 — Capability-Based Authorization

The application authorization contract is capability-based, default-deny, and
explicit-allow. Application use cases ask for a named capability and do not ask
whether an actor has a provider-specific role.

Possible future policy inputs may include:

- direct ActorRef-to-capability grants;
- BrokerOS-owned group/role-to-capability mappings;
- approved mappings from validated external attributes to capabilities.

An external role or claim is never direct proof of permission. It must be
translated by an explicit, governed BrokerOS policy.

### 13.1 Capability ownership and naming

Each application capability/module owns its business capability catalog.
Q-009 owns canonical syntax and the evaluation contract, not a speculative
global catalog.

Canonical names use lowercase ASCII, kebab-case segments, and the form
`<capability>:<action>`. They are case-sensitive stable contracts. The Q-008
examples already approved are:

- `risk-case:create`
- `risk-case:assign`
- `risk-case:review`
- `risk-case:resolve`
- `risk-case:reopen`

### 13.2 Decision result

The conceptual authorization port receives the trusted context, required
capability, and only bounded resource context needed by policy. It returns an
explicit `ALLOW` or `DENY` plus a safe reason category, policy provenance or
version, and UTC evaluation time. Missing, unavailable, ambiguous, or
indeterminate inputs produce no allow decision.

## 14. Decision 9 — Authoritative Enforcement Point

The authoritative authorization point is the application use-case boundary,
before protected data is loaded or mutated. Each protected use case explicitly
invokes the authorization port with its required capability.

HTTP route rules, controller checks, filters, and Spring method annotations may
provide defense in depth, but none is the sole authority. This prevents an
internal caller, scheduler, or future transport adapter from bypassing a
controller-only check. Domain models enforce business invariants, not caller
authentication.

For future Q-008 work the intended sequence is:

1. HTTP infrastructure authenticates and establishes a trusted context;
2. the controller translates transport and passes the context to the Risk Case
   application use case;
3. the use case checks the applicable `risk-case:*` capability before repository
   or upstream-provider access;
4. the use case derives `ActorRef` from the context for domain and audit data;
5. caller-supplied actor identity is ignored and cannot override the trusted
   actor.

This sequence is an architectural prerequisite only. Q-008 implementation is
not authorized by this document.

## 15. Decision 10 — Spring Security Boundary

Spring Security is **recommended** as the Servlet/HTTP authentication and
coarse security infrastructure adapter. Its filter chain and framework
`SecurityContext`/`Authentication` can validate or carry the transport
principal, then translate it to BrokerOS contracts.

Spring Security must not leak into application or domain types. Method security
may be used later as defense in depth; it is not the only use-case authorization
mechanism. Background jobs do not depend on a servlet `SecurityContext`.

The recommendation is consistent with Spring Security's official Servlet
architecture, where security filters authenticate before authorization, and
with its resource-server support for bearer JWT and opaque tokens. The exact
configuration remains an Implementation Design decision after architecture
approval.

## 16. Decision 11 — Future Dependencies

New security dependencies are expected in a later, separately authorized
implementation:

- `spring-boot-starter-security` for the recommended HTTP security boundary;
- likely `spring-boot-starter-oauth2-resource-server` for a bearer-token adapter.

JWT-specific JOSE support is required only if JWT local validation is selected;
opaque introspection has different operational and dependency needs. The exact
token dependency cannot be fixed before the authority and token-validation
model are decided.

No dependency is added by this architecture work.

## 17. Failure Semantics

All paths fail closed. Future API contracts must retain the repository's
`ApiResponse` and `GlobalExceptionHandler` standards without leaking identity
or policy details.

| Condition | Architectural outcome | Intended HTTP category |
| --- | --- | --- |
| Missing, malformed, expired, or forged credential | Unauthenticated; no `VerifiedPrincipal` or `ActorContext` | 401 |
| Authenticated principal is unmapped, ambiguous, or inactive | Denied before business access; mapping details hidden | 403 |
| Active actor lacks capability | Denied before protected lookup or mutation | 403 |
| Identity, mapping, or authorization authority is unavailable/indeterminate | Fail closed and identify dependency unavailability, not bad credentials | 503-like |
| Unexpected internal security error | Fail closed with safe internal error | 500-like |

Authorization must occur before resource lookup when practical so denial does
not disclose whether a protected resource exists. Responses and logs must not
contain raw credentials, full claims, sensitive identity attributes, policy
internals, or stack traces.

## 18. Auditability Boundary

Audit actor identity comes only from the trusted `ActorContext`. Q-009 supplies
the following conceptual evidence to protected application behavior:

- ActorRef and actor type;
- bounded authentication source, method, and provenance;
- requested capability and allow/deny outcome;
- policy provenance/version and evaluation time;
- separate request/trace/execution correlation.

No caller-provided actor field may become audit identity. No raw token or full
claims are persisted. Q-009 defines trusted inputs but does not invent or own an
Audit module or its persistence. Authentication failures and denied decisions
must be safely observable; when no ActorRef exists, only a non-sensitive,
bounded authority/subject fingerprint may be recorded under a future approved
audit design.

## 19. Operational Endpoint Policy

Unauthenticated production access is limited to the minimum liveness/readiness
surface required by the deployment platform, with no sensitive details.
Actuator information, metrics, environment/configuration endpoints, and
OpenAPI/Swagger surfaces must be protected or disabled in production. Exact
paths and deployment configuration are deferred to Implementation Design.

## 20. Threat Analysis

| Threat | Required architectural control |
| --- | --- |
| Spoofed actor header/body/query value | Identity accepted only from a validated authentication adapter and actor mapping |
| Forged credential | Cryptographic/provider validation with issuer, audience, validity, and method controls |
| Stale or revoked credential | Explicit freshness/revocation strategy; fail closed when status cannot be established where required |
| Privilege escalation | Default deny, least privilege, explicit governed capability grants |
| Role/capability confusion | External roles/claims are inputs to explicit mapping, never direct application permission |
| Service impersonation | Separate service principal type, purpose-specific credentials and ActorRef |
| Generic `SYSTEM` bypass | No universal system actor or implicit allow; jobs use the normal authorization port |
| Controller-only authorization bypass | Authoritative check at every protected application use-case boundary |
| Thread/context leakage | Immutable per-execution context, explicit passing, cleanup, no raw thread-local propagation |
| Audit actor spoofing | Audit ActorRef derives only from trusted context |
| Identity-provider outage | Defined fail-closed unavailable outcome; no cached implicit allow |
| Resource-existence disclosure | Authorize before protected lookup and return safe denial semantics |

## 21. Q-008 Prerequisite Impact

Approved Q-008 design already reserves an `ActorRef` and capability checks, but
the current codebase has no trusted source for either. An eventual approved
Q-009 implementation can supply that prerequisite without changing Q-008's
business design.

Q-009 alone does not make Q-008 implementation-ready. Trading Account,
Evidence, Decision, Action, and ActionOutcome provider contracts remain
separate prerequisites, and Q-008 still needs its own explicit implementation
authorization.

## 22. Future Verification Strategy

Implementation Design must define tests for at least:

- adapter translation from valid authentication to `VerifiedPrincipal`;
- rejection of invalid, expired, wrong-issuer, wrong-audience, or forged
  credentials;
- unique active actor mapping and missing/ambiguous/inactive mapping denial;
- separation of HUMAN and SERVICE identity;
- fresh background service context and absence of a `SYSTEM` bypass;
- default deny and explicit capability allow;
- denial before protected repository/upstream access;
- inability of caller actor fields or headers to influence trusted actor/audit;
- context cleanup and no cross-request/thread leakage;
- fail-closed provider and policy outages;
- protected operational endpoints and non-sensitive error/log behavior;
- Q-008 capability integration contracts when Q-008 becomes authorized.

No tests or implementation are created in this architecture phase.

## 23. Open Decisions Before Implementation

This section records the inputs that were open at the Architecture V2 gate.
Approved Implementation Design V1 resolved the Foundation runtime, service
identity, persistence, provisioning, cache, failure, and wiring decisions. The
concrete provider remains an open deployment input; organization and
asynchronous/delegation concerns remain deferred outside the Foundation.

The Architecture V2 inputs were:

1. concrete identity provider and deployment ownership;
2. issuer, audience, token form, validation, revocation, and key-rotation model;
3. service authentication mechanism per invocation environment;
4. actor-mapping lifecycle/provisioning administration;
5. policy storage, caching, invalidation, and change-audit mechanism;
6. organization/broker/tenant boundary, if separately required;
7. exact API result codes and operational endpoint rules;
8. persistence schema and Flyway migration;
9. asynchronous provenance and delegation semantics.

None of these gaps authorizes a provider-specific assumption.

## 24. Approval and Readiness Decisions

| Decision | Status |
| --- | --- |
| Architecture V2 approved | YES — external Architect decision confirmed 2026-08-26 |
| Architecture V3 required | NO |
| ADR-011 accepted | YES |
| Concrete identity provider selected | NO — OPEN |
| Implementation Design ready | YES |
| Implementation Design status | V1 — APPROVED — external Architect decision confirmed 2026-08-26 |
| Implementation Design V2 required | NO |
| Implementation ready for Authorization Gate | YES |
| Implementation authorized | NO |
| Q-008 implementation authorized | NO |

## 25. Official Framework References

- Spring Security Servlet authentication:
  https://docs.spring.io/spring-security/reference/servlet/authentication/
- Spring Security Servlet architecture:
  https://docs.spring.io/spring-security/reference/servlet/architecture.html
- Spring Security authorization:
  https://docs.spring.io/spring-security/reference/servlet/authorization/
- Spring Security method security:
  https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
- Spring Security opaque-token resource server:
  https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/opaque-token.html

## 26. Approval Record

- Q-009 Requirement V1: APPROVED
- Q-009 Architecture V2: APPROVED
- ADR-011: ACCEPTED
- Architect decision origin: explicit external Architect Review decision
  supplied by the Product Owner
- Decision recorded: 2026-08-26
- No Q-009 Architecture V3 is required.
- The concrete provider remains an open deployment/environment input; approved
  Implementation Design V1 resolves the Foundation runtime decisions.
- Q-009 Implementation Design V1: APPROVED
- Q-009 Implementation Design V2 Required: NO
- Q-009 Implementation: NOT STARTED
- Q-009 Implementation Authorized: NO
- Q-008 Implementation Authorized: NO
