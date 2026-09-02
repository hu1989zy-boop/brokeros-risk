# Q-016 Requirement V1 — Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12 and the Decision Principles.

## Task ID / Stage

Q-016 — Frontend Foundation (Flutter Risk Console). **Requirement** stage;
self-review by Claude Code (external Architect role). Started while the
Product Owner organizes the MT4/MT5 SDK, so Q-015 is parked and this
SDK-independent workstream proceeds.

## Scope Reviewed

`docs/requirements/Q-016-Frontend-Foundation.md` V1, checked for internal
consistency and against the actual committed backend security/API shape.

## Files Inspected (to ground the frontend prerequisites, not assume them)

- `com.brokeros.risk.security.*` — confirmed the backend **verifies**
  externally-issued JWTs (`SecurityJwtDecoderFactory`,
  `ActorContextAuthenticationFilter`, `JwtVerifiedPrincipalAdapter`) and
  has **no** login/token-issuance endpoint. So the console authenticates
  against an external IdP (OIDC) and sends a Bearer token — captured as
  §1(1)/§5.3(1), not assumed.
- `riskcase/interfaces/rest/RiskCaseController.java` — confirmed only
  `/api/risk-cases` + get-by-number/detail/history/lifecycle; **no
  list/search** endpoint. So a backend read/query endpoint is a real
  prerequisite — captured as §1(2)/FR-005/§5.3(3).

## Findings

The draft is grounded in the actual backend, not a generic frontend
template:

1. **Auth is delegated to an external IdP** (a real, verified fact about
   Q-009) — so the frontend "login" is an OIDC flow against the token
   issuer, and the genuine open question is *which IdP* (§5.3(1)), with a
   dev-IdP recommendation to keep the Foundation unblocked.
2. **A backend list/query endpoint must be added** — flagged as an
   additive, pattern-following backend change (FR-005), explicitly not a
   change to Q-008's aggregate/business rules.
3. **Thin-client discipline** (§4.5/§8/FR-006) — the console renders bounded
   backend data, never bypasses authorization, never holds business rules
   the backend doesn't enforce (Principles §5/§15). This is the main
   architectural risk for a frontend and is stated as a hard constraint.
4. **Scoped as one vertical slice + skeleton** (Risk Case: login → list →
   detail/history → one operation), not "build the whole UI" — avoids the
   over-scope trap and proves the whole stack end to end.

Three genuine product/infrastructure questions are surfaced in §5.3 with
recommendations (IdP: dev-IdP now, prod later; target platform: web first;
backend read/query: build the one endpoint here) — each is product/infra
(Decision Authority §16.2), not a technical detail Claude Code should
decide. Repo placement and Flutter client architecture are correctly left
as Claude Code HOW decisions for the Architecture stage.

No inconsistency found; the seven `Q016-FR-XXX` align with goals and ACs;
§16.1 single-live-status applied (Status header defers to §17).

## Remaining Risks

- The IdP decision gates real auth; the dev-IdP path keeps the slice
  runnable, but a production IdP is a separate infra decision the Product
  Owner will need to make eventually.
- Implementation needs the Flutter SDK installed (freely installable,
  unlike the Q-015 Manager SDK) — not a Requirement-stage blocker.

## Out-of-Scope Issues

None beyond §5.2 (no dashboards/analytics until Q-015 data exists, no IdP
built, no business-rule changes).

## Recommendation

Present to the Product Owner for a Gate Decision, with attention to §5.3's
three questions.

## Gate Decision

**PASS** (self-review only — the Product Owner's Gate Decision remains
outstanding).
