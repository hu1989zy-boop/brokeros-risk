# Q-020 External-Reference Scoped Search / Browse — Implementation Prompt

**CLEARED FOR USE — Product Owner approved Q-020 V1 and authorized the §16.5-B bundle
2026-09-05.** As one §16.5-B bundle at the implementation-authorization gate, the
Architecture V1, ADR-022 (Accepted), and Implementation Design V1 are accepted with
implementation authorized. Recorded in
`docs/requirements/Q-020-Risk-Console-External-Reference-Search.md` §17 and each
bundle document's status. Governed by the two `docs/engineering/` documents — read
them first.

Read in this exact order, each authoritative over anything below it:

1. `docs/requirements/Q-020-Risk-Console-External-Reference-Search.md` (V1, APPROVED;
   §5.3 all CONFIRMED; §17).
2. `docs/adr/ADR-022-external-reference-scoped-search.md` (Accepted).
3. `docs/architecture/q-020-external-reference-search-architecture.md` (V1).
4. `docs/architecture/q-020-external-reference-search-implementation-design.md` (V1)
   — the authoritative build spec (shared shape §1; per-module backend §2; console
   §3; tests §4; traceability §5).

Also read for context and **reuse the patterns of**: `EvidenceDetailReadService` /
`EvidenceQueryPort` / `JdbcEvidenceQueryAdapter` / `EvidenceController` `GET /{ref}`
(and the Decision/Action/ActionOutcome equivalents) — the scoped list mirrors the
detail read; the Q-018 `ReferenceInput` + `useReferencePreview` and the Q-019
associations projection hook on the console.

## The confirmed shape — summary; the four documents are authoritative

- **Backend (additive read only), per module:** add one scoped-list endpoint —
  `GET /api/evidence?subjectRef=ta-…`, `GET /api/decisions?subjectRef=ta-…`,
  `GET /api/actions?decisionRef=dec-…`, `GET /api/action-outcomes?actionRef=act-…` —
  each `requireAllowed(M.READ)` (existing capability, **no new capability**), reading
  `SELECT <ref, scope-key ref, status?, recorded_at> FROM M_record WHERE <scope_col>=?
  ORDER BY recorded_at DESC, id DESC LIMIT REFERENCE_LIST_MAX`, returning a bounded
  `ApiResponse` list of `{reference, scope-key ref, recordedAt, status?}`. **Content
  is never selected or returned** (`observation_text` / `conclusion_text` /
  `intent_text` / `outcome_text`). Valid-but-unknown key → empty list (200); malformed
  key → the module's request-invalid `ResultCode`. **No** full-detail access-log entry
  on the list path.
- **No migration.** The scope-key indexes already exist (`idx_evidence_record_subject`,
  `idx_decision_record_subject`, `idx_action_record_decision`,
  `idx_action_outcome_record_action`). Add a Flyway migration **only** if you can show
  a measured need, and if so it must be **index-only** (no table/column/data change)
  and explicitly called out in `Verification.md`. V1 is expected to add none.
- **Frontend:** a `ReferenceListRepository` (`listEvidence/listDecisions/listActions/
  listOutcomes`) + a TanStack Query hook per kind, and a **browse/pick mode** in
  `ReferenceInput` (searchable Select / "Browse" control) fed a `browseScope`
  (subjectRef for evidence/decision; on-case decisionRef/actionRef for action/outcome)
  by `CaseActionDialog` from the case detail + Q-019 projection — **restricted to the
  case in hand**. **Manual entry retained as a fallback.** The association action
  registry and request bodies are **unchanged**.

## Task

Implement Q-020 V1 exactly as specified in the Implementation Design, and only that:
the four scoped-list endpoints + query-service methods + port methods + read-only
`SELECT`s + list DTOs + a real-MySQL test per module; and the console
`ReferenceListRepository` + hooks + `ReferenceInput` browse/pick mode + `CaseActionDialog`
scope wiring + tests. Change only *how a reference is chosen*, never the association
contract.

## Hard boundaries — do not do these

- **Additive READ only.** No aggregate/domain/write change on any module; no change to
  `record`/`correct`/association writes; **no new capability** (reuse the four
  `*:read`); **no new table/column**; **no migration** (index-only only if measured +
  called out).
- **Content-free.** Never select or return recorded text; no truncated label in V1.
- Do **not** add pagination, free-text/content search, or a cross-module endpoint.
- Do **not** write a full-detail access-log entry on the list path.
- Thin client; no identity in request bodies beyond the Bearer JWT; reuse the existing
  `ApiClient`; no refs/content in logs or fixtures.
- Do not stage, commit, or push. Do not modify any existing timestamped review package.
- On any contradiction, resolve toward the approved documents (Requirement > ADR-022 >
  Architecture > Design) and record the assumption in `OutstandingItems.md`.

## Environment honesty (important)

Run the backend real-MySQL gate for each new endpoint (the project's disposable MySQL
pattern) and the full repository gate — report real pass/fail/skip counts, and confirm
**no aggregate diff and no migration** (index-only only if you added one, with the
reason). Run the frontend `npm ci` + `npm run typecheck` + `npm test` (Vitest — no
browser) + `npm run build`. Never claim a check passed that did not run. Mount the
**full repo** for the backend Docker run (ArchUnit/config tests need it).

## Required output

Create ONE new, non-overwriting, timestamped review package at
`review/q-020/review-q-020-v<N>-implementation-<YYYYMMDD-HHMMSS>/` with at least:
`Summary.md`, `ArchitectureReview.md` (against ADR-022 + Design),
`DesignTraceability.md` (map each Q020-FR-xx to code/test), `ProjectTree.txt`,
`GitStatus.txt`, `GitDiffStat.txt`, `Verification.md` (exact commands, tool
availability, pass/fail/skip — honest; backend real-MySQL results per module +
frontend results; **confirm no migration / no aggregate diff / no new capability**),
`SecurityReview.md` (content-free + `*:read` reuse + least-authority console scope),
`TestInventory.txt`, `OutstandingItems.md`. Add
`docs/lessons/<date>-q-020-implementation.md`.

Confirm the read-only boundary: `git diff` shows **no** aggregate/domain/write change,
**no** new capability, **no** migration (or an index-only one, called out), and the
backend delta is four additive scoped-list endpoints/queries/DTOs + tests.

This package is for Claude Code's independent implementation review — not your own
sign-off. State PASS/FAIL against each acceptance criterion honestly; list every open
question and assumption. Stop after producing the review package; do not begin any
other Requirement.
