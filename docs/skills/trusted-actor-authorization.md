# Trusted Actor and Authorization Skill

## When to Use

Use this guidance when adding or reviewing authentication adapters, BrokerOS
actor mapping, ActorContext propagation, capability authorization, security
provisioning, protected endpoint policy, or trusted Audit attribution.

Read Q-009, its approved Architecture V2, ADR-011, the approved Implementation
Design V1, `AGENTS.md`, and `development-standards.md` first. A new identity
provider, external service authentication, roles, delegation, policy cache,
online administration, or Q-008 behavior requires its own approved scope.

## Trust Boundary

Keep three decisions separate:

1. authentication validates a credential and creates a bounded
   `VerifiedPrincipal`;
2. authoritative BrokerOS mapping resolves that principal to one active
   `ActorRef` and fresh immutable `ActorContext`;
3. authorization evaluates one exact BrokerOS capability immediately before a
   protected application use case.

A valid token is not an actor grant, and a mapped actor has no implicit
capability. Request headers, bodies, query parameters, Request ID, Trace ID,
roles, scopes, and arbitrary internal strings never establish ActorRef or
permission.

## Human Authentication Pattern

- Keep Spring Security and JWT types in infrastructure adapters.
- Use the Boot-managed OAuth2 Resource Server dependency and one signed JWT
  decoder; do not pin Nimbus or add a second JWT implementation.
- Validate signature plus exact issuer, allowed audience, expiration, and
  not-before time with a bounded approved clock skew.
- Translate only exact issuer, exact subject, principal type, authentication
  method, authentication time, and credential expiry into BrokerOS types.
- Return the same bounded unauthenticated response for forged, malformed,
  expired, wrong-issuer, wrong-audience, premature, and structurally invalid
  credentials. Do not echo tokens, claims, issuer, or subject.
- Missing issuer/audience configuration must fail startup; there is no
  security-off property or permissive profile.

## Actor and Service Context Pattern

- ActorRef is a canonical lowercase UUIDv4 and is distinct from an external
  principal key.
- ActorContext is immutable, execution-scoped, and contains no embedded
  capability set.
- Use a fresh execution identifier for every context creation.
- Purpose-specific service identities require both a code-owned descriptor
  registry entry and an active authoritative database mapping.
- Use registry object identity, not caller-supplied service codes, as the
  in-process assertion boundary.
- Never introduce a generic `SYSTEM`, wildcard, admin, or always-allow actor.
- Domain and application packages must not import Spring Security, servlet,
  JDBC, or JWT types.

## Authorization and Persistence Pattern

- Represent capabilities as exact bounded `<domain>:<action>` values.
- Call one application-owned authorization boundary for every protected read
  and mutation; controllers alone are not an enforcement boundary.
- Only an explicit `ALLOW` proceeds. Unknown actor, disabled actor, missing
  grant, revoked grant, ambiguity, and dependency failure deny.
- Persist actors, exact external-principal mappings, and direct actor grants in
  application-owned MySQL tables with uniqueness, foreign keys, stable string
  codes, UTC timestamps, and optimistic versions.
- Do not add roles, permission inheritance, Redis caching, token/session
  storage, or provider payload columns under the foundation scope.
- Flyway owns schema only. Provision deployment identities and grants through
  an explicitly invoked non-web command after migration.
- Provisioning must validate the full manifest, be transactionally atomic,
  return unchanged only for exact state/provenance, and reject conflicts. Do
  not auto-enroll unknown authenticated principals.

## Failure and Logging Rules

- Distinguish invalid authentication, actor denial, authorization denial, and
  security dependency unavailability without disclosing existence or policy
  internals.
- A database or provider failure must never become an allow or a permissive
  fallback.
- Log bounded event name, outcome, and stable result code only. Do not log the
  bearer value, credential, full claims, issuer, subject, principal key,
  capability manifest, or authorization header.
- Request and trace correlation may accompany safe events but never become
  identity or authorization input.

## Verification Pattern

Use real signed JWTs through the complete Spring Security filter chain. Cover:

- valid mapping and explicit grant;
- every signature/issuer/audience/time/subject negative case;
- actor-header and role/scope injection resistance;
- unknown/disabled mapping and missing/revoked capability;
- mapping and authorization dependency failure;
- fresh context creation and cleanup between sequential requests;
- trusted service registry misuse and direct-grant behavior;
- provisioning idempotence, conflict, disable/reactivate, revoke/regrant, and
  optimistic-version failure;
- existing health, API response, correlation, and exception regressions; and
- package dependency rules that keep framework security out of domain and
  application code.

Migration and JDBC behavior must run against disposable MySQL 8.4, not H2.
Prove V1→V2 migration, three-table shape, exact collations and constraints,
index-size compatibility, safe mapping/authorization query plans, Flyway
validation/restart, and repository lifecycle behavior. A skipped real-MySQL
test does not complete this gate.

When asserting MySQL CHECK enforcement through Spring JDBC, verify the exact
vendor error code and SQL state under `DataAccessException`. Do not assume every
driver/translator version maps a CHECK failure to the same narrower Spring
exception subtype, and do not broaden an assertion so far that a connection,
syntax, or unrelated SQL failure could satisfy the constraint test.

Inspect the dependency tree to confirm one Boot-managed Spring Security/Nimbus
stack. Run Maven verify, static checks, and applicable Kustomize/infrastructure
checks, and record unavailable tools as blockers rather than PASS.

## Common Mistakes

- Treating a decoded JWT, role, or scope as direct application authority.
- Accepting `X-Actor-*`, actor DTO fields, or correlation values as identity.
- Storing capabilities in ActorContext and thereby making revocation stale.
- Protecting controllers while allowing an internal use case to bypass the
  authorization guard.
- Auto-provisioning an unknown authenticated subject.
- Adding a test-only production bypass instead of a real signed test JWT.
- Logging principal identifiers or returning different errors that disclose
  account/grant existence.
- Claiming MySQL correctness from SQL text inspection or an in-memory database.

## Validation Checklist

- Q-009 scope and ADR-011 remain authoritative; Q-008 is unchanged.
- Human trust uses signed JWT validation with exact issuer/audience/time rules.
- One active exact mapping is required before ActorContext creation.
- Service context requires both registered descriptor and active mapping.
- Every protected use case asks for one exact capability; only ALLOW proceeds.
- No actor/permission header, DTO, role, wildcard, SYSTEM, JIT enrollment, or
  permissive fallback exists.
- Security errors and logs are bounded and contain no sensitive identity data.
- Three additive Flyway-owned tables are the only security persistence added.
- Offline provisioning is atomic, exact-idempotent, versioned, and non-web.
- Signed-JWT/filter-chain tests and real MySQL 8.4 repository tests pass.
- Dependency, architecture, Maven, static, deployment, and Review evidence is
  complete and honest.
