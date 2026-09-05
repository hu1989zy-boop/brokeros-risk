# 2026-09-04 — Live `resolve` close-out (outstanding) + an authorization observation

Context: after Q-019 was accepted and pushed (`f295ab8`), a **post-acceptance
close-out** attempt tried to drive the **first end-to-end `resolve`** of the
Q-016 → Q-019 Risk Console arc through the live console (login → open a resolvable
case → resolve → close). This note records what was verified, why the live
`resolve` is still outstanding, and one authorization observation worth a separate
investigation. Nothing here changes Q-019's accepted status; it is a follow-up
tracker.

## What WAS verified live (real)

- **Q-019 association-projection endpoint, end to end.** On a case whose
  association tables were SQL-seeded, the console's `AssociationsPanel` rendered
  the real current decision + associated action, and `GET
  /api/risk-cases/{caseNumber}/associations` returned `SUCCESS` with the full
  projection (evidence associations incl. `eventRef` + disposition + replacement,
  decisions incl. current, actions incl. outcomes). This is the Q-019 deliverable,
  now proven live.
- (Also independently reproduced during the Q-019 review: backend full real-MySQL
  gate **309/0/0** incl. `Q019RiskCaseAssociationsMySqlTests` 4/4; frontend
  **150/150** + `vite build`.)
- A real gotcha found + noted: `docker compose up console-backend` **without
  `--build`** runs a stale backend image; the `/associations` route then 404s.
  Rebuild the image after backend changes.

## Why the live `resolve` is still OUTSTANDING

Reaching a *truly resolvable* case (status IN_REVIEW/ACTION_REQUIRED with a current
decision **and** an associated action) requires a consistent cross-module
Core-Domain provenance chain (trading account → evidence → decision-with-evidence
→ action-with-decision → outcome). Two paths were tried; both hit real depth:

1. **SQL-seed the provenance directly.** `resolve` validates every reference via
   the modules' `confirmProvenance` services (returns `RISK_CASE_REFERENCE_NOT_FOUND`
   / 422 when a ref is not recognized). Recognition requires each entity's
   *completeness*, not just a row: e.g. a decision is only recognized when its
   `decision_evidence_reference` linkage rows exist. Each layer seeded reveals the
   next requirement — a layered seed that is disproportionate to hand-assemble.
2. **Build the chain via the real create APIs** (the reliable path — the APIs
   enforce consistency). Blocked by the authorization observation below.

## Authorization observation — to investigate (NOT a confirmed defect)

Granting the console operator the cross-module record capabilities and creating the
chain via `POST /api/evidence` etc. failed: `POST /api/evidence` returns **403
`AUTHORIZATION_DENIED`** at `EvidenceRecordingService` line ~80
(`authorizationGuard.requireAllowed(actorContext, EvidenceCapabilities.RECORD)`),
even though:

- the operator actor is **HUMAN / ACTIVE** (so the `requireHuman` check at ~148 is
  not the cause, and it is downstream of the failing `requireAllowed` anyway);
- `evidence:record` was granted **through the proper security-bootstrap mechanism**
  on a **fresh database** (`createdActors=1`), not a raw insert;
- the exact `JdbcAuthorizationAdapter.DECISION_SQL` join for that actor_ref +
  `evidence:record` returns **exactly one `GRANTED` row**, which `toDecision` should
  turn into `allow`;
- the **same guard** honours `risk-case:read` (list → 200) and the module reads
  `evidence:read` / `decision:read` (GET by fake ref → 404, i.e. authorized) for
  the **same token/actor**.

So a properly-granted, single-row `GRANTED` `evidence:record` is denied while
sibling capabilities on the same actor pass. This could not be explained from the
static `decide()` query + `toDecision` logic; it needs **runtime debugging** of the
actor-context resolution / capability check for the write path. It is flagged as an
**observation**, not a confirmed bug — the full 309 real-MySQL gate and all
committed tests pass, so the guard is not broadly broken; the anomaly is specific
to this scenario and may reflect a nuance not yet understood.

Reproduction (local): `docker compose --profile console` fresh stack; temporarily
add `evidence:record` (etc.) to `deploy/keycloak/q016-security-bootstrap.json`;
bootstrap on a fresh DB; obtain an operator token via the PKCE flow; `POST
/api/evidence {operationId, subjectRef: <ACTIVE ta-…>, observationText}` → 403
`AUTHORIZATION_DENIED`. (The temporary bootstrap edit was reverted; committed
config stays least-privilege.)

## What remains verified about resolve/close (despite no live run)

- `resolve`/`close` were code-reviewed at Q-017; the backend real-MySQL gate
  (309/0/0) exercises the aggregate; the console `resolve` UI correctly fires the
  request with `expectedVersion` and surfaces the backend's `422` domain rejection
  as a typed error (approach-c), all observed during this attempt.

## Recommended follow-up

- **Investigate the authorization observation** with runtime logging on the write
  path (a real authz bug here would matter broadly).
- Provide a **proper resolvable-case seed harness** (or a system/service actor able
  to author provenance) so the live `resolve` E2E can run.
- Fold the **live end-to-end `resolve`** confirmation into a future Requirement —
  candidate **Q-020** (alongside the deferred Option B external-reference search).
