# Q-008 Implementation Prerequisite Architecture Analysis

## Executive Decision

| Required decision | Result |
| --- | --- |
| Decision 1 — Can Actor/Authorization be satisfied by Q-007? | **NO** |
| Decision 2 — Can authoritative reference providers be satisfied by Q-007? | **NO** |
| Decision 3 — Can Q-008 proceed with implementation without a new Requirement? | **NO** |
| Decision 4 — Is Q-009 recommended? | **YES** |
| Decision 5 — What must happen before authorization is YES? | Complete the trust and owning-provider prerequisites, verify fail-closed runtime wiring, then record a separate explicit Architect implementation authorization |

Q-007 supplies important semantic boundaries, but it is explicitly a
documentation-only design baseline. A semantic definition is not a runtime
identity authority or authoritative provider.

## 1. Actor / Authorization Analysis

### Repository evidence

- `docs/requirements/Q-007-Requirement.md` states that Q-007 does not authorize
  Evidence, Decision, Action, Risk Case, Audit, or RBAC implementation and says
  not to infer authorization.
- `docs/architecture/phase-0.6-development-standards.md` states explicitly that
  authentication and authorization are not implemented in Phase 0.6.
- `backend/pom.xml` contains no `spring-boot-starter-security` or Spring
  Security dependency.
- `backend/src/main/java/com/brokeros/risk` contains only application, API,
  configuration, exception, health, and observability packages. It contains no
  security, identity, actor, or access-control package.
- Repository-wide symbol inspection found no `ActorContext`,
  `AuthorizationPort`, `SecurityFilterChain`, `Authentication`,
  `SecurityContextHolder`, or equivalent production abstraction.
- `backend/src/main/java/com/brokeros/risk/observability/RequestCorrelationFilter.java`
  accepts/generates `X-Request-ID`, but `backend/README.md`, ADR-007, and
  `docs/skills/observability-correlation.md` explicitly prohibit using Request
  ID or Trace ID as identity, authorization, or audit actor.

### Decision

Q-007 cannot satisfy this prerequisite: **NO**.

The approved Q-008 V4 requirement for a non-spoofable `ActorContext` and an
authorization decision remains correct. Caller-supplied identity, hard-coded
system actors, request/trace correlation identifiers, and fake authentication
providers remain prohibited.

### Required production ownership

A new cohesive platform capability must own authentication integration,
principal-to-ActorRef mapping, and authorization decisions. It must remain
outside the Risk Case domain. A future Requirement must decide:

- the real identity authority and authentication protocol;
- whether Spring Security or another approved mechanism is used;
- trusted human and service/system actor semantics;
- capability-based authorization and default-deny behavior;
- unauthenticated, forbidden, and unavailable failure contracts;
- which operational endpoints are protected;
- audit-safe actor attributes and sensitive-claim/log handling; and
- dependency/ADR impact of any new security library or external identity
  integration.

## 2. Evidence Provider Analysis

Q-007 defines Evidence as traceable support for or against a Decision and
places its semantics in the Decision Core Domain. ADR-009 requires provenance.
It creates no Evidence entity, repository, API, published contract, or runtime
provider. The backend contains none.

`EvidenceReferenceQuery` may be a valid Q-008 consumer port, but it can only be
wired to a real provider owned by an approved Evidence/Decision capability. It
must verify existence and return only safe provenance needed by Risk Case. It
must distinguish not-found from provider-unavailable and must never use an
always-true or opaque-string fallback.

Provider available: **NO**.

## 3. Decision Provider Analysis

Q-007 and ADR-009 establish Decision as the Core Domain and require it to be
attributable to Evidence. They explicitly authorize no Decision runtime,
contract, persistence, or Rule Engine. The backend contains no Decision package
or provider.

`DecisionReferenceQuery` must be supplied by the future owning Decision
capability and return enough authoritative information to confirm existence and
Evidence attribution. Decision-driven case creation, Decision association,
Action association, and resolution cannot trust a request `decisionRef` alone.

Provider available: **NO**.

## 4. Action Provider Analysis

Q-007 formally defines Action as business response intent originating from a
Decision and separates it from Execution. It supplies no Action runtime,
published query contract, repository, or provider.

`ActionReferenceQuery` must be owned or backed by the approved Action
capability and must confirm both existence and originating DecisionRef. Risk
Case may consume that information through a read-only port but may not create a
placeholder Action capability or infer an Action from a string.

Provider available: **NO**.

## 5. ActionOutcome Provider Analysis

ADR-009 and Q-007 define the separation between Action intent and execution
attempt/outcome. They deliberately do not design or implement the execution
side. There is no Account Control, Execution, MT4/MT5 adapter, outcome model, or
provider in the backend.

`ActionOutcomeReferenceQuery` therefore requires a separately approved owning
execution/outcome capability. It may confirm an opaque outcome reference but
must not cause execution or interpret unsupported vendor success. Q-008 must
not implement Account Control, Manager API, or an execution engine to satisfy
this port.

Provider available: **NO**.

## 6. Q-007 Capability Mapping

| Needed capability | Q-007 contribution | Runtime/backend evidence | Satisfies prerequisite? |
| --- | --- | --- | --- |
| ActorContext | No actor or identity model; authorization must not be inferred | No security dependency or actor package | NO |
| Authorization | Explicitly excluded with RBAC/runtime behavior | No filter chain, authentication context, or authorization service | NO |
| EvidenceReferenceQuery | Evidence semantics and provenance ownership only | No Evidence contract/provider | NO |
| DecisionReferenceQuery | Decision Core Domain semantics only | No Decision contract/provider | NO |
| ActionReferenceQuery | Action-intent/origin invariant only | No Action contract/provider | NO |
| ActionOutcomeReferenceQuery | Action/Execution separation only | No Execution/Outcome contract/provider | NO |
| TradingAccountReferenceQuery | Trading Data named as supporting upstream context | No Trading Account authority/provider | NO |

Q-007 is reused as the ownership and semantic authority, not misrepresented as
an executable prerequisite implementation.

## 7. Gap Analysis

The current repository lacks all of the following:

1. an approved authentication/authorization Requirement and architecture;
2. a real identity authority and production principal mapping;
3. an authenticated ActorContext and authorization runtime wiring;
4. application-owned authoritative Trading Account, Evidence, Decision, Action,
   and ActionOutcome providers;
5. published owning-capability query contracts or adapters;
6. fail-closed startup/runtime behavior for missing providers; and
7. verification proving request identities and opaque references cannot bypass
   the authorities.

The Q-008 V4 `RISK_CASE_REFERENCE_NOT_FOUND` and
`RISK_CASE_REFERENCE_PROVIDER_UNAVAILABLE` design describes consumer failure
meaning but does not create the missing providers.

## 8. Option A Analysis — Reuse Q-007

**Result: REJECTED / INSUFFICIENT.**

Advantages:

- preserves the correct Evidence → Decision → Action → Risk Case ownership;
- provides the Action ≠ Execution invariant and Evidence provenance rule.

Why it cannot satisfy the prerequisites:

- Q-007 status is `Design Approved — Implementation Deferred`;
- Q-007 explicitly excludes the required runtime capabilities;
- no Java contract, provider, persistence, security context, or runtime wiring
  exists; and
- treating domain language as a provider would silently broaden Q-007 and make
  unverified references authoritative.

## 9. Option B Analysis — Minimal Foundation Prerequisite

**Result: PARTIALLY USEFUL, NOT SUFFICIENT FOR AUTHORIZATION.**

Consumer ports such as `AuthorizationPort`, `EvidenceReferenceQuery`,
`DecisionReferenceQuery`, `ActionReferenceQuery`, and
`ActionOutcomeReferenceQuery` can preserve dependency inversion. However,
interfaces alone cannot establish trust or existence.

Required production arrangement:

- Actor/authentication implementation is owned by a separate platform security
  capability; Risk Case consumes only a trusted ActorContext and authorization
  decision.
- Evidence and Decision providers are owned by the Decision Core Domain.
- Action provider is owned by the Action capability.
- ActionOutcome provider is owned by a separately approved execution/outcome
  capability.
- Trading Account subject authority is owned by its approved upstream account
  or Trading Data capability.

Runtime and failure rules:

- authorization occurs before case data is disclosed or mutated;
- missing/invalid authentication fails closed; denied access discloses no case
  data;
- reference providers perform read-only validation and never join the Risk
  Case/Audit database transaction as another resource manager;
- not-found produces the designed reference-not-found outcome;
- unavailable/timeout produces provider-unavailable and no case mutation;
- no fake/default/always-true bean is permitted for production startup;
- provider wiring must be explicit, and unavailable mandatory providers either
  prevent enabling dependent endpoints/use cases or fail closed at invocation;
- bounded timeout/retry/idempotency rules are required if any provider crosses
  a process boundary.

Module dependency direction:

```text
HTTP/security adapter → trusted ActorContext
Risk Case application → authorization consumer port
Risk Case application → reference-query consumer ports
Owning capability published query/adapter → authoritative data
Risk Case domain → no Spring Security, vendor, or owning-capability classes
```

No upstream provider may depend on the Risk Case domain merely to answer a
query, and Risk Case must not implement upstream ownership.

## 10. Option C Analysis — New Q-009 Requirement

**Result: RECOMMENDED.**

Q-008 explicitly excludes full authentication/IAM/RBAC and cannot silently add
a security dependency or identity integration. Q-007 also excludes them. A new
formal Requirement is therefore necessary.

Recommended Q-009 scope is a cohesive **Trusted Actor and Authorization
Foundation**, not an omnibus domain-provider module. It should decide the real
authentication source, trusted ActorContext, authorization boundary, failure
contracts, runtime wiring, security dependency/ADR need, audit attribution, and
tests. It must not create teams, organization hierarchy, full IAM
administration, or Risk Case behavior unless separately approved.

Authoritative domain providers remain the responsibility of their owning
capability Requirements. Q-009 may record the integration contract and
sequencing obligation, but it must not implement placeholder Evidence,
Decision, Action, ActionOutcome, or Trading Account truth.

The repository workflow does not explicitly authorize Codex to create a Draft
Requirement merely because prerequisite architecture analysis recommends one.
Therefore this task creates no `Q-009-Requirement.md` and waits for the
Architect/Product Owner decision.

## 11. Recommended Architecture Path

1. Preserve Q-008 V4 unchanged and approved.
2. Authorize a new Q-009 Requirement for Trusted Actor and Authorization
   Foundation.
3. Assign formal Requirements and sequence to the owning capabilities that
   will provide Trading Account, Evidence, Decision, Action, and ActionOutcome
   authoritative queries. Do not invent their Q-numbers in this Review.
4. Let Q-008 define/consume narrow read-only ports only when real providers and
   explicit runtime wiring are available.
5. Require fail-closed security/provider behavior and verification; no fake
   production beans or unchecked references.
6. After all prerequisite Requirements are approved, implemented, and verified,
   issue a separate Architect decision that changes Q-008 Implementation
   Authorization to YES and supplies the required executable Codex Prompt.

## 12. Required Next Step

The Architect/Product Owner should authorize drafting Q-009 for the trusted
Actor/authorization foundation and assign the owning-provider Requirement
sequence. Q-008 remains blocked until both tracks are complete.

## Exact Authorization Preconditions

Q-008 Implementation Authorization may become YES only after:

1. Q-009 (or another explicitly identified approved Requirement) defines and
   implements the real authentication, ActorContext, authorization, and
   fail-closed boundary;
2. approved owning-capability Requirements provide real Trading Account,
   Evidence, Decision, Action, and ActionOutcome reference authorities;
3. production runtime wiring, module dependency direction, unavailable/not-
   found behavior, timeout/retry rules where applicable, and security/audit
   tests are verified with no fake fallback;
4. ADR reviews for new dependencies or architecture boundaries are accepted;
5. Q-008 V4 compatibility is rechecked without redesign; and
6. an external Architect explicitly grants Q-008 implementation authorization
   and provides a complete ready-to-use Implementation Prompt.
