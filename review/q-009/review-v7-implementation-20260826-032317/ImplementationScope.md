# Implementation Scope

## Implemented

- BrokerOS-owned immutable security domain values for ActorRef, HUMAN/SERVICE
  actors, exact external principal keys, verified principals, ActorContext,
  capabilities, authorization decisions, lifecycle statuses, and provisioning
  provenance.
- Framework-neutral application ports and services for actor mapping,
  ActorContext access, capability decisions, guarded execution, service actor
  construction, provisioning, disable/reactivate, and revoke/regrant.
- Spring Security OAuth2 Resource Server signed-JWT boundary with exact issuer,
  audience, expiry, not-before, and bounded clock-skew validation.
- Safe 401/403/503 response adapters, stateless filter-chain policy, public
  health routes, authenticated information/documentation routes, and
  production-disabled OpenAPI/Swagger.
- Authoritative JDBC actor mapping, direct actor capability decisions, and
  transactional exact-idempotent offline provisioning.
- Additive Flyway V2 migration with exactly three security tables and no seed
  data, role schema, token store, session store, or destructive DDL.
- Non-web one-shot bootstrap command consuming one explicit manifest path.
- Boot-managed Spring Security/resource-server/test dependencies without a
  direct Nimbus version pin.
- Q-009 configuration catalog, governance status, lessons learned, reusable
  repository skill, static checks, and Q-009-aware infrastructure assertions.
- Unit, signed-JWT filter-chain, architecture, bootstrap, regression, migration,
  and opt-in real MySQL 8.4 tests.

## Explicitly Not Implemented

- Q-008 Risk Case code or behavior.
- A concrete identity-provider vendor or deployment trust values.
- Human password storage, login UI, OIDC login, sessions, HTTP Basic, API keys,
  gateway headers, mTLS, or external service-to-service authentication.
- Roles, groups, wildcard/admin/SYSTEM authority, hierarchy, delegation,
  impersonation, break-glass, resource-scoped policy, or JIT enrollment.
- Redis authorization cache, Kafka security events, online IAM administration,
  SIEM integration, or a new Audit module.
- Real deployment principals, mappings, capabilities, or Flyway seed data.
- Q-008 capability-to-use-case wiring.

## Change Volume

- Modified tracked files before Review generation: 19
- New implementation/governance files before Review generation: 56
- Total implementation/governance paths: 75
- New production security Java files: 46
- New security test Java files: 7
- Review transfer ZIP: excluded from the future source baseline
