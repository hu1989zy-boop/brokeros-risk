# Q-009 ADR Review

## ADR Under Review

- File: `docs/adr/ADR-011-trusted-actor-capability-authorization-architecture.md`
- Title: Trusted Actor and Capability Authorization Architecture
- Status: Proposed
- Requirement: Q-009
- Architect approval: NOT RECORDED

## ADR Necessity

An ADR is required because Q-009 establishes system-wide trust boundaries,
application authorization semantics, framework dependency direction, and the
identity-provider adapter boundary. These choices affect every protected
application use case, including future Q-008 integration.

## Completeness

ADR-011 includes:

- Context grounded in the current baseline and Q-008 dependency;
- a coherent Decision for identity authority, principal translation, actor
  mapping/context, service execution, authorization, enforcement, framework,
  audit, and failure boundaries;
- Alternatives covering owned passwords, provider-specific roles, caller actor,
  transport-only authorization, role contracts, generic SYSTEM, framework
  leakage, and stateful sessions;
- positive, cost, operational, compatibility, and deferred Consequences;
- an explicit approval boundary.

## Review Finding

ADR-011 is ready for Architect Review as one coherent proposed decision. It is
not fragmented into implementation-level or provider-specific ADRs, and it does
not falsely claim acceptance.

## Decision Requested from Architect

Accept, reject, or return ADR-011 for revision. Acceptance should not be
interpreted as authorization to begin Implementation Design or Implementation
without a separate instruction.
