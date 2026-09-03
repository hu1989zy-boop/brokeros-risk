# Q-018: Risk Console — Association Management (Group C)

## Status

V1 draft, by Claude Code holding the external Architect role, per the two
`docs/engineering/` governance documents. **The authoritative live status is
§17 (Current Gate)** (Execution Protocol §16). Requirement stage; the §5.3
product/architecture questions are surfaced for explicit Product Owner
confirmation (Decision Authority §16.2) rather than silently assumed.

- Requirement ID: `Q-018`
- Builds on: **Q-016** (React console) + **Q-017** (case lifecycle operations).
- Frontend technology: **React + TypeScript SPA** (ADR-018; unchanged).
- Depends on: the **already-committed** Risk Case association endpoints and the
  Evidence / Decision / Action / ActionOutcome modules (Q-011…Q-014).

## 1. Background

Q-016/Q-017 gave operators a console that reads Risk Cases and drives their
lifecycle (assign, review, resolve, close, …). But a Risk Case's substance is its
**associations** — the evidence, decisions, and actions attached to it — and the
console cannot yet manage them. Group C was deferred from Q-017 for one concrete
reason: associating evidence/decisions/actions requires the operator to supply a
**reference** (`ev-…`, `dc-…`, `ac-…`, `ao-…`) from another module, and those
modules currently expose only `POST` (create) and `GET /{ref}` (fetch one) —
**no list/search endpoint** — so there is no way to *discover* a reference. This
Requirement resolves how the console manages associations given that constraint.

Group C also **unblocks the rest of the lifecycle**: Q-017's `resolve`/`close`
are gated by a backend invariant requiring a **current decision with an
associated action**; that state is reachable only through the decision/action
association operations in this Requirement.

## 2. Existing Capability and Gap Analysis

| Operation | Endpoint (committed) | Reference needed | Console today |
| --- | --- | --- | --- |
| Associate evidence | `POST /{c}/evidence-associations` | `evidenceRef` (+ source) | none |
| Change evidence disposition | `POST /{c}/evidence-associations/{eventRef}/dispositions` | target event (on case) + optional `replacementEvidenceRef` | none |
| Associate decision | `POST /{c}/decision-associations` | `decisionRef` | none |
| Select current decision | `POST /{c}/decision-selection` | a decision already on the case | none |
| Associate action | `POST /{c}/action-associations` | `actionRef` | none |
| Reference action outcome | `POST /{c}/action-associations/{actionRef}/outcomes` | action on case + `outcomeRef` | none |

All six require the **`risk-case:associate`** capability (the operator does not
hold it yet). All are version-checked (`expectedVersion`) and audited.
References are opaque UUID-form strings the operator cannot reasonably memorize.

Two reference kinds:

- **On-case references** — the disposition target event, the decisions already
  associated (for selection), and the action already associated (for an
  outcome). These are **already visible** in the case detail/history and can be
  picked from what the console already shows — no new backend.
- **External references** — a *new* `evidenceRef` / `decisionRef` / `actionRef`,
  and an `outcomeRef`. These live in other modules with **no list endpoint**, so
  the operator must obtain the UUID some other way. This is the crux (§5.3).

## 3. Problem Statement

The console must let an authorized operator attach and manage the evidence,
decision, and action associations that give a Risk Case its substance — while the
source modules offer no way to browse their entities. The Requirement must decide
how references are supplied without weakening the thin-client model or shipping an
unusable "paste a raw UUID" flow.

## 4. Goals

1. Let an authorized operator perform the six Group C association operations from
   the console, version-safe and honoring the backend `ResultCode` contract.
2. Make **on-case** references selectable from what the console already displays
   (detail/history) — no guessing.
3. Give **external** references a safe, low-error entry path (see §5.3).
4. Unblock the full lifecycle: with decision + action associations in place,
   Q-017's `resolve`/`close` become reachable.
5. Preserve thin-client discipline and least privilege; keep backend change to
   the minimum the chosen §5.3 option requires.

## 5. Scope and Non-Goals

### 5.1 In Scope (V1 — CONFIRMED §5.3: Option A; all six Group C operations)

- The six Group C operations (§2), wired into the case detail as version-safe,
  reason-carrying actions reusing the Q-017 action framework.
- **On-case reference selection** from the case's own associations/history
  (disposition target; current-decision selection among associated decisions;
  the action for an outcome).
- **External reference entry** for new evidence/decision/action associations and
  outcome references, per the §5.3 decision (recommended: manual entry validated
  by a fetch-by-ref **preview** using the existing `GET /{ref}`).
- Rendering the case's current associations (effective evidence, associated
  decisions incl. the current one, associated actions + outcomes) so the operator
  sees state before and after each operation.

### 5.2 Non-Goals (candidates to defer — see §5.3)

- **Browse/search pickers** backed by new list/query endpoints on the
  Evidence/Decision/Action/ActionOutcome modules (Option B) — deferred unless the
  Product Owner pulls it into V1.
- Creating evidence/decisions/actions from the console (those modules' `POST`).
- Group E (Risk Case creation). Dashboards (need Q-015).

### 5.3 Product / architecture decisions — CONFIRMED by the Product Owner (2026-09-03)

1. **External reference sourcing — CONFIRMED: Option A** (manual entry +
   fetch-by-ref preview). The operator enters/pastes a reference; the console
   calls the existing `GET /api/{evidence|decisions|actions|action-outcomes}/{ref}`
   to **validate and preview** the entity before associating. **No new backend
   endpoint.** Option B (browse/search pickers backed by new list endpoints) is
   **deferred** to a future Requirement.
2. **Capabilities — CONFIRMED.** Grant the console-operator role
   **`risk-case:associate`** (covers all six operations) **plus the read
   capability the source modules' `GET /{ref}` enforces** (for the fetch-preview),
   to be pinned exactly at Architecture. If that source-module read grant turns
   out heavier than intended, the confirmed fallback is manual entry **without**
   preview (the association operations themselves are unaffected).
3. **Scope within Group C — CONFIRMED: all six** operations (evidence associate +
   disposition, decision associate + selection, action associate + outcome).
4. **Evidence `source` field — CONFIRMED:** captured as an operator-entered
   free-text provenance label (≤64), no fixed vocabulary in V1.

## 6. Definitions

- **Association operation** — an authorized, version-checked attach/disposition/
  selection/outcome operation already exposed by `RiskCaseController`.
- **On-case reference** — a reference already attached to the case, selectable
  from its detail/history.
- **External reference** — a `ev-/dc-/ac-/ao-` reference from another module,
  supplied per §5.3.
- **Fetch-by-ref preview** — validating/showing an entered reference via the
  existing `GET /{ref}` before associating.

## 7. Functional Requirements

- **Q018-FR-01** An authorized operator can **associate a new evidence** item
  (evidenceRef + source + reason) and see it in the case's effective evidence.
- **Q018-FR-02** An operator can **change an evidence association's disposition**
  (SUPERSEDED | INVALIDATED | WITHDRAWN, optional replacement evidence + source +
  reason), selecting the target from the case's existing evidence associations.
- **Q018-FR-03** An operator can **associate a decision** (decisionRef + reason)
  and **select the current decision** from the decisions associated to the case.
- **Q018-FR-04** An operator can **associate an action** (actionRef + reason) and
  **reference an action outcome** (outcomeRef + reason) for an action on the case.
- **Q018-FR-05** External references are entered per §5.3; when preview is in
  scope, an invalid/unknown reference is caught (via `GET /{ref}`) before the
  association request is sent.
- **Q018-FR-06** Every operation sends `expectedVersion`; a
  `RISK_CASE_VERSION_CONFLICT` reloads the case and preserves operator input.
- **Q018-FR-07** The case detail renders current associations (effective
  evidence, associated + current decision, associated actions + outcomes) so
  state is visible before/after each operation; backend `403`/`ResultCode`
  rejections are surfaced as typed errors (backend authoritative).
- **Q018-FR-08** No actor identity in any request body; thin-client preserved.

## 8. Security Requirements

- Authorization stays server-side; the operator is granted **`risk-case:associate`**
  (and, if Option A preview is included, the source-module read capability — §5.3).
  Least privilege otherwise unchanged.
- No tokens/reasons/references or entity content in logs or artifacts.

## 9. Data / Contract Requirements

- Consume the existing association request/response DTOs and (for preview) the
  existing `GET /{ref}` responses via typed TypeScript models.
- **Option A: no new backend endpoint.** **Option B (if chosen): additive,
  bounded, authorized list endpoints only — no aggregate/business-rule change**,
  decided at Architecture.

## 10. Acceptance Criteria

1. An authorized operator can perform all in-scope Group C operations end to end
   against the live backend; the case's associations and version update.
2. On-case references are selected from displayed state; external references use
   the §5.3 path (and, if preview is in scope, an unknown ref is rejected before
   send).
3. A concurrent change triggers `RISK_CASE_VERSION_CONFLICT` and a reload with
   input preserved.
4. After associating a decision, selecting it as current, and associating an
   action to it, a subsequent Q-017 `resolve` is reachable (lifecycle unblocked).
5. Thin-client holds: no Q-008…Q-014 aggregate/business-rule/migration change; no
   identity in bodies; only the §5.3-scoped additions (capability grant, and list
   endpoints only if Option B).
6. Frontend tests (Vitest/RTL/MSW per operation incl. conflict/error/preview) and
   a live Playwright association slice pass; typecheck/build clean.

## 11. Technical Constraints

- React 18 + TS SPA (ADR-018); reuse the Q-017 action registry/runner and the
  Q-016 axios client. Thin client; per-operation `expectedVersion`.
- Option A: no new production infrastructure or backend endpoint. Option B: only
  additive bounded read endpoints.

## 12. Dependencies

- Q-016 + Q-017 console. Q-008 association endpoints + Q-011…Q-014 modules +
  Q-009 (committed, consumed). The `risk-case:associate` grant (§5.3). Independent
  of Q-015.

## 13. Verification Plan

- Component/unit (Vitest + RTL + MSW) per operation: success, pending, validation,
  ordinary `ResultCode` error, `403`, `RISK_CASE_VERSION_CONFLICT` reload; and (if
  preview) unknown-ref rejection.
- Live Playwright: on a seeded case, associate a decision → select it as current →
  associate an action → (optionally) resolve, asserting association + version
  state — this also demonstrates AC 4 (lifecycle unblock).
- Typecheck + build clean; backend diff empty (Option A) or additive-read-only
  (Option B).

## 14. Risks and Inputs

- **Reference discovery gap** — the core risk; §5.3 Option A mitigates with
  preview, Option B removes it at a backend cost.
- **Cross-module read grant** — fetch-preview may require read access to four
  modules; confirm scope (§5.3(2)).
- **Association state complexity** — evidence supersession/disposition and
  current-decision selection have real domain rules; the backend remains
  authoritative and the console surfaces its `ResultCode`s.

## 15. Deliverables

- This Requirement; then (after approval + §5.3) the Architecture / ADR /
  Implementation Design bundle (§16.5-B) and a Codex prompt; then implementation
  and independent review.

## 16. Review Checklist

- [ ] §5.3 decisions confirmed (reference sourcing A vs B; capability grant;
      Group C scope; `source` handling).
- [ ] Thin-client / minimal-backend confirmed for the chosen option.
- [ ] Product Owner Gate Decision recorded (§17).

## 17. Current Gate

Q-018 Requirement status: **APPROVED — V1 — 2026-09-03 — Product Owner.**
Gate Decision: **PASS.** §5.3 CONFIRMED: reference sourcing = **Option A**
(manual entry + fetch-by-ref preview via existing `GET /{ref}`; Option B new list
endpoints deferred); grant the operator **`risk-case:associate`** + the source-
module read capability for preview (pinned at Architecture; fallback = manual
entry without preview); **all six** Group C operations in V1; evidence `source` =
operator-entered free-text label.

Q-018 Architecture V1 / ADR-020 / Implementation Design V1: **ACCEPTED (bundle) —
2026-09-03 — Product Owner** at the implementation-authorization gate (§16.5-B).
ADR-020 is **Accepted**. **Implementation AUTHORIZED — 2026-09-03 — Product
Owner.** The Codex implementation prompt
(`prompts/Q-018-Implementation-Prompt.md`) is **CLEARED FOR USE**.

Confirmed build shape: reuse the Q-017 action registry + `useCaseAction` runner
for the six Group C operations; external references via a `ReferenceInput` that
previews through the existing `GET /{ref}`; on-case references picked from the
existing detail/history; the operator granted `risk-case:associate` +
`evidence:read` + `decision:read` + `action:read` + `action-outcome:read` in the
security bootstrap. Thin-client; **no new backend endpoint**; **no backend
code/aggregate/migration change** — the only backend-adjacent change is that
capability grant.

Next gate: Codex implements Q-018 V1 → Claude Code independent review (incl. the
live association slice and the resulting Q-017 `resolve` reachability) → Product
Owner acceptance → commit. Option B (browse/search pickers) and Group E (case
creation) remain deferred to future Requirements.
