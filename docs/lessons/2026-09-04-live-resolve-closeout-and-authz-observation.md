# 2026-09-04 — Live `resolve` close-out (ACHIEVED) + an authorization observation

Context: after Q-019 was accepted and pushed (`f295ab8`), a **post-acceptance
close-out** drove the **first end-to-end `resolve` + `close`** of the
Q-016 → Q-019 Risk Console arc against a real, API-built provenance chain. This note
records what was verified, the now-**completed** live `resolve` (2026-09-05), and an
authorization observation that a follow-up investigation **root-caused (not a bug)**.
Nothing here changes Q-019's accepted status; it is a follow-up tracker.

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

## Live `resolve` — ACHIEVED (2026-09-05)

The first live end-to-end `resolve` + `close` of the arc was executed against a real
provenance chain built through the create APIs, on a fresh local stack
(`docker compose --profile console` + Keycloak realm). Verified at both the API and
database layers, then the stack was torn down (`down -v`) and the two test-only
config edits reverted.

**Chain built (real create APIs, operator token):** seed one ACTIVE
`trading_account_reference` (`ta-…`) → `POST /api/evidence` → `ev-…` →
`POST /api/decisions` (refs the evidence) → `dec-…` → `POST /api/risk-cases`
(`RC-…`, OPEN) → self-assign → `POST /review-start` (IN_REVIEW) →
`POST /decision-associations` (the domain sets it as the **current** decision).
`GET /associations` then showed the decision `current: true` — a resolvable case.

**Resolve + close (the identical requests the console's buttons issue):**
`POST /{case}/resolutions` `{outcome: MONITORING_ONLY, evidenceRefs:[], actionRefs:[],
expectedVersion:4}` → **HTTP 201, status RESOLVED**; `POST /{case}/closure` → **HTTP
200, status CLOSED**. The resolve's `decisionQuery.requireRecognized(currentDecisionRef)`
(`confirmProvenance`) passed against the real decision — the exact path that had
never before run live.

**Independent DB verification** (queried directly, not just the HTTP responses):

- `risk_case`: `status=CLOSED`, `version=6`, `current_decision_ref=dec-…`.
- `risk_case_transition_history`: `CREATE→OPEN`, `BEGIN_REVIEW→IN_REVIEW`,
  **`RESOLVE: IN_REVIEW→RESOLVED`**, **`CLOSE: RESOLVED→CLOSED`**.
- `risk_case_resolution_history`: cycle 1, `outcome_code=MONITORING_ONLY`,
  `decision_ref=dec-…`, resolved-by the operator actor.

**Two nuances confirmed in the domain along the way:**

1. `beginReview` calls `requireAssignment()` — a case must be **assigned** before it
   can enter review (an unassigned OPEN case → `RISK_CASE_INVARIANT_VIOLATION`).
2. Resolving from **IN_REVIEW** needs only a current decision — **no associated
   action is required** (the "current decision + associated action" rule is
   `markActionRequired`'s precondition for the ACTION_REQUIRED path, not resolve's).
   `associateDecision` also *sets* the current decision directly, so a single
   decision-association suffices (a second `decision-selection` for the same ref
   fails "already current").

The earlier "authz anomaly" that had blocked the API path is root-caused and cleared
below — the operator was simply missing one test-only grant,
`trading-account-reference:read`.

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

## On "console-driven"

The final `resolve` + `close` were issued as the **identical authenticated HTTP
requests the console's Resolve/Close buttons fire** (`POST /{case}/resolutions`,
`/closure` with the operator's Bearer token), rather than by clicking the buttons in
the browser — entering even a throwaway dev password into a login form is disallowed
by the assistant's safety rules. The backend logic exercised (authorization,
`confirmProvenance` on the current decision, the resolve/close transitions) is
exactly what the UI triggers. The console UI layer itself was already live-verified
earlier in the arc (Q-017 assign/priority/cancel; Q-019 associations panel).

## Recommended follow-up

- ~~Investigate the authorization observation.~~ **Done** — root-caused (not a bug;
  a missing `trading-account-reference:read` test grant). No code change.
- ~~Re-run the API-create path and drive the live resolve/close.~~ **Done
  (2026-09-05)** — see "Live `resolve` — ACHIEVED" above.
- Optional: a reusable **resolvable-case seed harness** (or a system/service actor
  able to author provenance) would let this run be re-executed on demand — worth
  folding into a future Requirement (candidate **Q-020**, alongside the deferred
  Option B external-reference search) if the live `resolve` E2E should become a
  standing check.

## Lesson

`AuthorizationDeniedException` → `403 AUTHORIZATION_DENIED` carries no capability
discriminator, and a single flow can gate on **more than one** capability (a
primary write capability plus secondary read/eligibility capabilities on the
referenced entities). Do not attribute a 403 to the first capability you checked:
trace the whole request path for every `requireAllowed` before concluding. Verifying
one `GRANTED` row proves that one capability, not the request.
