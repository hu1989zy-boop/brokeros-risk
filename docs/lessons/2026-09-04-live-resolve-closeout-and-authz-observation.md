# 2026-09-04 — Live `resolve` close-out (outstanding) + an authorization observation

Context: after Q-019 was accepted and pushed (`f295ab8`), a **post-acceptance
close-out** attempt tried to drive the **first end-to-end `resolve`** of the
Q-016 → Q-019 Risk Console arc through the live console (login → open a resolvable
case → resolve → close). This note records what was verified, why the live
`resolve` is still outstanding, and an authorization observation that a follow-up
investigation has since **root-caused (not a bug)**. Nothing here changes Q-019's
accepted status; it is a follow-up tracker.

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
   enforce consistency). Was believed blocked by an "authz anomaly"; that is now
   **root-caused and cleared** (see below) — the operator was simply missing one
   test-only grant, `trading-account-reference:read`. With the capability set in the
   table below, this path is expected to work; it has not yet been re-run live.

## Authorization observation — RESOLVED (root-caused; NOT a bug)

**Update (2026-09-04, static source investigation): this is not an authorization
bug. The authorization system worked exactly as designed; the earlier note
mis-attributed the denial.**

`POST /api/evidence` requires **two** capabilities, checked in order:

1. `evidence:record` — `EvidenceRecordingService.record` ~line 80
   (`requireAllowed(actorContext, EvidenceCapabilities.RECORD)`). This **passed** —
   it was granted, and the single `GRANTED` row is real.
2. `trading-account-reference:read` — reached at `EvidenceRecordingService` ~line
   111 via `eligibilityService.validateForNewRiskCaseAssociation(...)`, whose first
   act is `requireAllowed(actorContext, TradingAccountCapabilities.READ)`
   (`= "trading-account-reference:read"`) in
   `TradingAccountReferenceEligibilityService` line 31. This **failed** — that
   capability was never in the operator's grant set — throwing
   `AuthorizationDeniedException`.

That second denial is **not caught** by the `try/catch` around the eligibility call
(which only catches `TradingAccountAuthorityUnavailableException`), so it propagates
to the standard **403 `AUTHORIZATION_DENIED`**. Crucially,
`AuthorizationDeniedException` carries **only** `ResultCode.AUTHORIZATION_DENIED`
with **no capability field**, so the response for a missing
`trading-account-reference:read` is byte-identical to one for a missing
`evidence:record`. That is why the earlier note read the 403 as an `evidence:record`
denial: having confirmed the single `GRANTED` row for `evidence:record`, it did not
trace the flow to the **second** capability gate inside subject-eligibility
validation. (By design: recording evidence about a trading account requires
authority to read/validate that account reference.)

The temporary test bootstrap added the five `:record` capabilities + `risk-case:create`
but **not** `trading-account-reference:read` — the one, and only, missing grant. It
blocks at the very first create call, which is exactly where the 403 was seen.

### Full capability set to build the resolvable chain via the create APIs

Each record service also validates the referenced entity's provenance / subject
eligibility, each needing one extra READ capability:

| Endpoint | Primary | Secondary (provenance / eligibility) |
| --- | --- | --- |
| `POST /api/evidence` | `evidence:record` | `trading-account-reference:read` |
| `POST /api/decisions` | `decision:record` | `trading-account-reference:read` + `evidence:read` |
| `POST /api/actions` | `action:record` | `decision:read` |
| `POST /api/action-outcomes` | `action-outcome:record` | `action:read` |

The committed 13 already include `evidence:read` / `decision:read` / `action:read`,
so for a test operator the only additional grants needed to build the chain are the
five `:record` + `risk-case:create` **plus `trading-account-reference:read`**. These
are **test-only** grants; the committed least-privilege set for a read-only console
operator correctly excludes all record/create and trading-account capabilities.

## What remains verified about resolve/close (despite no live run)

- `resolve`/`close` were code-reviewed at Q-017; the backend real-MySQL gate
  (309/0/0) exercises the aggregate; the console `resolve` UI correctly fires the
  request with `expectedVersion` and surfaces the backend's `422` domain rejection
  as a typed error (approach-c), all observed during this attempt.

## Recommended follow-up

- ~~Investigate the authorization observation.~~ **Done** — root-caused above (not
  a bug; a missing `trading-account-reference:read` test grant). No code change.
- **Re-run the API-create path** to build a real resolvable case, granting the test
  operator the capability set in the table above (the five `:record` +
  `risk-case:create` + `trading-account-reference:read`), then drive the live
  `resolve`/`close` from the console — the first live `resolve` of the arc. This is
  the remaining outstanding item.
- Fold that **live end-to-end `resolve`** confirmation into a future Requirement —
  candidate **Q-020** (alongside the deferred Option B external-reference search).

## Lesson

`AuthorizationDeniedException` → `403 AUTHORIZATION_DENIED` carries no capability
discriminator, and a single flow can gate on **more than one** capability (a
primary write capability plus secondary read/eligibility capabilities on the
referenced entities). Do not attribute a 403 to the first capability you checked:
trace the whole request path for every `requireAllowed` before concluding. Verifying
one `GRANTED` row proves that one capability, not the request.
