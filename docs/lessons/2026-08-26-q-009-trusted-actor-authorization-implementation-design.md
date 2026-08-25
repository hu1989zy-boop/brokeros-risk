# Q-009 Trusted Actor and Authorization Implementation Design Lessons Learned

## Approval Status

- Implementation Design: V1 — APPROVED
- Architect decision: APPROVED — external decision confirmed 2026-08-26
- Implementation Design V2 required: No
- Implementation: Not started
- Implementation authorized: No

## Scope

This lesson records design-phase findings only. Q-009 production implementation
has not started, no framework dependency or configuration has been added, and
none of the proposed runtime behavior has been validated in code.

## What Was Designed

The design selected signed JWT OAuth2 Resource Server authentication for human
requests, a registered trusted in-process identity for Phase 1 internal
services, BrokerOS-owned MySQL principal mapping, direct actor-to-capability
grants, pre-provisioning through an offline one-shot command, explicit
application-boundary authorization, and no role or security cache.

It also defined a three-table additive schema, proposed Flyway V2 migration,
safe failure responses, bounded security logging, context isolation, transaction
and concurrency behavior, Q-008 consumption, dependencies, rollout, and a
signed-JWT/MySQL 8.4 verification strategy.

## Repository Evidence That Changed the Design

- The backend has Spring Boot web/JDBC/Flyway but no Spring Security or JWT
  dependency. The design therefore records a minimal Boot-managed dependency
  plan instead of assuming security already exists.
- `V1__initial_schema.sql` has no business tables. The Q-009 schema can be an
  additive V2 without data migration or compatibility shims.
- `RequestCorrelationFilter` and W3C tracing already distinguish request/trace
  context from identity. ActorContext preserves that separation.
- The current `ResultCode` set has no authentication or authorization codes.
  The design treats new codes as future stable contracts, not documentation-
  only aliases.
- ADR-008 requires native framework properties to remain native and only real
  BrokerOS-owned groups to receive typed binding. This led to native resource-
  server issuer/audience/JWK properties and one bounded owned clock-skew value.
- Q-008 reserves ActorRef semantics but has no implementation. Q-009 can own one
  canonical ActorRef without modifying Q-008 or creating a competing identity.

## Alternatives Rejected

- JIT human provisioning was rejected because an authenticated external
  principal is not automatically an authorized BrokerOS actor.
- A generic `SYSTEM` identity was rejected because it destroys purpose-specific
  service attribution and becomes a privilege bypass.
- Service JWTs and mTLS were deferred because Phase 1 has no distributed service
  boundary requiring them.
- Caller roles/scopes were rejected as direct authority; the foundation stores
  explicit BrokerOS capabilities.
- A Phase 1 role model and Redis authorization cache were rejected as
  unnecessary sources of expansion and stale privilege.
- Flyway identity seed data and a public administration endpoint were rejected;
  schema and controlled application provisioning have separate ownership.
- Disabling security in tests was rejected in favor of ephemeral signed JWTs
  through the actual resource-server/filter boundary.

## Design Problems Resolved

The largest ambiguity was how to authenticate a scheduled or internal service
without inventing a fake token. The selected boundary uses a code-owned registry
to limit which identities can be asserted and MySQL to control whether the
service actor is active and which capabilities it holds. Neither source alone
can create authority.

Another ambiguity was whether capabilities belong in ActorContext. Embedding
them would make disable/revoke behavior stale and complicate async propagation.
The design keeps ActorContext as authentication identity/provenance only and
evaluates capabilities through the authoritative port for each protected use
case.

Pre-provisioning needed a path that was neither manual DDL nor a full IAM API.
An explicitly invoked, non-web, idempotent application command provides a
bounded transition while preserving Flyway schema ownership and fail-closed
empty-database behavior.

## Reusable Design Guidance

- Authentication proves a principal; mapping establishes a BrokerOS actor;
  authorization independently evaluates a capability.
- Keep framework authentication at the adapter and pass immutable business
  security context explicitly into use cases.
- Do not cache authority before revocation, invalidation, outage, and
  multi-instance behavior are specified and measured.
- Use database uniqueness for identity ambiguity and optimistic versions for
  administrative races; avoid distributed locks without a distributed need.
- Test a security boundary using real signed assertions and the real filter
  chain. Framework test helpers are supplementary, not a replacement.
- Keep schema migration and deployment-specific identity provisioning separate.

## Honest Limitations and Future Risks

- No implementation or executable test yet proves the proposed Spring Security
  filter ordering, MySQL constraints, startup validation, or failure mapping.
- The concrete identity provider and environment trust values remain open and
  must be supplied before rollout.
- Actual service identities and capabilities cannot be provisioned until their
  owning Requirements are approved.
- Q-008 still requires its upstream provider prerequisites and a final exact
  capability-to-use-case mapping before implementation.
- A reusable repository skill is not updated in this phase because the pattern
  has not yet been validated in production code and tests. Skill extraction
  must be reevaluated after successful implementation.
