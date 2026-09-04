# Q-019: Risk Case Association Projection (read endpoint)

## Status

V1 draft, by Claude Code holding the external Architect role, per the two
`docs/engineering/` governance documents. **The authoritative live status is
§17 (Current Gate)** (Execution Protocol §16). Requirement stage; the §5.3
scope questions are surfaced for explicit Product Owner confirmation (Decision
Authority §16.2) rather than silently assumed.

- Requirement ID: `Q-019`
- Type: **backend additive read** (+ a thin console follow-through). This is the
  first deliberate backend addition since Q-016's list endpoint; the Q-016/017/018
  "no backend change" constraint is deliberately relaxed for this bounded read.
- Depends on: the committed Q-008 Risk Case aggregate + association tables, Q-009
  (JWT verification), and the Q-018 console.

## 1. Background

Q-018 gave the console association management, but hit a real gap (defect D2): the
Risk Case detail/history does not expose an evidence association's **event
reference** (`EvidenceAssociationEventRef`) — history exposes only `{version,
eventType, affectedRef, actorRef}`, and detail exposes only `currentDecisionRef`.
So the disposition **on-case picker** could not be built (it fell back to
manual UUID entry, PO decision B1), and the `AssociationsPanel` is only a bounded
**reconstruction from the history page**, not an authoritative current view.

The underlying data already exists and is already queried internally — the
repository exposes `findAllEffectiveEvidence`, `findAllEffectiveActions`,
`findEvidenceEvent` (with its event ref), and the decision-association tables.
Q-019 exposes a bounded, authorized **authoritative current-associations
projection** so the console can show real state and pick on-case references
(including the disposition event ref) without guessing.

## 2. Existing Capability and Gap Analysis

| Data | Persisted? | Internally queryable? | Exposed via REST? |
| --- | --- | --- | --- |
| Effective evidence (assoc + disposition + source + replacement) | Yes | `findAllEffectiveEvidence` | **No** |
| Evidence association **event ref** (`associationEventRef`) | Yes | `findEvidenceEvent` / event rows | **No** — the D2 gap |
| Associated decisions (+ which is current) | Yes | current on aggregate; associations in table | Only `currentDecisionRef` |
| Effective actions (+ referenced outcomes) | Yes | `findAllEffectiveActions` | **No** |

The projection is a **read**; it returns the case's own association references and
states (not the external entities' content), so it needs only the existing
**`risk-case:read`** capability — no new grant.

## 3. Problem Statement

The console (and any client) needs an authoritative, bounded, authorized way to
read a Risk Case's **current associations** — evidence associations with their
event refs and dispositions, associated decisions with the current one marked, and
actions with their outcomes — so on-case pickers (especially evidence disposition)
and the associations view are correct rather than reconstructed or manual.

## 4. Goals

1. Add **one additive, bounded, authorized read endpoint** exposing a Risk Case's
   current-associations projection, reusing the existing internal queries.
2. Include the **evidence association event ref** so the console's disposition
   on-case picker (Q-018 D2) works without manual entry.
3. Keep it a **pure read**: no aggregate/business-rule change, no migration, no new
   capability (uses `risk-case:read`).
4. Switch the Q-018 console to consume it (authoritative `AssociationsPanel` +
   disposition on-case picker), replacing the history-reconstruction / manual
   fallback.
5. Unblock the full **live** lifecycle E2E: with real decisions/actions associated
   and visible, the Q-017 `resolve` path becomes verifiable end to end.

## 5. Scope and Non-Goals

### 5.1 In Scope (V1 — CONFIRMED §5.3: single endpoint + console follow-through)

- A backend read endpoint returning the current-associations projection for one
  case (evidence associations incl. event ref + disposition + source + replacement;
  associated decisions incl. current; effective actions incl. outcomes), bounded
  and authorized by `risk-case:read`.
- Any small **additive read query** needed to back it (e.g. list associated
  decisions / evidence association events) — read-only, no schema change.
- The console follow-through: `AssociationsPanel` reads the projection
  (authoritative), and the evidence-disposition target uses an **on-case picker**
  from it (replacing the Q-018 B1 manual fallback).

### 5.2 Non-Goals (defer)

- **Option B — cross-module browse/search** for *external* reference discovery
  (list/search endpoints on Evidence/Decision/Action/ActionOutcome). Different
  concern (discovery of new refs), larger surface — a separate Requirement
  (candidate Q-020). See §5.3.
- Any aggregate/business-rule/migration change; any write operation.
- Group E (case creation); dashboards (need Q-015).

### 5.3 Scope decisions — CONFIRMED by the Product Owner (2026-09-03)

1. **Endpoint shape — CONFIRMED: single endpoint.**
   `GET /api/risk-cases/{caseNumber}/associations` returns the whole current-
   associations projection (one authoritative view).
2. **Console follow-through — CONFIRMED: in V1.** Q-019 delivers the backend
   endpoint **and** the console switch (authoritative `AssociationsPanel` +
   evidence-disposition on-case picker), closing the Q-018 D2 gap end to end.
3. **Option B (external-ref search) — CONFIRMED: deferred** to a separate
   Requirement (candidate Q-020). Q-019 stays focused on the association projection.
4. **Bounding — CONFIRMED:** return the case's associations in full with a sane
   server cap (matching the Q-016 bounded-projection discipline); no pagination in
   V1.

## 6. Definitions

- **Association projection** — an authoritative, read-only view of a case's current
  evidence/decision/action associations, including the refs the console needs.
- **Evidence association event ref** — `EvidenceAssociationEventRef`, the id of an
  evidence association event (the disposition target); absent from today's reads.

## 7. Functional Requirements

- **Q019-FR-01** `GET /api/risk-cases/{caseNumber}/associations` returns, for an
  authorized reader, the case's current associations: evidence associations (each
  with its event ref, evidence ref, disposition, source, replacement ref if any),
  associated decisions (each ref, with the current one marked), and effective
  actions (each ref, with referenced outcome refs).
- **Q019-FR-02** The endpoint is authorized by `risk-case:read`, returns the
  standard `ApiResponse` envelope, and is bounded (§5.3(4)); a missing case yields
  the standard not-found `ResultCode`.
- **Q019-FR-03** No aggregate/business-rule/migration change; the endpoint reads
  existing tables via existing/additive read queries only.
- **Q019-FR-04** The console's `AssociationsPanel` reads this projection as the
  authoritative source, and the evidence-disposition target is chosen from an
  on-case picker over it (replacing the Q-018 manual fallback).
- **Q019-FR-05** With decisions/actions associated and visible via the projection,
  the console can drive and the reviewer can live-verify the Q-017 `resolve` path.

## 8. Security Requirements

- Authorization server-side via `risk-case:read` (already granted); **no new
  capability**. The projection returns the case's own association refs/states, not
  external entity content. No identity in requests beyond the Bearer JWT.

## 9. Data / Contract Requirements

- One additive REST endpoint + a bounded projection response DTO; typed console
  models. **No new table, column, or migration**; additive read queries only.

## 10. Acceptance Criteria

1. The endpoint returns the correct current-associations projection (incl. evidence
   event refs) for a case, authorized by `risk-case:read`, against real MySQL.
2. No aggregate/business-rule/migration change; `git diff` on aggregates/migrations
   is empty; only additive read query/endpoint/DTO code is added.
3. The console `AssociationsPanel` shows the authoritative projection and the
   disposition target is an on-case picker (no manual UUID) — Q-018 D2 closed.
4. A live slice associates a decision → selects it → associates an action, the
   projection reflects it, and the Q-017 `resolve` path is then reachable and
   exercised end to end.
5. Backend tests (real-MySQL for the endpoint) + frontend tests (projection
   consumption) pass; typecheck/build clean.

## 11. Technical Constraints

- Additive read only; reuse existing repository queries (`findAllEffectiveEvidence`,
  `findAllEffectiveActions`, evidence event lookups) + minimal additive read
  queries. Bounded projection; `risk-case:read`.
- Console: React (ADR-018), reuse the Q-016 client + Q-018 association UI.

## 12. Dependencies

- Q-008 aggregate + association tables; Q-009; the Q-018 console. Independent of
  Q-015.

## 13. Verification Plan

- Backend: real-MySQL test asserting the projection (incl. an evidence association
  with an event ref + disposition, associated decisions incl. current, actions +
  outcomes) and `risk-case:read` authorization; the full repository gate stays green.
- Frontend: the panel/picker consume the projection (unit + component); a live
  slice (seed decision/action, associate via console, read projection, then
  `resolve`) — the first end-to-end `resolve` verification.
- No aggregate/migration diff.

## 14. Risks and Inputs

- **Scope creep into Option B** — mitigated by §5.3(3) deferral.
- **Projection/aggregate drift** — the projection must match the aggregate's
  effective-association semantics; back it with the existing effective-* queries
  and a real-MySQL test.

## 15. Deliverables

- This Requirement; then (after approval + §5.3) the Architecture / ADR /
  Implementation Design bundle (§16.5-B) and a Codex prompt; then implementation
  and independent review.

## 16. Review Checklist

- [ ] §5.3 decisions confirmed (endpoint shape; console follow-through in V1;
      Option B deferral; bounding).
- [ ] Additive-read-only / no aggregate/migration change confirmed.
- [ ] Product Owner Gate Decision recorded (§17).

## 17. Current Gate

Q-019 Requirement status: **APPROVED — V1 — 2026-09-03 — Product Owner.**
Gate Decision: **PASS.** §5.3 CONFIRMED: single `GET /api/risk-cases/{caseNumber}/
associations` projection endpoint; console follow-through (authoritative panel +
disposition on-case picker) included in V1; Option B (external-ref search) deferred
to a separate Requirement (Q-020); bounded full projection with a server cap (no
pagination). Additive backend read only — **no aggregate/business-rule/migration
change, no new capability** (`risk-case:read`).

Q-019 Architecture V1 / ADR-021 / Implementation Design V1: **ACCEPTED (bundle) —
2026-09-03 — Product Owner** at the implementation-authorization gate (§16.5-B).
ADR-021 is **Accepted**. **Implementation AUTHORIZED — 2026-09-03 — Product
Owner.** The Codex implementation prompt
(`prompts/Q-019-Implementation-Prompt.md`) is **CLEARED FOR USE**.

Confirmed build shape: a single additive `GET /api/risk-cases/{caseNumber}/
associations` read endpoint on the existing controller, `risk-case:read`-authorized,
returning a bounded `RiskCaseAssociationsResponse` (evidence associations incl.
event ref + disposition + source + replacement; associated decisions incl. current;
effective actions incl. outcomes); backed by the existing effective-* queries plus
two additive read-only `SELECT`s (evidence events, decision associations). **No
aggregate/business-rule/migration change, no new table/column, no new capability.**
Console switches `AssociationsPanel` to the authoritative projection and the
evidence-disposition target to an on-case picker (closes Q-018 D2).

Q-019 implementation (Codex, v1
`review/q-019/review-q-019-v1-implementation-20260903-204945`): the additive read
endpoint + query-service method + two read-only queries + DTO + real-MySQL test,
and the console switch (authoritative `AssociationsPanel` + evidence-disposition
on-case `eventRef` picker). No aggregate/business-rule/migration change; no new
capability.

Claude Code independent review: **PASS — 2026-09-03** — see
`review/q-019/review-q-019-v2-claude-code-independent-review-20260903-210401/`.
Independently reproduced: backend full real-MySQL gate **309/0/0** (incl.
`Q019RiskCaseAssociationsMySqlTests` **4/4**); frontend **150/150** + typecheck 0 +
`vite build`; additive-read-only boundary confirmed (endpoint uses `risk-case:read`,
two read-only `SELECT`s, bounded projection with a server cap, no aggregate/
migration/capability change). Console closes the Q-018 D2 gap (authoritative panel +
real disposition picker). One live item outstanding (AC 4): the console-driven
`resolve` slice needs a seeded decision/action (the same deep Core-Domain seed
noted since Q-017); the backend real-MySQL test already proves the resolvable
associated state is reachable.

Q-019 (V1) acceptance: **ACCEPTED — 2026-09-03 — Product Owner**; committed with
the implementation + v2 review package.

Q-019 status: **COMPLETE — 2026-09-03** (Risk Case association projection endpoint;
console switched to the authoritative projection + evidence-disposition on-case
picker — Q-018 D2 closed). The console-driven live `resolve` slice (AC 4) is
scheduled when a decision/action Core-Domain seed is set up. Option B (external-ref
search, Q-020) and Group E remain deferred; Q-015 remains parked awaiting the
MT4/MT5 SDK.
