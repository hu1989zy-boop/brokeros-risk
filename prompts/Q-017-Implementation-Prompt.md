# Q-017 Case Lifecycle Operations — Implementation Prompt

**CLEARED FOR USE — Product Owner authorized implementation 2026-09-03.** Q-017
Requirement V1 is APPROVED, and — as one §16.5-B bundle at the
implementation-authorization gate — the Architecture V1, ADR-019 (Accepted), and
Implementation Design V1 are accepted with implementation explicitly authorized.
Recorded in `docs/requirements/Q-017-Risk-Console-Case-Lifecycle-Operations.md`
§17 and each governing document's gate section. Governed by the two
`docs/engineering/` documents — read them first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-017-Risk-Console-Case-Lifecycle-Operations.md` (V1,
   APPROVED; §5.3 decisions CONFIRMED; §17).
2. `docs/adr/ADR-019-risk-console-case-lifecycle-operations.md` (Accepted).
3. `docs/architecture/q-017-case-lifecycle-operations-architecture.md` (V1).
4. `docs/architecture/q-017-case-lifecycle-operations-implementation-design.md`
   (V1) — the authoritative build spec (operation table in §4).

Also read for context and **reuse unchanged**: the Q-016 React console
(`frontend/src/**`, especially `core/api/apiClient.ts`, `core/auth/*`,
`features/riskcase/**`) and ADR-018; and the **already-committed**
`RiskCaseController` + its request DTOs + `RiskCaseCapabilities` (consume; do not
modify).

## The confirmed shape — summary; the four documents are authoritative

- A thin-client expansion of the Q-016 React SPA (ADR-018 stack; Ant Design;
  TanStack Query). **V1 scope = Groups A + B + D only** (Design §4). Groups C
  (associations) and E (case creation) are **out of scope** — do not build them.
- A **declarative action registry** (one descriptor per operation) + a shared
  **`useCaseAction`** runner (TanStack Query `useMutation`) that reuses the Q-016
  `ApiClient` (Bearer, one `401` refresh, `403`→typed error, `ApiResponse`/
  `ResultCode` parsing) and **always sends `expectedVersion`**.
- The 11 operations, exact endpoints/bodies in Design §4: assign, changePriority,
  beginReview, markActionRequired, returnToReview, resolve, close, cancel,
  resume, reopen, correctNote. All `POST` under `/api/risk-cases`.
- **Action availability = approach (c)** (Design §4 `allowedFrom`): show the
  actions valid for the case's current status; the backend `403`/`ResultCode` is
  the authority and must be surfaced as a typed, readable error — never swallowed.
- **Terminal actions (resolve, close, cancel)** require an explicit confirmation
  step + mandatory reason (Design §5).
- On `RISK_CASE_VERSION_CONFLICT`: reload the case, keep the operator's unsaved
  input, prompt a retry (Design §3) — no data loss.
- Wire the actions into `RiskCaseDetailPage` (actions bar + generic dialog +
  a notes panel with a per-note "Correct" action) — Design §6.

## Task

Implement Q-017 V1 exactly as specified in Implementation Design V1, and only
that. Add the typed request/response models (Design §7), the action registry +
runner, the UI (actions bar, action dialog with terminal confirmation, notes
panel), and the detail-page wiring. Update the security bootstrap so the
console-operator role holds the V1 capability set (Design §8). Include the tests
in Design §9 (Vitest/RTL/MSW per operation incl. `403` and version-conflict
paths; the live Playwright lifecycle slice extending the Q-016 harness).

## Hard boundaries — do not do these

- **No backend code change.** Do not modify `RiskCaseController`, any Risk Case
  service/aggregate, Q-009, or any Java under `backend/src/main/java`. Add **no**
  Flyway migration. The ONLY backend-adjacent change permitted is the
  console-operator **capability grant** in the security bootstrap under `deploy/`
  (Design §8) — authorization config, not code.
- Do not build Group C (associations: evidence/decision/action, dispositions,
  selection, outcomes) or Group E (case creation). They are deferred.
- Do not move any authorization, invariant, or transition decision into the
  client. Availability is status-only (approach c); the backend decides, and its
  `403`/`ResultCode` must be shown. Do not parse JWT claims or add a capability
  probe in V1.
- Do not send actor identity in any request body; identity is the Bearer JWT only.
- Do not weaken or bypass the terminal-action confirmation + mandatory reason.
- Do not put tokens, reasons, or case content in logs, fixtures, or artifacts.
- Do not stage, commit, or push. Do not touch any existing timestamped review
  package.
- If you find a contradiction, resolve toward the approved documents (Requirement
  > ADR-019 > Architecture > Design) and record the assumption in
  `OutstandingItems.md`.

## Environment honesty (important)

The React toolchain is Node + Vite. Run `npm ci`, `npm run typecheck`,
`npm test` (Vitest — no browser needed), and `npm run build`, and report real
pass/fail counts. The live Playwright lifecycle slice needs the full local stack
(`docker compose --profile console` + a seeded case + the operator granted the V1
capabilities + `vite preview`). If you cannot stand that up in your environment,
say so explicitly in `Verification.md` and deliver the Playwright spec as code
marked "not executed — stack unavailable"; **never claim a check passed that did
not run.** (The Node unit/component layer must be genuinely executed.)

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-017/review-q-017-v<N>-implementation-<YYYYMMDD-HHMMSS>/` containing at
least: `Summary.md`, `ArchitectureReview.md` (against ADR-019 + Design),
`DesignTraceability.md` (map each Q017-FR-xx + each operation to code/test),
`ProjectTree.txt`, `GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact
commands, tool availability, pass/fail/skip — honest; state what was/wasn't run),
`SecurityReview.md`, `TestInventory.txt`, `OutstandingItems.md`. Add a
`docs/lessons/<date>-q-017-implementation.md` entry.

Confirm the backend is untouched: `git diff --stat -- backend/` is empty and no
migration is added (the only non-frontend change is the bootstrap JSON grant
under `deploy/`).

This package is for Claude Code's independent implementation review — not your own
sign-off. State PASS/FAIL against each acceptance criterion honestly; list every
open question and assumption. Stop after producing the review package; do not
begin any other Requirement.
