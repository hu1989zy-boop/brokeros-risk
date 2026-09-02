# Q-016 Architecture + ADR-017 + Implementation Design — Bundle Self-Review

Produced under `docs/engineering/AI-Engineering-Execution-Protocol.md`
§11–§12 and the Decision Principles §16.5-B (connected-chain drafting).

## Task ID / Stage

Q-016 — Frontend Foundation (Flutter Risk Console). Architecture + ADR +
Implementation Design drafted as one connected chain under §16.5-B and
self-reviewed together; presented as a bundle at the implementation
-authorization gate (the Product Owner's).

## Scope Reviewed

- `docs/architecture/q-016-frontend-foundation-architecture.md` V1
- `docs/adr/ADR-017-frontend-foundation.md`
- `docs/architecture/q-016-frontend-foundation-implementation-design.md` V1

Checked for consistency with approved Requirement V1 (incl. the confirmed
§5.3: Keycloak / web-first / list endpoint here), internal consistency, and
against the actual committed backend security + Risk Case controller shape.

## Files Inspected

- Q-016 Requirement V1 (all 7 FRs traced into Design §10).
- `com.brokeros.risk.security.*` (verify-only JWT; drives the OIDC/Keycloak
  decision) and `riskcase/interfaces/rest/RiskCaseController.java` (no
  list endpoint; drives the additive read endpoint).
- ADR-009 boundary and Q-008's read-audit intent (§9.5) to keep the new
  list endpoint consistent, not inventive.

## Verification Executed

Not applicable — no code exists yet. `GitStatus.txt`/`GitDiffStat.txt`
confirm the only working-tree changes are the Q-016 documents/packages.

## Findings

The bundle is grounded in the actual backend and holds the thin-client
line:

1. **Auth is OIDC Auth Code + PKCE against Keycloak** — matching Q-009's
   verify-only design (the frontend does the login at Keycloak; the backend
   just verifies), and dev→prod is a config repoint. This is the correct,
   security-best-practice binding, not an invented in-backend login.
2. **Thin-client discipline is a hard, repeated constraint** (Arch §3/§4,
   ADR "Decision", Design §1/§6) — no business rules client-side, backend
   authorization never bypassed, bounded data only. This is the main
   architectural risk for any frontend and is treated as primary.
3. **The only backend change is one additive read/query endpoint** (Design
   §7) — explicitly no Q-008 aggregate/business/migration change, reusing
   existing controller/query/repository patterns, bounded pagination
   (server max 100), summary projection only. It even instructs the
   implementer NOT to invent a list-read audit if the existing pattern does
   not audit list reads, and NOT to add a migration.
4. **Scoped as skeleton + one vertical slice** — proving the full
   authenticated stack (Keycloak → JWT → Q-009 → Q-008) end to end, not
   broad shallow UI. Dashboards are correctly deferred to post-Q-015 (no
   trading data yet).
5. **HOW decisions recorded** (Riverpod/go_router/dio/typed contract,
   mono-repo `frontend/`) as durable Type-1-ish frontend-stack choices
   (§12 reversibility), under Decision Authority §16.1.

The session's **test-discipline lessons are carried into the backend
endpoint's test** (Design §9): no hard-coded migration counts, exact-name
ownership, and explicitly no new migration.

No inconsistency found across the three documents; all 7 `Q016-FR-XXX` map
to Design sections; §16.1 single-live-status applied (headers defer to the
gate sections).

## Remaining Risks

- The Flutter SDK must be installed to build/run the frontend
  (freely-installable, not a proprietary blocker like Q-015's); Design §11
  instructs honest disclosure if it is unavailable rather than a
  workaround.
- Keycloak becomes a new deployment component (the one new infra the
  Requirement authorized).

## Out-of-Scope Issues

None beyond the Non-Goals (no dashboards until Q-015 data, no broad UI, no
business-rule changes, no production Keycloak hardening yet).

## Recommendation

Present the bundle to the Product Owner at the implementation-authorization
gate. Self-review is PASS on all three documents; the Product Owner's
review + implementation authorization is the next and only remaining gate
before Codex implements.

## Gate Decision

**PASS** (self-review only — the Product Owner's bundle acceptance and
implementation authorization remain outstanding).
