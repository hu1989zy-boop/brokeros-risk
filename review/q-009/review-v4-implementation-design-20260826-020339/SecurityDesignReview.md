# Q-009 Security Design Review

## Result

Security Design: **PASS FOR ARCHITECT REVIEW**

Implementation Authorized: **NO**

## Trust Boundary

Human credentials are untrusted until the signed JWT decoder validates
signature, issuer, audience, expiry, and not-before time. Only validated issuer
and subject enter a bounded `VerifiedPrincipal`. Exact BrokerOS mapping then
establishes an active ActorRef and immutable ActorContext. The application use
case obtains an explicit capability decision before protected work.

Internal service work uses a separate registered descriptor and active SERVICE
mapping. It does not fabricate a JWT, inherit a human context, accept a caller
ActorRef, or use `SYSTEM`. HTTP and internal paths share the same authorization
port.

## Security Decisions

| Topic | Decision |
| --- | --- |
| Human credential | Signed bearer JWT through OAuth2 Resource Server |
| Provider vendor | Open; one externally configured issuer contract per deployment |
| Session/basic/form login | Disabled/not selected |
| Forwarded identity | Not trusted; no gateway contract exists |
| JWT authorization claims | Roles/groups/scopes are not capabilities |
| Human enrollment | Pre-provisioned; unknown authenticated user receives generic 403 |
| Service trust | Code-owned registered descriptor plus DB activation/grants |
| Authorization | Server-side explicit capability at use-case boundary |
| Default | Deny; unavailable is distinct 503 and never allow |
| Role/superuser | None in Phase 1 |
| Cache | None in Phase 1 |
| Context propagation | Explicit and execution-scoped; no async/thread inheritance |
| Secrets/logging | No token, credential, full claims, issuer/subject, or headers logged/stored |

## Failure and Disclosure Review

- 401 is proposed for missing/invalid credentials with generic messages.
- 403 is proposed for unknown/disabled mapping and explicit capability denial;
  responses do not reveal policy or protected-resource existence.
- 503 is proposed for mapping/authorization dependency unavailability.
- Filter-chain failures retain the existing `ApiResponse` and request-ID
  contract through safe Spring Security handlers.
- Health/probes remain public with hidden details; other routes authenticate by
  default. API docs are not public in production.

## Threat/Test Coverage

The design includes forged/expired/wrong-issuer/wrong-audience JWTs, caller
actor/username spoofing, unknown/disabled actor, missing/revoked capability,
mapping/authorization outage, service identity misuse, `SYSTEM` attempt,
human-context reuse, request/trace misuse, and thread/context leakage.
Ephemeral signed JWTs exercise the real filter boundary; disabling security is
not the primary test strategy.

## Residual Security Risks

- Concrete provider trust values must be supplied securely before rollout.
- External service authentication is deferred because no distributed boundary
  is approved.
- Online administration, delegation, break-glass, roles, resource policy, and
  caching remain outside Q-009 and require later decisions.
- Runtime behavior is unverified until implementation tests pass.

No unresolved security-design violation prevents Architect review.
