# Q-019 Association Projection — Implementation Prompt

**CLEARED FOR USE — Product Owner authorized implementation 2026-09-03.** Q-019
Requirement V1 is APPROVED, and — as one §16.5-B bundle at the
implementation-authorization gate — the Architecture V1, ADR-021 (Accepted), and
Implementation Design V1 are accepted with implementation explicitly authorized.
Recorded in `docs/requirements/Q-019-Risk-Case-Association-Projection-Endpoint.md`
§17 and each governing document's gate section. Governed by the two
`docs/engineering/` documents — read them first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-019-Risk-Case-Association-Projection-Endpoint.md` (V1,
   APPROVED; §5.3 CONFIRMED; §17).
2. `docs/adr/ADR-021-risk-case-association-projection.md` (Accepted).
3. `docs/architecture/q-019-association-projection-architecture.md` (V1).
4. `docs/architecture/q-019-association-projection-implementation-design.md` (V1)
   — the authoritative build spec (endpoint §2.1; assembly §2.2; DTO §2.3;
   console §3; tests §4).

Also read for context and **reuse the patterns of**: `RiskCaseController.detail`/
`history`/`list` and `RiskCaseQueryService` (the additive `listCases` from Q-016 is
the closest precedent), `JdbcRiskCaseRepository` (`findAllEffectiveEvidence`,
`findAllEffectiveActions`, evidence event rows, `risk_case_decision_association`),
and the Q-018 console (`AssociationsPanel`, the association action framework).

## The confirmed shape — summary; the four documents are authoritative

- **Backend (additive read only):** add
  `GET /api/risk-cases/{caseNumber}/associations` on the existing
  `RiskCaseController`, delegating to a new `RiskCaseQueryService.associations`
  guarded by **`RiskCaseCapabilities.READ`** (mirror `detail`/`history`). Return a
  bounded `RiskCaseAssociationsResponse` (Design §2.3): evidence associations
  (`eventRef`, `evidenceRef`, disposition/`eventType`, `source`,
  `replacementEvidenceRef?`, `occurredAt`), decisions (`decisionRef`, `current`),
  actions (`actionRef`, `outcomeRefs[]`). Refs/enums/timestamps only — no external
  entity content. Missing case → the standard not-found `ResultCode`. Bounded with
  a sane server cap; no pagination.
- Assemble from `findAllEffectiveEvidence` / `findAllEffectiveActions` plus **two
  additive read-only queries** — list the evidence association events (with their
  `eventRef`) and list the decision associations (over
  `risk_case_decision_association`) — mark the aggregate's `currentDecisionRef` as
  current. Plain `SELECT`s over existing tables.
- **Frontend:** add `getAssociations(caseNumber)` + a TanStack Query hook; switch
  `AssociationsPanel` to the projection as the **authoritative** source (drop the
  "reconstructed from history" caveat); make the **evidence-disposition** target an
  on-case picker over the projection's evidence associations (option value =
  `eventRef`), replacing the Q-018 B1 manual fallback. Invalidate/refetch after any
  association write.

## Task

Implement Q-019 V1 exactly as specified, and only that: the read endpoint + query
service method + two read-only repository queries + the DTO + a real-MySQL
projection test; and the console switch (projection hook, authoritative panel,
disposition on-case picker) + its tests. Extend the live Playwright slice so that,
after associating a decision → selecting it → associating an action (all via the
console) and reading the projection, the Q-017 `resolve` (and `close`) path is
driven end to end — the first live `resolve` verification.

## Hard boundaries — do not do these

- **Additive READ only.** Do **not** change any Risk Case aggregate, domain rule,
  or write path; do **not** add a Flyway migration or any new table/column; do
  **not** add a new capability (authorize with the existing `risk-case:read`). The
  backend addition is: one controller GET method, one query-service method, two
  read-only repository `SELECT` queries, and one response DTO — plus tests.
- Do **not** touch other modules (Evidence/Decision/Action/ActionOutcome) or their
  endpoints. Do **not** implement Option B (external-ref browse/search) or any
  write operation.
- Keep the projection a pure read consistent with the aggregate's effective-
  association semantics; do not re-derive or re-decide association rules client- or
  server-side beyond reading.
- Thin client on the frontend; no identity in any request body; reuse the existing
  `ApiClient`. No tokens/refs/entity content in logs or fixtures.
- Do not stage, commit, or push. Do not modify any existing timestamped review
  package.
- On any contradiction, resolve toward the approved documents (Requirement >
  ADR-021 > Architecture > Design) and record the assumption in `OutstandingItems.md`.

## Environment honesty (important)

Run the backend real-MySQL gate for the new endpoint (the project's disposable
MySQL pattern) and the full repository gate — report real pass/fail/skip counts,
and confirm no aggregate/migration diff. Run the frontend `npm ci` +
`npm run typecheck` + `npm test` (Vitest — no browser) + `npm run build`. The live
`resolve` Playwright slice needs the full local stack + a seeded decision/action;
if you cannot stand it up or seed real referenceable entities, say so explicitly in
`Verification.md` and deliver the spec marked "not executed — stack/inputs
unavailable"; **never claim a check passed that did not run.** The backend
real-MySQL endpoint test and the Node unit/component layer must be genuinely
executed.

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-019/review-q-019-v<N>-implementation-<YYYYMMDD-HHMMSS>/` with at least:
`Summary.md`, `ArchitectureReview.md` (against ADR-021 + Design),
`DesignTraceability.md` (map each Q019-FR-xx to code/test), `ProjectTree.txt`,
`GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact commands, tool
availability, pass/fail/skip — honest; backend real-MySQL result + frontend
results + live-slice status), `SecurityReview.md`, `TestInventory.txt`,
`OutstandingItems.md`. Add `docs/lessons/<date>-q-019-implementation.md`.

Confirm the read-only boundary: `git diff` shows **no** change to Risk Case
aggregate/domain write code, **no** Flyway migration, and **no** new capability;
the backend delta is the additive read endpoint/query/DTO/tests.

This package is for Claude Code's independent implementation review — not your own
sign-off. State PASS/FAIL against each acceptance criterion honestly; list every
open question and assumption. Stop after producing the review package; do not begin
any other Requirement.
