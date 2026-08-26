# Architecture Conformance

## Result

Architecture Conformance: **PASS**

ADR-011 Conformance: **PASS**

## Evidence

- One Phase 1 Spring Boot modular-monolith deployable is preserved.
- `com.brokeros.risk.security.domain` and `.application` contain no Spring
  Security, servlet, JWT, or JDBC imports; `SecurityArchitectureTests` enforces
  the boundary.
- Spring Security terminates in infrastructure adapters. The application sees
  immutable BrokerOS `VerifiedPrincipal`, `ActorRef`, `ActorContext`,
  `Capability`, and `AuthorizationDecision` values.
- Authentication, actor mapping, and authorization remain separate decisions.
  A valid JWT cannot bypass the exact active MySQL mapping, and a mapped actor
  receives no implicit capability.
- Human authentication uses the approved signed-JWT OAuth2 Resource Server
  boundary. The identity-provider vendor remains replaceable and open.
- Service identity uses an in-process descriptor identity allow-list plus the
  same authoritative active mapping; no token fabrication or SYSTEM bypass is
  present.
- Authorization is an explicit application-owned port backed by direct actor
  grants. No role model, cache, gateway trust, or framework-authority mapping
  was added.
- The V2 migration is forward-only and creates exactly the approved actor,
  principal-mapping, and actor-capability tables without provisioning data.
- No Q-008 file or behavior changed.

## Limit of This Finding

This PASS records structural and code-level conformance to approved Architecture
V2 and ADR-011. It does not replace the required MySQL 8.4 runtime verification,
which keeps the overall implementation gate incomplete.
