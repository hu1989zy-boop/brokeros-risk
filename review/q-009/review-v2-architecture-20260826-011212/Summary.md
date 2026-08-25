# Q-009 Architecture Review Summary

## Review Identity

- Review ID: Q-009-ARCHITECTURE-V2-20260826-011212
- Requirement ID: Q-009
- Review type: Architecture Review
- Review version: v2
- Review status: READY FOR ARCHITECT REVIEW
- Architect approval: NOT RECORDED
- Implementation allowed: NO

## Scope Completed

This package contains Q-009 Architecture Analysis, one proposed architecture
decision record, an architecture Lessons Learned entry, security and gap
analysis, verification evidence, and the relevant approved Q-007/Q-008/Q-009
authority documents.

No Requirement analysis was repeated and the approved Q-009 Requirement was not
modified. No Implementation Design or Implementation was performed.

## New Documents

- `docs/architecture/q-009-trusted-actor-authorization-architecture.md`
- `docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md`
- `docs/lessons/2026-08-26-q-009-trusted-actor-authorization-architecture.md`
- this independent Review directory and its ZIP package

## Decision Summary

| # | Question | Proposed decision |
| --- | --- | --- |
| 1 | Identity authority | Pluggable-hybrid: external credential authority, BrokerOS actor/mapping/policy ownership |
| 2 | Concrete identity provider | OPEN |
| 3 | Human authentication | Externally issued credential validated at HTTP boundary; application stateless for login session |
| 4 | Service authentication | Separately typed purpose-specific service identity; controlled background bootstrap; no generic SYSTEM |
| 5 | VerifiedPrincipal | Required BrokerOS boundary between authentication and actor mapping |
| 6 | Actor mapping | Q-009 platform security capability owns durable, active, auditable BrokerOS mapping |
| 7 | ActorContext | Q-009-owned immutable per-execution context; bounded and explicitly passed |
| 8 | Authorization | Capability-based, default deny, explicit allow; external roles/claims only governed inputs |
| 9 | Enforcement | Protected application use-case boundary before protected access/mutation |
| 10 | Spring Security | Recommended only as HTTP/Servlet security infrastructure adapter |
| 11 | Dependencies | Future implementation: Spring Security and likely OAuth2 Resource Server; none added now |
| 12 | ADR | ADR-011 created with Proposed status |
| 13 | Ready for Architect Review | YES |
| 14 | Implementation Authorized | NO |

## Architecture Outcome

The proposal prevents caller-supplied actor spoofing, provider role coupling,
controller-only authorization, and a background `SYSTEM` bypass. It preserves
the Phase 1 modular monolith and adapter boundaries. Q-008 can later consume the
trusted ActorRef/capability boundary, but remains parked and has additional
unresolved prerequisites.

## Change Boundary

- Java/source changes: NONE
- Dependency/POM changes: NONE
- Configuration changes: NONE
- Migration/database changes: NONE
- REST endpoint changes: NONE
- Kafka/Redis changes: NONE
- Docker/Kubernetes changes: NONE
- Q-009 Requirement changes: NONE
- Q-008 changes: NONE
- Git staging/commit/push: NONE
