# ADR-011 Approval Record

## Decision

- ADR: ADR-011
- Title: Trusted Actor and Capability Authorization Architecture
- Architect Decision: ACCEPTED
- Previous Status: PROPOSED
- Current Status: ACCEPTED
- Decision origin: explicit external Architect Review decision supplied by the
  Product Owner
- Decision recorded: 2026-08-26

## Repository Synchronization

`docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md` now
uses the repository convention `Status: Accepted`, records approval date and
origin, and retains Context, Decision, Alternatives, and Consequences.

No substantive decision was changed. No duplicate ADR-011 exists in the
repository. The accepted ADR does not authorize Implementation Design or
Implementation.

## Accepted Boundary

ADR-011 accepts the pluggable-hybrid identity authority boundary, BrokerOS-owned
VerifiedPrincipal/ActorRef mapping/ActorContext abstractions, distinct HUMAN and
SERVICE identities, capability-based use-case authorization, no generic SYSTEM
bypass, trusted audit attribution, Spring Security infrastructure isolation,
and fail-closed behavior.

## Deferred Inputs

The accepted ADR intentionally leaves provider selection, token validation,
service credential mechanism, mapping/policy persistence, provisioning,
caching/invalidation, and runtime wiring for Implementation Design.
