# Q-009: Trusted Actor and Authorization Foundation

## Status

**APPROVED — Requirement V1**

| Gate | Result |
| --- | --- |
| Requirement Discovery | COMPLETE |
| Requirement Review | PASS / APPROVED — external Architect decision confirmed 2026-08-25 |
| Requirement Approval | APPROVED — V1 |
| Architecture | NOT STARTED |
| ADR | REQUIRED — NOT CREATED |
| Implementation Design | NOT STARTED |
| Implementation | NOT STARTED |
| Implementation Allowed | NO |

- Requirement ID: `Q-009`
- Approved version: `v1`
- Approved date: 2026-08-25
- Approval origin: explicit external Architect approval confirmed by the
  Product Owner during Approved Baseline Commit Preparation
- Approval review reference:
  `review/q-009/review-v1-requirement-20260825-180019/`
- Architecture phase: Phase 1
- Change type: cross-capability trusted actor and authorization foundation
- Prerequisite for: Q-008 Risk Case Foundation implementation
- Identity Authority: **OPEN DECISION**
- Authorization direction: capability-based server-side enforcement; any
  future role/group mapping remains an upstream policy input, not direct
  authorization proof
- ADR Required: **YES**
- Recommended ADR topic: Trusted Identity Boundary, ActorContext Ownership, and
  Capability Authorization Model

This approved Requirement V1 records business/security needs only. Approval
does not choose Spring Security, JWT, OAuth2, OIDC, session, API Gateway,
reverse-proxy identity, an identity vendor, Java contracts, persistence,
endpoints, or runtime wiring.

## 1. Background

Q-003 and the Phase 0.6 standards intentionally did not implement
authentication or authorization. Q-005 and ADR-007 implemented Request ID and
W3C trace correlation but explicitly prohibit using Request ID or Trace ID as
authentication, authorization, Audit actor, or business identity.

Q-007 and ADR-009 establish the BrokerOS Risk Core Domain model but do not
define identity or access control. Q-008 defines an opaque `ActorRef` semantic
for case provenance and requires every protected use case to obtain its actor
from a trusted ActorContext. Q-008 Implementation Design V4 is approved, but
implementation authorization remains blocked because the repository has no
trusted authentication, actor mapping, or authorization provider.

`ActorRef` is a domain reference. It is not evidence that a caller was
authenticated, is still active, owns a credential, or is authorized for an
operation. Q-009 establishes the minimum trusted foundation needed to turn an
external or internal authenticated principal into a BrokerOS actor and an
auditable authorization decision.

## 2. Existing Capability and Gap Analysis

### 2.1 Already Exists

| Capability | Repository evidence | Q-009 treatment |
| --- | --- | --- |
| Phase 1 modular monolith and adapter isolation | AGENTS.md, Q-003, Phase 0.6 architecture | Preserve one deployable and broker-neutral boundaries |
| Safe API/error foundation | `ApiResponse`, `ResultCode`, `BusinessException`, `GlobalExceptionHandler` | Reuse after separate implementation approval; no new API now |
| Request/Trace correlation | Q-005, ADR-007, `RequestCorrelationFilter`, observability Skill | Preserve as correlation only; never treat as identity |
| Sensitive logging standards | AGENTS.md, development standards, Q-005 guidance | Apply to credentials, tokens, claims, principals, and authorization context |
| Auditability obligation | AGENTS.md and Phase 0.6 architecture | Extend with trustworthy attribution requirements; do not create Audit module |
| Actor reference semantics | Q-008 Requirement/ADR-010/V4 Design | Reuse opaque broker-neutral ActorRef meaning; do not mistake it for authentication |
| Q-008 capability authorization needs | Approved Q-008 V4 named use cases | Use as the first consumer; do not implement Risk Case in Q-009 |

### 2.2 Need Improvement

- Define the difference between external identity, authenticated principal,
  BrokerOS ActorRef, ActorContext, authorization attributes, and Audit actor.
- Define where caller-controlled data stops and trusted server-side context
  begins.
- Define minimum human and non-human actor requirements.
- Define capability-based authorization, default deny, least privilege,
  server-side enforcement, and auditable decision requirements.
- Define broker-neutral mapping and integration constraints without selecting a
  technology or vendor during Requirement Discovery.
- Define how Q-008 and future capabilities consume trusted actor/authorization
  information without accepting identity fields in request DTOs.

### 2.3 Missing

Repository inspection found no:

- approved identity authority;
- Spring Security dependency or configuration;
- JWT, OAuth2, OIDC, session, API Gateway, or reverse-proxy identity contract;
- authenticated principal abstraction;
- principal-to-ActorRef mapping authority;
- trusted ActorContext owner;
- authorization policy source or decision provider;
- capability catalog, role mapping, or permission storage;
- human/service identity provisioning or lifecycle;
- service credential/trust mechanism; or
- verified security tests and failure behavior.

### 2.4 Out of Scope

The absence of these capabilities does not authorize Q-009 to implement a full
IAM platform, company directory, customer login, or any Q-008 upstream domain
provider. Section 5 is authoritative.

### 2.5 Open Decisions

The concrete identity authority, authentication mechanism, security framework,
mapping source, policy source, service identity mechanism, and runtime
availability model are not present in the repository and remain open for the
Architecture Gate.

## 3. Problem Statement

BrokerOS cannot currently answer, through a trusted server-side mechanism:

- who or what is calling;
- whether the principal was authenticated by an approved authority;
- which stable BrokerOS actor that principal represents;
- which named capability the actor may perform on the target/context;
- whether the decision was default-deny and least-privilege; and
- whether Audit can trust the attributed actor and authentication context.

Without Q-009, future modules could accept `X-Actor-Id`, `request.actorId`, a
username, Request ID, Trace ID, or a hard-coded `SYSTEM` value and present it as
trusted Audit attribution. They could also enforce roles inconsistently in
controllers, bypass authorization in internal paths, or permit operations when
the identity/policy provider is unavailable.

## 4. Goals

1. Establish a broker-neutral trust chain from authenticated principal to
   ActorContext, authorization decision, application use case, and Audit
   attribution.
2. Define the minimum ActorContext business/security semantics without
   designing a Java interface.
3. Define a minimal actor-type taxonomy that supports humans and accountable
   automation without a universal `SYSTEM` bypass.
4. Require capability-based, default-deny, least-privilege, server-side
   authorization for protected use cases.
5. Keep external roles/groups/claims behind a verified mapping boundary.
6. Make successful and denied security decisions attributable and auditable
   without storing secrets or full credentials.
7. Provide the trusted Actor/Authorization prerequisite consumed by Q-008 and
   future capabilities while preserving their domain ownership.
8. Keep identity authority and implementation technology open until the
   Architecture/ADR Gate.

## 5. Scope and Non Goals

### 5.1 In Scope

- authentication-boundary requirements for human and service principals;
- broker-neutral external-principal to ActorRef mapping requirements;
- trusted ActorContext semantics and ownership expectations;
- capability-based authorization requirements;
- default-deny, least-privilege, explicit decision, and server-side enforcement;
- trusted service/automation actor requirements;
- Audit attribution and security-decision context requirements;
- failure, unavailable-authority, spoofing, sensitive-data, and logging
  requirements;
- Q-008 consumption and provider-prerequisite separation;
- architecture and ADR questions that must be resolved after Requirement
  approval.

### 5.2 Non Goals

Q-009 does not authorize or define implementation for:

- Risk Case, Trading Account provider, Evidence provider, Decision, Rule Engine,
  Action, ActionOutcome, Account Control, or Execution Engine;
- MT4/MT5 authentication, MT4/MT5 Manager API, CRM authentication, broker SDK,
  dealer plugin, bridge, LP, or another vendor integration;
- customer login, customer KYC, IB/Agent login, employee directory, or a
  broker/company-specific organization model;
- full enterprise IAM, identity lifecycle administration, password management,
  organization directory, SSO administration portal, permission-management UI,
  access-request workflow, or entitlement certification;
- team/queue ownership, organization hierarchy, or Risk Case assignment rules;
- authentication or authorization for Kafka infrastructure;
- microservices, API Gateway deployment, service mesh, Flink, Python, or AI;
- Java interfaces/classes, SecurityFilter, Spring Security configuration,
  dependency addition, JWT/OAuth/OIDC/session implementation, database tables,
  migration, cache, topic/event, endpoint, controller, service, repository,
  Docker, Kubernetes, or CI changes;
- Q-008 Implementation Design V5 or Q-008 implementation authorization; or
- automatic creation/numbering of Trading Account, Evidence, Decision, Action,
  or ActionOutcome provider Requirements.

## 6. Functional Requirements

- **Q009-FR-001:** Every protected BrokerOS application use case shall receive
  a trusted server-produced actor context before accessing protected business
  data or performing a mutation.
- **Q009-FR-002:** The system shall authenticate the presented principal through
  an approved identity authority/trust boundary before creating a trusted
  ActorContext.
- **Q009-FR-003:** The system shall map an authenticated external/internal
  principal to one stable broker-neutral ActorRef through an authoritative
  mapping boundary.
- **Q009-FR-004:** The system shall make an explicit authorization decision for
  the named application capability and relevant target/context before the use
  case executes.
- **Q009-FR-005:** Absence of an explicit allow decision shall result in deny.
- **Q009-FR-006:** Request bodies, query parameters, caller-selected headers,
  Request ID, Trace ID, display names, usernames, and unverified claims shall
  not establish ActorRef or authorization.
- **Q009-FR-007:** The minimum actor types shall be `HUMAN` and `SERVICE`.
  `HUMAN` represents an authenticated individual; `SERVICE` represents a
  distinct non-human workload/automation identity with its own provenance and
  authorization. A generic privileged `SYSTEM` actor type is not part of the
  Foundation.
- **Q009-FR-008:** Scheduled work, automated detection, Rule Engine activity,
  maintenance, and internal service operations shall use a stable purpose-
  specific service ActorRef and explicit capabilities; they shall not use
  `actor = "SYSTEM"` as an authorization bypass.
- **Q009-FR-009:** The same authorization boundary shall apply to HTTP-triggered
  and internal application use cases; moving a call inside the process shall
  not bypass authorization.
- **Q009-FR-010:** Successful and denied protected operations shall expose
  sufficient trusted context for Audit attribution without exposing credentials
  or full identity-provider payloads.
- **Q009-FR-011:** Authentication, actor mapping, or authorization provider
  unavailability shall fail closed for protected operations.
- **Q009-FR-012:** Q-008 Risk Case use cases shall derive actor attribution only
  from Q-009 trusted context; Q-008 request DTOs shall not accept an Audit actor
  identity.

## 7. Security Requirements

- **Q009-SR-001:** Caller-supplied `X-Actor-Id`, `X-User-Id`, `X-Username`,
  `actorId`, `userId`, `username`, and equivalent fields are untrusted and shall
  not become ActorRef.
- **Q009-SR-002:** Any credential/assertion validation shall verify the trust
  properties applicable to the selected mechanism, including integrity,
  issuing authority, validity, intended recipient/context, and replay/freshness
  controls where applicable.
- **Q009-SR-003:** Forwarded identity from a gateway or reverse proxy shall be
  trusted only after an approved trust contract prevents direct/bypass access,
  authenticates the upstream hop, bounds accepted fields, and strips spoofed
  client values. No such contract currently exists.
- **Q009-SR-004:** Authorization shall be enforced server-side at the
  application boundary and shall not depend on hidden UI controls or
  client-side role checks.
- **Q009-SR-005:** Authorization shall follow least privilege and default deny.
  Global admin/superuser bypass is not part of the Foundation.
- **Q009-SR-006:** Authentication and authorization errors shall use safe,
  consistent API behavior and shall not reveal credentials, claims, policy
  internals, sensitive subject data, or protected-resource existence beyond the
  approved contract.
- **Q009-SR-007:** Passwords, secrets, tokens, full authorization/cookie
  headers, raw assertions, private keys, and complete identity-provider claims
  shall not be logged, returned, stored in Audit records, or placed in
  Request/Trace correlation.
- **Q009-SR-008:** Local/test behavior shall not install a permissive provider
  that can be enabled accidentally in production. Production must not start or
  enable protected capabilities with an always-allow/fake authority.
- **Q009-SR-009:** Changes to actor mapping and authorization policy, when those
  capabilities are later designed, shall be controlled and auditable.
- **Q009-SR-010:** Authentication, ActorContext, and authorization information
  shall be immutable for one use-case decision except through an explicit new
  trusted evaluation.

## 8. Trust Boundary Requirements

The required trust flow is:

```text
External or internal identity
        ↓
Authentication boundary
        ↓
Verified principal
        ↓
Authoritative actor mapping
        ↓
Trusted ActorContext
        ↓
Capability authorization decision
        ↓
Application use case
        ↓
Audit attribution
```

- **Q009-TR-001:** All data before successful authentication is untrusted.
- **Q009-TR-002:** Only a server-controlled trusted boundary may create the
  ActorContext consumed by application use cases.
- **Q009-TR-003:** Actor mapping shall be broker-neutral and shall not bind
  BrokerOS domain code to a CRM schema, employee table, broker database, MT4,
  MT5, or identity-vendor object.
- **Q009-TR-004:** External roles, groups, scopes, and claims shall remain
  untrusted authorization inputs until validated and mapped through approved
  BrokerOS policy.
- **Q009-TR-005:** Request ID and Trace ID shall remain correlation context only.
  They may accompany Audit attribution but shall never replace principal,
  ActorRef, or authorization decision.
- **Q009-TR-006:** The Risk Case/domain layer shall not depend directly on a
  chosen security framework, token type, identity vendor, or transport header.
- **Q009-TR-007:** A provider timeout, error, unavailable mapping, unknown actor,
  or indeterminate authorization decision shall deny protected access.

## 9. Actor Requirements

### 9.1 ActorRef

- ActorRef is a stable, opaque, broker-neutral BrokerOS identity reference.
- ActorRef is not a username, display name, email address, token subject,
  database credential, Request ID, or Trace ID.
- Mapping from external principal identity to ActorRef must be authoritative and
  auditable; exact storage/ownership remains an Architecture decision.
- Deactivated, unknown, ambiguous, or unmapped principals cannot receive a
  trusted actor context.

### 9.2 ActorContext semantics

The future ActorContext shall carry only the minimum trusted information needed
for one application decision, including:

- authenticated ActorRef;
- actor type;
- authentication source/trust-boundary reference;
- bounded authentication-context metadata needed to evaluate trust;
- validated authorization-relevant attributes, if required;
- Request ID/Trace ID as separate correlation values; and
- delegation/on-behalf-of context only if a later approved decision defines it.

It shall not contain reusable credentials, raw tokens, full external claim
sets, mutable display/profile data, or caller-selected actor fields. The exact
Java type and fields are Architecture/Implementation Design concerns.

### 9.3 Actor types

Foundation actor types are limited to:

- `HUMAN` — an authenticated individual mapped to a stable ActorRef;
- `SERVICE` — a distinct non-human workload, scheduled process, automation, or
  internal service identity with purpose-specific authorization.

`SYSTEM` is not selected as a third Foundation type because a single global
system identity would erase accountability and invite privilege bypass. Future
Architecture may propose a narrower distinction only with an approved business
need and without weakening service identity, provenance, and authorization.

## 10. Authorization Requirements

- **Q009-AZ-001:** BrokerOS application enforcement shall be capability-based.
  Each protected use case checks a named capability rather than accepting a
  caller-provided role as permission.
- **Q009-AZ-002:** External roles/groups may later map to BrokerOS capabilities,
  but the mapping source, lifecycle, administration, and storage require
  Architecture decisions and do not create an RBAC framework in this phase.
- **Q009-AZ-003:** Authorization decisions shall include actor, capability,
  target/context when applicable, allow/deny result, and sufficient policy
  provenance for audit and diagnosis.
- **Q009-AZ-004:** Deny is the default for missing policy, unknown capability,
  unknown actor, invalid target context, unavailable provider, or indeterminate
  decision.
- **Q009-AZ-005:** Controllers, DTOs, repositories, database records, UI flags,
  and adapters shall not decide or override business authorization independently.
- **Q009-AZ-006:** Capability checks shall be applied consistently to reads and
  mutations, including sensitive history/access operations.
- **Q009-AZ-007:** Service actors shall receive only explicit capabilities and
  shall not inherit universal privileges merely because they run inside
  BrokerOS.
- **Q009-AZ-008:** Break-glass, impersonation, delegation, role administration,
  policy-management UI, and organization-derived entitlements remain outside
  the Foundation unless explicitly approved later.

## 11. Audit Attribution Requirements

- **Q009-AA-001:** Audit attribution for a protected operation shall obtain
  ActorRef and actor type from the trusted ActorContext, never the request DTO.
- **Q009-AA-002:** Audit context shall be able to record who/what acted, the
  capability evaluated, operation, target, UTC time, authentication source,
  authorization outcome/provenance, and Request/Trace correlation where
  applicable.
- **Q009-AA-003:** Denied and security-relevant failed attempts shall provide
  safe auditable facts without copying credentials, raw tokens, or sensitive
  claims.
- **Q009-AA-004:** A service operation shall remain attributable to its stable
  service ActorRef. A generic `SYSTEM` label is insufficient.
- **Q009-AA-005:** Delegated or on-behalf-of operations, if later approved, must
  preserve both initiating and effective actor provenance; this Requirement
  does not define delegation behavior.
- **Q009-AA-006:** Q-009 defines trusted attribution requirements but does not
  create the Audit module, audit table, retention engine, or audit query API.

## 12. Q-008 Dependency

Q-008 depends on Q-009 for trusted actor and authorization context. Risk Case
create, assign, priority change, review start, resolve, reopen, Evidence/
Decision/Action association, note, close, cancel, history read, and other
protected operations must:

1. receive an authenticated ActorContext from the Q-009 boundary;
2. receive an explicit allow decision for the named capability;
3. derive ActorRef/Audit attribution server-side; and
4. fail closed without exposing or mutating case data when trust cannot be
   established.

No Risk Case request DTO may accept `createdBy`, `assignedBy`, `resolvedBy`,
`reopenedBy`, `actorId`, `username`, or another field that decides Audit actor.

Q-009 completion alone will not authorize Q-008 implementation. Q-008 also
requires authoritative Trading Account, Evidence, Decision, Action, and
ActionOutcome providers from their owning capabilities, followed by explicit
Q-008 implementation authorization.

## 13. Technical Constraints

- Preserve Java 21, Spring Boot 3.x, Maven, one repository, and one Phase 1
  modular-monolith deployable.
- Preserve broker, CRM, trading-platform, employee-directory, and identity-
  vendor neutrality.
- Keep identity/protocol/vendor details behind replaceable adapters and keep
  domain/application capability contracts independent from them.
- Add no dependency, source, configuration, endpoint, schema, table, migration,
  Kafka topic/event, Redis key, container, deployment, or CI change during
  Requirement Discovery.
- Do not assume Spring Security, JWT, OAuth2, OIDC, session, API Gateway, or
  reverse-proxy identity before Architecture/ADR approval.
- Continue using safe ApiResponse/GlobalExceptionHandler conventions in any
  future API design without pre-creating ResultCodes in this phase.
- Keep authentication credentials and secrets outside application source,
  committed configuration, logs, Audit payloads, and Review evidence.
- Evaluate any security framework/major dependency, trust protocol, authority,
  module boundary, policy persistence, or deployment integration against the
  ADR threshold before implementation.

## 14. Integration Constraints

- The external identity authority is an independently owned system and shall be
  reached only through an approved broker-neutral integration boundary.
- BrokerOS shall not read or write a CRM, employee, MT4, MT5, broker, or identity
  provider database directly to authenticate or authorize callers.
- Future external calls must define timeout, bounded selective retry, failure
  mapping, freshness, duplicate/replay behavior, partial failure, and safe
  observability.
- Authentication/authorization provider unavailability shall not produce an
  allow decision.
- Gateway/proxy-provided identity requires an explicit trusted-hop contract;
  forwarded headers alone are insufficient.
- Service-to-service and scheduled/internal execution require real service
  identity semantics even inside the Phase 1 monolith.
- Q-009 shall not own or implement Trading Account, Evidence, Decision, Action,
  ActionOutcome, Rule Engine, Account Control, or vendor execution providers.

## 15. Acceptance Criteria

### 15.1 Requirement Gate

1. The Requirement distinguishes ActorRef, authenticated principal,
   ActorContext, authorization decision, and Audit actor.
2. Repository evidence honestly records that no identity authority,
   authentication framework, ActorContext, or authorization provider exists.
3. Identity Authority remains `OPEN DECISION`; no security technology/vendor is
   selected during Requirement Discovery.
4. The required trust chain and caller-supplied identity prohibition are
   explicit.
5. The minimum actor types are `HUMAN` and `SERVICE`; generic `SYSTEM` bypass is
   prohibited.
6. Authorization direction is capability-based, default-deny, least-privilege,
   explicit, server-side, and auditable without implementing RBAC.
7. Q-008 dependency and server-side Audit attribution requirements are clear.
8. Provider prerequisites remain separately owned and receive no invented
   Requirement IDs.
9. ADR need is recorded as YES with a recommended topic, but no ADR is created
   or accepted.
10. All open Architecture decisions are listed honestly.
11. The Review Package and ZIP contain current bounded evidence.
12. No Architecture, Design, implementation, dependency, migration, API,
    infrastructure, stage, commit, or push change occurs.

### 15.2 Future Behavior Gate After Separate Approval

13. Unauthenticated, unmapped, denied, invalid, indeterminate, and unavailable-
    authority paths fail closed.
14. Caller identity fields, correlation IDs, raw roles/claims, and generic
    system actors cannot bypass authentication or authorization.
15. Human and service identities map to stable ActorRefs with auditable
    provenance.
16. Every protected application read/mutation receives consistent capability
    enforcement and trusted Audit context.
17. Secrets/tokens/full claims are absent from logs, API errors, persistence,
    Audit payloads, and Review evidence.
18. Focused security tests cover spoofing, default deny, least privilege,
    unavailable providers, direct/bypass paths, service actors, attribution,
    and correlation separation.

## 16. Deliverables

Current authorized deliverables:

- approved V1 `docs/requirements/Q-009-Requirement.md`;
- independent Q-009 V1 Requirement Review directory;
- honest Q-009 Requirement Discovery Lessons Learned;
- timestamped self-contained Requirement Review ZIP;
- explicit Gap Analysis, ADR determination, Open Decisions, Q-008 dependency,
  and future provider-prerequisite sequencing.

Not authorized: Architecture document, ADR draft/acceptance, Implementation
Design, Java, tests, dependency, security configuration, migration, table,
endpoint, Kafka/Redis, adapter, deployment, Q-008 change, Git stage, commit, or
push.

## 17. Verification Plan

- Re-scan repository dependencies, packages, configuration, and documentation
  for authentication, authorization, principal, actor, identity, JWT, OAuth2,
  OIDC, session, gateway, proxy, and Spring Security evidence.
- Verify Request/Trace correlation remains separate from identity and Audit
  actor.
- Verify all required sections, requirement IDs, Gap categories, trust flow,
  Q-008 dependency, ADR determination, Open Decisions, and Review files.
- Verify Q-008 V4 Design and every existing Q-007/Q-008 Review/ZIP remain
  unchanged.
- Verify no backend source/test/dependency/configuration/migration or deployment
  path changed.
- Run `git status`, `git diff --stat`, whitespace/static checks, high-confidence
  secret scan, protected-file hashes, and exact ZIP validation.
- Existing Maven tests may be run as an unchanged baseline; they are not Q-009
  behavior verification.
- Record Docker, Kubernetes, MySQL/Flyway, Redis, Kafka, and security runtime as
  `NOT APPLICABLE` to Requirement Discovery.

## 18. Risks

- A caller header or request field may be mistaken for trusted identity.
- Request ID/Trace ID may be reused as an actor despite ADR-007.
- Selecting a token/framework before selecting the identity authority may lock
  BrokerOS into a vendor without solving actor mapping.
- A role-first design may create broad privileges and inconsistent use-case
  enforcement.
- A generic `SYSTEM` actor may erase the identity of automated operations.
- Service operations inside the monolith may bypass the same authorization
  boundary applied to HTTP.
- Gateway identity may be spoofed if direct application access remains open.
- Tokens, credentials, full claims, or policy internals may leak into logs or
  Audit records.
- Q-009 may become a full IAM or omnibus Q-008 prerequisite capability.
- Completing Q-009 may be misreported as full Q-008 implementation readiness
  while authoritative domain providers remain absent.

## 19. ADR Evaluation

**ADR Required: YES**

Recommended topic:

```text
Trusted Identity Boundary, ActorContext Ownership, and Capability Authorization Model
```

Rationale: the later Architecture decision will select or define the identity
authority/trust integration, ActorContext ownership and dependency direction,
authorization model, runtime enforcement boundary, security dependency, failure
semantics, and Audit attribution contract. These are durable cross-capability
and runtime-security decisions that meet the repository ADR threshold.

No ADR was created or accepted in Requirement Discovery. Requirement approval
is now recorded; ADR work remains deferred to a separately authorized
Architecture Gate.

## 20. Future Q-008 Provider Prerequisite Sequencing

Q-009 solves only the Trusted Actor/Authorization prerequisite. It does not
solve or own Q-008 authoritative reference prerequisites:

- Trading Account;
- Evidence;
- Decision;
- Action; and
- ActionOutcome.

Those providers require future approved owning-capability Requirements and
explicit sequencing. This Requirement does not implement them, put them inside
Q-009, create Q-010/Q-011/Q-012, or assign any Requirement ID.

## 21. Open Decisions

1. **Identity Authority:** which real authority authenticates Phase 1 human and
   service principals? **OPEN**.
2. **Authentication Mechanism:** session, signed token, OAuth2/OIDC, gateway,
   reverse-proxy, or another approved protocol? **OPEN**.
3. **Security Framework/Dependency:** whether Spring Security or another
   mechanism is justified? **OPEN; ADR evaluation required**.
4. **Actor Mapping Authority:** where principal-to-ActorRef mapping is owned,
   stored, versioned, deactivated, and audited? **OPEN**.
5. **Service Identity:** how scheduled/internal services authenticate and how
   purpose-specific service ActorRefs are provisioned/rotated? **OPEN**.
6. **Capability Policy Source:** where capabilities and mappings are owned,
   stored, changed, versioned, and invalidated? **OPEN**.
7. **Authorization Context:** which target/resource attributes are required by
   the Foundation without coupling to Risk Case internals? **OPEN**.
8. **Availability/Freshness:** provider timeout, retry, cache, revocation, policy
   freshness, and outage behavior beyond fail-closed? **OPEN**.
9. **API Failure Contract:** exact unauthenticated/forbidden/indeterminate
   ResultCodes and resource-existence disclosure rules? **OPEN**.
10. **Operational Endpoint Policy:** required protection for health, info,
    OpenAPI, Swagger, and future operational endpoints? **OPEN**.
11. **Delegation/Impersonation:** whether on-behalf-of or break-glass semantics
    are required at all? **OPEN; excluded until explicitly approved**.
12. **Local/Test Strategy:** how tests obtain trusted contexts without a fake
    production fallback? **OPEN**.

## 22. Review Checklist

- [x] AGENTS.md, Q-003, Q-005, Q-007, Q-008, ADR-007/009/010, applicable
      Skills, and latest Q-008 prerequisite Review inspected.
- [x] Repository dependencies, source packages, configuration, and symbols
      inspected for authentication/security/identity capability.
- [x] Already Exists, Need Improvement, Missing, Out of Scope, and Open
      Decisions distinguished.
- [x] ActorRef is not misreported as Authentication or Authorization.
- [x] Identity Authority recorded as OPEN DECISION.
- [x] No Spring Security/JWT/OAuth/OIDC/gateway/session technology selected.
- [x] Caller-supplied identity and correlation-ID identity are prohibited.
- [x] ActorContext semantics are defined without a Java interface.
- [x] Minimum HUMAN/SERVICE actor types and service accountability analyzed.
- [x] Capability-based/default-deny/least-privilege direction is explicit.
- [x] Audit attribution and Q-008 dependency are explicit.
- [x] Provider prerequisites remain outside Q-009 and unnumbered.
- [x] ADR Required YES recorded; no ADR created.
- [x] Non-goals prevent Risk Case/provider/IAM/vendor/infrastructure expansion.
- [x] Review Package and ZIP prepared for Architect Review.
- [x] No Architecture, Design, implementation, dependency, migration, API,
      configuration, stage, commit, or push performed.

## 23. Requirement Gate

- Requirement Discovery: COMPLETE
- Requirement status: **APPROVED — V1**
- Architect Requirement Review: **PASS / APPROVED — external decision
  confirmed 2026-08-25**
- Ready for Architecture Gate: YES
- Architecture status: **NOT STARTED**
- Architecture Allowed in baseline preparation: **NO**
- ADR status: **REQUIRED — NOT CREATED**
- ADR creation/acceptance Allowed in baseline preparation: **NO**
- Implementation Allowed: **NO**

The Requirement Gate is complete. Q-009 Architecture/ADR analysis requires a
separate explicitly authorized task; this approval and baseline preparation do
not start Architecture, ADR, Design, or implementation.
