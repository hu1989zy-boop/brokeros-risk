# Q-009 Outstanding Implementation Design Inputs

## Architecture Approval Blockers

None. Architecture V2 is approved, ADR-011 is accepted, and no Architecture V3
is required.

## Inputs for Q-009 Implementation Design

The next design phase must resolve, without changing the accepted boundaries:

1. concrete identity-provider strategy while preserving provider neutrality;
2. human authentication runtime contract;
3. token format, issuer/audience, validation, claims mapping, freshness, and
   revocation behavior;
4. service authentication and credential mechanism;
5. concrete `VerifiedPrincipal` contract;
6. actor mapping persistence and integrity model;
7. actor provisioning lifecycle;
8. actor disable/revoke behavior;
9. capability policy representation and module catalog ownership;
10. capability/policy persistence strategy;
11. concrete authorization-port contract and safe decision evidence;
12. ActorContext creation, lifecycle, cleanup, and explicit passing;
13. background service ActorContext establishment;
14. authentication, mapping, authorization, and provider-outage failure
    semantics;
15. policy cache and invalidation decision, if caching is required;
16. Spring Security infrastructure-adapter wiring;
17. operational endpoint protection;
18. focused unit, contract, integration, negative-path, and leakage test
    strategy.

## Decisions Explicitly Still Open

- Concrete Identity Provider: OPEN
- Human token/runtime details: OPEN
- Service credential mechanism: OPEN
- Persistence schema: OPEN
- Provisioning administration: OPEN
- Caching/invalidation: OPEN unless the design proves it unnecessary
- Runtime wiring: OPEN

No item above was resolved in this approval-recording task.

## Q-008 Outstanding Prerequisites

Trading Account, Evidence, Decision, Action, and ActionOutcome authoritative
providers plus explicit Q-008 implementation authorization remain outstanding.
