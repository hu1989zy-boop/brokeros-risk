# Q-017: Risk Console — Case Lifecycle Operations

## Status

V1 draft, by Claude Code holding the external Architect role, per the two
`docs/engineering/` governance documents. **The authoritative live status is
§17 (Current Gate)** (Execution Protocol §16). Requirement stage; the §5.3
product/authorization questions are surfaced for explicit Product Owner
confirmation (Decision Authority §16.2) rather than silently assumed.

- Requirement ID: `Q-017`
- Builds on: **Q-016** (the React Risk Console — complete, `efc169f`). This is a
  thin-client feature expansion, not a new surface.
- Frontend technology: **React + TypeScript SPA** (ADR-018; unchanged).
- Depends on: the **already-committed** Risk Case backend operations (the full
  `RiskCaseController`), Q-009 JWT verification, and the security capability
  model. **No new backend business logic, aggregate change, or migration is
  expected** — the endpoints already exist; Q-017 wires them into the console.

## 1. Background

Q-016 delivered the Risk Console foundation: authentication, routing, the typed
backend client, and one vertical slice (login → list → detail/history → add
note). It deliberately surfaced only one write operation to prove the stack, not
breadth. Meanwhile the Q-008 Risk Case aggregate already exposes the full
operator workflow over HTTP — roughly twenty authorized, version-checked
operations — none of which the console yet surfaces. Operators can currently
only read cases and add a note; everything else (assign, drive the review
workflow, resolve, close, …) still requires raw HTTP.

## 2. Existing Capability and Gap Analysis

| Capability | Backend (committed) | Console (Q-016) | Gap (Q-017) |
| --- | --- | --- | --- |
| Read list / detail / history | Yes | Yes | — |
| Add investigation note | Yes | Yes | — |
| Assign / reassign | `POST /assignments` | No | **wire up** |
| Change priority | `POST /priority-changes` | No | **wire up** |
| Begin review / mark action-required / return to review | `POST /review-start`, `/action-required`, `/review-return` | No | **wire up** |
| Resolve / close / cancel | `POST /resolutions`, `/closure`, `/cancellation` | No | **wire up** |
| Resume resolved / reopen closed | `POST /resume`, `/reopen` | No | **wire up** |
| Correct a note | `POST /notes/{ref}/corrections` | No | **wire up** |
| Evidence / decision / action associations (+ dispositions, selection, outcomes) | `POST /evidence-associations`, `/decision-associations`, `/action-associations`, … | No | scope decision (§5.3) |
| Create a case | `POST /` (needs an eligible trading-account subject) | No | scope decision (§5.3) |

Every backend operation is authorized (per-capability), version-checked
(`expectedVersion` → `RISK_CASE_VERSION_CONFLICT`), and audited server-side. The
console stays a thin client: it renders bounded data, calls these operations, and
honors the backend's answer.

## 3. Problem Statement

The Risk Console cannot yet be used to actually *operate* a risk case through its
lifecycle. Operators need to assign, prioritize, move a case through
review → action-required → resolved → closed (and cancel/reopen/resume), and
correct notes — all from the UI, with the backend remaining the sole authority
for validation, authorization, and concurrency.

## 4. Goals

1. Surface the Risk Case **lifecycle operations** in the console as guarded,
   version-safe actions, each honoring the backend `ResultCode` contract
   (including `RISK_CASE_VERSION_CONFLICT` reload).
2. Make the available actions **reflect case state and the operator's
   capabilities** — show/enable only what the backend would accept, but never
   re-decide authorization or invariants client-side (the backend still rejects).
3. Keep the thin-client discipline and the typed contract; **no backend business
   logic / migration change**.
4. Preserve auditability and least privilege — the console requests only the
   capabilities the operator role is granted (§5.3).

## 5. Scope and Non-Goals

### 5.1 In Scope (V1 — CONFIRMED §5.3: Groups A + B + D)

- **Group A — Lifecycle/workflow transitions:** begin review, mark action
  required, return to review, resolve, close, cancel, resume resolved, reopen
  closed. Each with its required `reason`/inputs and `expectedVersion`.
- **Group B — Assignment & priority:** assign/reassign (assignee + reason),
  change priority (priority + reason).
- **Group D — Note correction:** correct an existing note.
- Cross-cutting: a consistent action UX (confirmation, reason capture, pending
  state, typed error surfacing, version-conflict reload), and state-aware
  action availability derived from case status + a capability probe.

### 5.2 Non-Goals (CONFIRMED deferred — see §5.3)

- **Group C — Association management** (evidence/decision/action associations,
  dispositions, decision selection, outcome references): these require the
  operator to supply evidence/decision/action/outcome references, and no
  cross-module browse/query UI exists yet to pick them. Recommended **deferred**
  to a later Requirement (or done with manual reference entry if the PO wants it
  now).
- **Group E — Case creation** (`POST /`): requires an *eligible* trading-account
  subject (Q-010), and no trading-account query/picker UI exists. Recommended
  **deferred** to a Requirement that includes subject selection.
- Dashboards / real-time exposure views (need Q-015 trading data).
- Any new backend endpoint, aggregate rule, migration, or bulk/batch operation.
- Role administration / capability management UI.

### 5.3 Product / authorization decisions — CONFIRMED by the Product Owner (2026-09-03)

1. **V1 operation scope — CONFIRMED: Groups A + B + D** (full lifecycle workflow +
   assignment/priority + note correction). **Group C (associations) and Group E
   (case creation) are deferred** to a follow-up Requirement.
2. **Console operator capabilities — CONFIRMED: a single console-operator role
   granted the full V1 lifecycle capability set** (`risk-case:read`, `:note`,
   `:assign`, `:review`, `:resolve`, `:close`, `:cancel`, `:reopen`). No role
   split in V1. This is a governed grant to the dev security bootstrap and the
   production authorization model (config, not business logic).
3. **Terminal actions — CONFIRMED: yes.** `close`, `cancel`, and `resolve`
   require an explicit confirmation step and a mandatory reason in the console.
4. **Action availability source — CONFIRMED: approach (c).** The console shows all
   actions valid for the case's current status and relies on the backend's
   `403`/`ResultCode` to reject an unauthorized attempt (surfaced as a typed
   error). No JWT-claims parsing or capability-probe is added in V1 — so **no
   backend change is needed for action availability**.

## 6. Definitions

- **Lifecycle operation** — an authorized, version-checked Risk Case state or
  attribute change already exposed by `RiskCaseController`.
- **State-aware action** — a UI action shown/enabled based on the case's current
  status (and, if available, the operator's capabilities), always still enforced
  by the backend.
- **Version-safe** — every write sends `expectedVersion`; a
  `RISK_CASE_VERSION_CONFLICT` prompts a reload without losing operator input.

## 7. Functional Requirements

- **Q017-FR-01** From a case detail, an authorized operator can **assign/reassign**
  (assignee + reason) and **change priority** (priority + reason); the detail
  reflects the new assignee/priority and version.
- **Q017-FR-02** An authorized operator can drive the **review workflow**: begin
  review (OPEN → IN_REVIEW), mark action required (IN_REVIEW → ACTION_REQUIRED),
  return to review (ACTION_REQUIRED → IN_REVIEW), each with a reason.
- **Q017-FR-03** An authorized operator can **resolve** a case (outcome +
  resolution summary + optional evidence/action references), **close** it, and
  **cancel** it (with optional duplicate case reference) — each with a mandatory
  confirmation and reason.
- **Q017-FR-04** An authorized operator can **resume** a resolved case and
  **reopen** a closed case (reason + optional assignee).
- **Q017-FR-05** An authorized operator can **correct an existing note**.
- **Q017-FR-06** Every operation sends `expectedVersion`; on
  `RISK_CASE_VERSION_CONFLICT` the console reloads the case and preserves the
  operator's unsaved input, prompting a retry.
- **Q017-FR-07** Available actions are **state-aware**: only actions valid for the
  current status are offered; a backend `403`/`ResultCode` rejection is surfaced
  as a typed, readable error and never silently swallowed.
- **Q017-FR-08** No operation embeds actor identity in the request body; identity
  is the verified Bearer JWT only (thin-client, unchanged from Q-016).

## 8. Security Requirements

- Authorization remains **server-side** (Q-009 + per-capability guards). The
  console may hide/disable an action for UX but must always call the backend and
  honor its result.
- The console-operator role is granted **only** the capabilities V1 needs (§5.3);
  least privilege preserved. Any capability grant is a governed change to the
  security bootstrap/authorization config (no business-logic change).
- No tokens, credentials, reasons, or case content in logs or artifacts.

## 9. Data / Contract Requirements

- Consume the existing `ApiResponse`/`ResultCode` envelope and the existing
  operation request/response DTOs via typed TypeScript models; a contract change
  is a compile-time signal.
- No new backend endpoint or field is required for Groups A/B/D. (If §5.3 admits
  Group C/E or a capability probe, any addition must be **additive and
  read-only**, decided at Architecture.)

## 10. Acceptance Criteria

1. From the console, an authorized operator can execute each in-scope operation
   (per §5.3) end to end against the live backend, with the detail/history
   updating and the version advancing.
2. Each terminal action (resolve/close/cancel) requires explicit confirmation and
   a reason; a missing/invalid input is blocked client-side and, if sent, by the
   backend.
3. A concurrent change triggers `RISK_CASE_VERSION_CONFLICT`, and the console
   reloads without losing operator input.
4. An operator lacking a capability sees the action unavailable or receives a
   typed authorization error; the backend authorization is never bypassed.
5. Thin-client and no-backend-change hold: `git diff` shows no Q-008…Q-014
   aggregate/business-rule/migration change; identity is never body-supplied.
6. Frontend tests (Vitest/RTL/MSW for each operation incl. the conflict and
   error paths) and the live Playwright slice pass; `npm run build`/typecheck/
   analyze clean.

## 11. Technical Constraints

- React 18 + TypeScript SPA (ADR-018); TanStack Query mutations; Ant Design;
  reuse the Q-016 axios client (Bearer, 401-refresh, 403 mapping, envelope).
- Thin client; per-operation `expectedVersion`; bounded reads only.
- No new production infrastructure; Keycloak unchanged.

## 12. Dependencies

- Q-016 console (complete). Q-008 Risk Case operations + Q-009 JWT verification
  (committed, consumed). The console-operator capability grant (§5.3) — dev
  security bootstrap + production authorization model.
- Independent of Q-015 (no trading-data dependency for A/B/D).

## 13. Verification Plan

- Component/unit (Vitest + RTL + MSW) for each operation: success, pending,
  validation, ordinary `ResultCode` error, `403`, and `RISK_CASE_VERSION_CONFLICT`
  reload-with-input-preserved.
- Live Playwright slice (extend the Q-016 harness): drive a case through
  assign → begin review → (priority) → resolve/close, asserting state + version
  transitions against the live stack.
- Typecheck + `vite build` + analyze clean; backend diff empty.

## 14. Risks and Inputs

- **Capability scope creep / over-grant** — mitigated by §5.3(2) least-privilege
  confirmation.
- **State-machine mismatch** — the console's state-aware availability must match
  the backend's transition rules; the backend remains authoritative, and tests
  assert the allowed transitions.
- **Association/creation refs (C/E)** — no picker UIs exist; deferring them (§5.3)
  avoids shipping raw-ref-entry UX prematurely.

## 15. Deliverables

- This Requirement; then (after approval) Architecture / ADR / Implementation
  Design (as a §16.5-B bundle) and a Codex implementation prompt; then Codex
  implementation and independent review.

## 16. Review Checklist

- [ ] §5.3 product/authorization decisions confirmed by the Product Owner.
- [ ] V1 operation scope (A+B+D vs. including C/E) fixed.
- [ ] Console-operator capability set (and any role split) fixed.
- [ ] Thin-client / no-backend-business-change confirmed.
- [ ] Product Owner Gate Decision recorded (§17).

## 17. Current Gate

Q-017 Requirement status: **APPROVED — V1 — 2026-09-03 — Product Owner.**
Gate Decision: **PASS.** §5.3 CONFIRMED: V1 scope = Groups **A + B + D**
(C/E deferred); **single console-operator role** granted the full V1 lifecycle
capability set; **terminal actions (close/cancel/resolve) require confirmation +
reason**; **action availability = approach (c)** (show state-valid actions, rely
on backend `403`/`ResultCode`; no backend change for availability).

Q-017 Architecture V1 / ADR-019 / Implementation Design V1: **ACCEPTED (bundle) —
2026-09-03 — Product Owner** at the implementation-authorization gate (§16.5-B).
ADR-019 is **Accepted**. **Implementation AUTHORIZED — 2026-09-03 — Product
Owner.** The Codex implementation prompt
(`prompts/Q-017-Implementation-Prompt.md`) is **CLEARED FOR USE**.

Confirmed build shape: a declarative action registry + shared `useCaseAction`
runner over the Q-016 axios client; status-only action availability (approach c);
terminal actions (resolve/close/cancel) require confirmation + reason; the
console-operator role granted `{read, note, assign, review, resolve, close,
cancel, reopen}` in the security bootstrap. Thin-client; **no backend
business-logic/aggregate/migration change** — the only backend-adjacent change is
that capability grant.

Q-017 implementation (Codex, v1
`review/q-017/review-q-017-v1-implementation-20260903-020721`): the 11 A+B+D
operations via a declarative action registry + one TanStack Query runner
(reusing the Q-016 axios client), status-only availability, terminal
confirmation, version-conflict reload; the operator granted the V1 capability set
in the security bootstrap. No backend code/migration change.

Claude Code independent review: **PASS — zero defects — 2026-09-03** — see
`review/q-017/review-q-017-v2-claude-code-independent-review-20260903-122426/`.
Reproduced from a fresh `npm ci`: strict typecheck 0 errors; **Vitest 103/103**;
`vite build` PASS; backend untouched; bootstrap grant = the exact V1 set. **Live
lifecycle verified** against a real Keycloak→backend→MySQL stack: login (PKCE) →
assign → begin-review → change-priority → cancel (terminal), with version
increments + audit rows confirmed in the DB, and `mark-action-required` correctly
rejected by the backend (422) and surfaced as a readable typed error
(approach-c working). resolve/close/resume/reopen require a case with a current
decision + associated action (backend invariant; Group C deferred) — their
frontend is unit-covered.

Q-017 (V1) acceptance: **ACCEPTED — 2026-09-03 — Product Owner**; committed with
the implementation + v2 review package.

Q-017 status: **COMPLETE — 2026-09-03** (Risk Console case lifecycle operations,
Groups A+B+D; live lifecycle verified). Next: Groups C (associations) and E (case
creation) remain deferred to a future Requirement; Q-015 remains parked awaiting
the MT4/MT5 SDK.
