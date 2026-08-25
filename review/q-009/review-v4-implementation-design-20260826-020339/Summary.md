# Q-009 Implementation Design Review Summary

## Review Metadata

- Review ID: Q-009-IMPLEMENTATION-DESIGN-V4-20260826-020339
- Requirement: Q-009 — Trusted Actor and Authorization Foundation
- Review Type: Implementation Design Review
- Review Package Version: V4
- Design document version: V1
- Review Status: **COMPLETE — READY FOR ARCHITECT REVIEW**
- Design Status: **Draft — awaiting architect approval**
- Implementation Design Complete: **YES**
- Implementation: **NOT STARTED**
- Implementation Authorized: **NO**

`V4` is the Review Package version. It is not Architecture V4 and does not
change approved Architecture V2.

## Scope Completed

The draft Implementation Design resolves the concrete runtime, trust,
persistence, provisioning, authorization, configuration, failure, transaction,
cache, Q-008 integration, dependency, migration, test, rollout, and sequencing
decisions required before implementation review.

The design selects:

- signed JWT OAuth2 Resource Server for human requests;
- an open, deployment-supplied concrete identity-provider vendor;
- registered trusted in-process service identities for Phase 1 automation;
- authoritative BrokerOS-owned MySQL actor/principal mapping;
- controlled pre-provisioning with no JIT enrollment;
- explicit direct actor-to-capability grants with default deny;
- no Phase 1 role model and no security cache;
- Boot-managed Spring Security resource-server dependencies; and
- additive Flyway V2 with three application-owned tables.

## Files Added

- `docs/architecture/q-009-trusted-actor-authorization-implementation-design.md`
- `docs/lessons/2026-08-26-q-009-trusted-actor-authorization-implementation-design.md`
- this V4 Review directory and its 13 files
- independent V4 Review ZIP outside the Git baseline

No approved Requirement, Architecture, ADR, Q-008 artifact, production source,
POM, configuration, migration, infrastructure, or frontend file was modified.

## Final Decisions

| Decision | Result |
| --- | --- |
| Implementation Design Complete | YES |
| Human Authentication Runtime | Spring Security OAuth2 Resource Server with signed JWT |
| Concrete Identity Provider Vendor | OPEN |
| Service Authentication | Registered trusted in-process service identity plus active DB mapping |
| Actor Mapping Persistence | BrokerOS-owned MySQL exact principal mapping |
| Actor Provisioning | Controlled offline pre-provisioning; no JIT |
| ActorContext Capability Strategy | No embedded capabilities; fresh decision per protected use case |
| Authorization Persistence | Direct actor-to-capability MySQL grants |
| Role Model Required in Phase 1 | NO |
| Security Cache Required in Phase 1 | NO |
| Spring Security Dependencies | Boot security starter, OAuth2 resource-server starter, test security support |
| Database Migration Required | YES — proposed Flyway V2 after approval |
| Q-009 Implementation Ready for Architect Review | YES |
| Q-009 Implementation Authorized | NO |

## Next Gate

Architect Review of the Q-009 Implementation Design. No implementation may
start without an explicit approval and implementation authorization.
