# Q-009 Implementation Design V1 Approval Record

## Approved Design

- Document: `docs/architecture/q-009-trusted-actor-authorization-implementation-design.md`
- Version: V1
- Status: APPROVED
- Architect decision: APPROVED — external decision confirmed 2026-08-26
- Design V2 required: NO
- Implementation authorized: NO

## Approved Decisions

| Area | Approved result |
| --- | --- |
| Human Authentication | Signed JWT OAuth2 Resource Server |
| Concrete IdP | OPEN — deployment/environment input |
| Human password ownership | BrokerOS does not own human passwords in Q-009 |
| VerifiedPrincipal | Trusted post-authentication, pre-Actor mapping value |
| Principal/actor types | HUMAN and SERVICE only |
| Service Authentication | Registered trusted in-process service identities |
| Future distributed service auth | Separate later architecture such as service principal, workload identity, or mTLS |
| Actor Mapping | BrokerOS authoritative exact issuer/subject/type mapping in MySQL |
| Actor lifecycle | ACTIVE/DISABLED; no hard deletion of historical ActorRef |
| Actor Provisioning | Pre-provisioning; no JIT actor creation |
| Bootstrap | Controlled offline one-shot non-secret manifest, one local transaction |
| ActorContext | Immutable, explicit per execution, no embedded capability cache |
| Authorization | Capability-based direct actor grants; default deny and explicit allow |
| Enforcement | Application use-case boundary; framework rules are defense in depth |
| Role Model | Not required in Phase 1 |
| Security Cache | Not required in Phase 1 |
| Capability ownership | Q-009 owns semantics; business modules own business catalogs |
| Spring Security | Infrastructure adapter only |
| Failure semantics | 401 authentication, 403 mapping/capability denial, fail-closed/503 persistence outage |
| Security logging | Bounded events; no credential/token/full-claim logging |
| Database | `security_actor`, `security_principal_mapping`, `security_actor_capability` |
| Integrity | Unique ActorRef, exact principal identity, and actor/capability assignment |
| Database Migration | Required under Flyway during implementation |
| Test boundary | Ephemeral signed JWT and real Spring Security chain, not disabled security |
| SYSTEM superuser | Prohibited |

## Identity Semantics

The authoritative external identity is exact issuer plus subject with principal
type semantics. No trim, case conversion, Unicode normalization, email/
username substitution, request actor field, or caller header establishes
identity unless a future provider contract explicitly changes the approved
mapping design.

## Q-008 Boundary

Future Q-008 mutation actor attribution must originate from Q-009 ActorContext,
never request `actorId`, `createdBy`, `resolvedBy`, or similar fields. Assignee
remains a distinct business concept. Q-008 is still unauthorized and its
Trading Account, Evidence, Decision, Action, and ActionOutcome prerequisites
remain outside this recording.
