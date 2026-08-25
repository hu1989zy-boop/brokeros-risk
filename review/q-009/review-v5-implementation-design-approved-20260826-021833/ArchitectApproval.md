# Q-009 Implementation Design Architect Approval

## Formal Architect Decision

Q-009 Implementation Design V1

Architect Decision: **APPROVED**

Implementation Design V2 Required: **NO**

Decision origin: explicit Architect Review approval supplied by the Product
Owner on 2026-08-26.

This approval does not authorize implementation.

## Approved Governance Baseline

- Q-009 Requirement V1: APPROVED
- Q-009 Architecture V2: APPROVED
- ADR-011: ACCEPTED
- Q-009 Implementation Design V1: APPROVED
- Q-009 Implementation: NOT STARTED
- Q-009 Implementation Authorized: NO
- Q-008 Implementation Authorized: NO

## Approved Major Decisions

- Human authentication uses signed JWT OAuth2 Resource Server validation;
  concrete IdP vendor remains an open deployment input.
- `VerifiedPrincipal` is a trusted post-authentication, pre-Actor mapping value;
  framework/JWT/HTTP types do not enter domain/application core.
- Foundation principal/actor types are HUMAN and SERVICE; generic privileged
  SYSTEM is prohibited.
- Phase 1 service trust uses registered purpose-specific in-process identities,
  code-owned descriptors, active authoritative SERVICE mapping, and explicit
  capability authorization.
- BrokerOS owns exact authoritative principal-to-ActorRef mapping and the three-
  table MySQL security foundation.
- Human actors are pre-provisioned; unknown valid JWT principals fail closed.
- Bootstrap is a controlled offline one-shot operation using a versioned non-
  secret manifest and one local transaction.
- ActorContext is immutable and contains no effective-capability cache.
- Authorization is capability-based, explicit-allow, default-deny,
  least-privilege, server-side, and enforced at the application use-case
  boundary.
- Direct actor grants are sufficient for Phase 1; roles and security caches are
  not required.
- Spring Security remains an infrastructure/framework adapter.
- A new Flyway migration is required during a separately authorized
  implementation; no migration exists in this recording phase.
- Signed test JWTs, the real filter chain, MySQL persistence tests, negative
  cases, concurrency, leakage, and regression verification are required.

## Approval Boundary

Deployment values remain open without requiring Design V2. Cross-process
service authentication remains a future architecture concern. Q-009 and Q-008
implementation remain unauthorized.
