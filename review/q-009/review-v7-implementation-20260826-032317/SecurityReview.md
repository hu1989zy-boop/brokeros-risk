# Security Review

## Result

Security Review: **FAIL — mandatory runtime evidence incomplete**

No code-level privilege escalation or sensitive-data disclosure was identified
in the inspected scope. The gate remains FAIL because the authoritative MySQL
mapping/grant store, constraints, collations, query plans, Flyway restart, and
JDBC lifecycle were not executed on MySQL 8.4.

## Controls Verified by Executable Tests and Inspection

- Actor identity is not accepted from request headers, bodies, query values,
  roles, scopes, Request ID, or Trace ID.
- Signed JWT validation covers signature, exact issuer, approved audience,
  expiration, not-before, required subject, and bounded clock skew.
- Forged, malformed, wrong-issuer, wrong-audience, expired, premature, and
  structurally invalid tokens return the same bounded 401 contract.
- Unknown or disabled actor mapping returns bounded 403 without auto-enrollment
  or existence details.
- Mapping/authorization dependency failure is fail-closed and distinct from a
  normal denial.
- ActorContext and AuthorizationDecision are immutable; a fresh execution
  identifier is created for each context and request context is cleaned up.
- Capabilities use exact bounded values. Only explicit ALLOW proceeds; missing
  and revoked grants deny.
- In-process services cannot assert arbitrary codes and cannot use SYSTEM.
- The production service registry is empty until a separately approved service
  identity exists.
- Security logs contain bounded event, outcome, and stable result code only;
  token, claims, issuer, subject, principal key, and authorization header are
  absent.
- Production OpenAPI/Swagger is disabled; health remains public while other
  routes default to authenticated.
- No permissive test provider or production security-off property exists.

## Evidence Still Required

- Apply and validate V1→V2 on disposable MySQL 8.4.
- Prove binary exact issuer/subject semantics, CHECK constraints, FK RESTRICT,
  unique/index size compatibility, and approved three-table shape.
- Prove safe query plans for mapping and capability decisions.
- Prove exact-idempotent provisioning, conflict rollback, disable/reactivate,
  revoke/regrant, stale-version refusal, and restart idempotence against MySQL.
