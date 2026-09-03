# Q-018 Association Management — Implementation Prompt

**CLEARED FOR USE — Product Owner authorized implementation 2026-09-03.** Q-018
Requirement V1 is APPROVED, and — as one §16.5-B bundle at the
implementation-authorization gate — the Architecture V1, ADR-020 (Accepted), and
Implementation Design V1 are accepted with implementation explicitly authorized.
Recorded in `docs/requirements/Q-018-Risk-Console-Association-Management.md` §17
and each governing document's gate section. Governed by the two
`docs/engineering/` documents — read them first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-018-Risk-Console-Association-Management.md` (V1, APPROVED;
   §5.3 CONFIRMED; §17).
2. `docs/adr/ADR-020-risk-console-association-management.md` (Accepted).
3. `docs/architecture/q-018-association-management-architecture.md` (V1).
4. `docs/architecture/q-018-association-management-implementation-design.md` (V1)
   — the authoritative build spec (operation table §3; preview §4; pickers §5).

Also read for context and **reuse unchanged**: the Q-017 action framework
(`frontend/src/features/riskcase/actions/**`, `ui/CaseActionDialog.tsx`,
`ui/CaseActionsBar.tsx`), the Q-016 `core/api/apiClient.ts` + auth, and the
**already-committed** association endpoints on `RiskCaseController` plus the
`GET /{ref}` reads on the Evidence/Decision/Action/ActionOutcome controllers
(consume; do not modify).

## The confirmed shape — summary; the four documents are authoritative

- A thin-client expansion of the Q-016/Q-017 React console (ADR-018/019 stack; Ant
  Design; TanStack Query). **V1 = the six Group C operations** (Design §3):
  associate evidence, change evidence disposition, associate decision, select
  current decision, associate action, reference action outcome.
- Reuse the **Q-017 action registry + `useCaseAction` runner** (Bearer,
  `expectedVersion`, `403`/`ResultCode` typed errors, `RISK_CASE_VERSION_CONFLICT`
  reload). Do not build new execution machinery.
- **External references** (new evidence/decision/action refs, `outcomeRef`,
  replacement evidence) use a **`ReferenceInput`** that client-validates the
  `ev-/dc-/ac-/ao-` UUID format, then previews the entity via the **existing**
  `GET /api/{evidence|decisions|actions|action-outcomes}/{ref}` (Option A). The
  action cannot submit an external ref until a valid preview is confirmed. Show
  only identifying preview fields — do not dump full entity content.
- **On-case references** (disposition target event; current-decision candidates;
  the action for an outcome) are picked from the case's **existing detail/history**
  — no manual UUID. **Verify** the detail/history payloads expose these refs; if a
  specific ref is absent, fall back to a `ReferenceInput` for it (still no new
  endpoint) and record it in `OutstandingItems.md`.
- Render current associations in an `AssociationsPanel` (effective evidence;
  associated + current decision; associated actions + outcomes); refresh after
  each operation.

## Task

Implement Q-018 V1 exactly as specified in Implementation Design V1, and only
that: the six association operations + `ReferenceInput`/preview + on-case pickers +
`AssociationsPanel`, wired into the detail page; the typed request/preview models;
and the security-bootstrap capability grant (Design §7). Include the tests in
Design §9 (Vitest/RTL/MSW per operation + preview states; the live Playwright
association slice that leaves the case resolve-reachable).

## Hard boundaries — do not do these

- **No backend code change.** Do not modify any Java under `backend/src/main/java`
  (RiskCaseController, the association services, the module controllers, Q-009) and
  add **no** Flyway migration. **Do not add any new endpoint** — Option A reuses
  the existing `GET /{ref}`; Option B (new list/search endpoints) is out of scope.
  The ONLY backend-adjacent change is the console-operator **capability grant** in
  the security bootstrap under `deploy/` (Design §7): add
  `risk-case:associate`, `evidence:read`, `decision:read`, `action:read`,
  `action-outcome:read` to the existing set.
- Do not create evidence/decisions/actions from the console (their `POST`); do not
  build Group E (case creation).
- Do not move authorization/association rules into the client. The backend decides;
  surface its `403`/`ResultCode` as readable typed errors. Do not parse JWT claims
  or add a capability probe.
- Do not send actor identity in any request body; identity is the Bearer JWT only.
- Do not log or place in fixtures any token, reference, reason, or entity content.
- Do not stage, commit, or push. Do not modify any existing timestamped review
  package.
- On any contradiction, resolve toward the approved documents (Requirement >
  ADR-020 > Architecture > Design) and record the assumption in
  `OutstandingItems.md`.

## Environment honesty (important)

Run `npm ci`, `npm run typecheck`, `npm test` (Vitest — no browser), and
`npm run build`; report real counts. The live Playwright association slice needs
the full local stack (`docker compose --profile console` + the operator granted
the Q-018 capabilities + a seeded case + real `ev-/dc-/ac-` entities to associate).
If you cannot stand that up (or cannot seed real referenceable entities), say so
explicitly in `Verification.md` and deliver the spec marked "not executed —
inputs/stack unavailable"; **never claim a check passed that did not run.** The
Node unit/component layer must be genuinely executed.

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-018/review-q-018-v<N>-implementation-<YYYYMMDD-HHMMSS>/` with at least:
`Summary.md`, `ArchitectureReview.md` (against ADR-020 + Design),
`DesignTraceability.md` (map each Q018-FR-xx + each operation to code/test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact
commands, tool availability, pass/fail/skip — honest), `SecurityReview.md`,
`TestInventory.txt`, `OutstandingItems.md` (incl. the on-case-ref availability
finding). Add `docs/lessons/<date>-q-018-implementation.md`.

Confirm the backend is untouched: `git diff --stat -- backend/` empty and no
migration; the only non-frontend change is the bootstrap JSON grant under
`deploy/`.

This package is for Claude Code's independent implementation review — not your own
sign-off. State PASS/FAIL against each acceptance criterion honestly; list every
open question and assumption. Stop after producing the review package; do not begin
any other Requirement.
