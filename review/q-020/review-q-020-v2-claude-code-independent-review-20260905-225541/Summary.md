# Q-020 — Claude Code Independent Implementation Review (v2)

- Requirement: Q-020 — External-Reference Scoped Search / Browse (Option B), V1
- Reviewed: Codex v1 delivery (`review-q-020-v1-implementation-20260905-223756`)
- Baseline: `6c7b8bc` (Q-020 Requirement + §16.5-B bundle)
- Reviewer: Claude Code (external Architect role) — Date: 2026-09-05
- **Gate Decision: PASS**

## Verdict

The deferred Option B, delivered cleanly. Codex added exactly the authorized additive
reads — four scoped-list endpoints on the provenance modules (evidence/decision by
`subjectRef`, action by `decisionRef`, outcome by `actionRef`) reusing the existing
per-module `*:read` capabilities — plus a case-scoped console browse/pick mode with
manual entry retained. I independently reproduced both gates and code-reviewed the
additive-read boundary, the content-free posture, and the least-authority console
scoping. Codex's own gate was honest (PASS with no self-acceptance; it flagged its one
design interpretation for this review).

## Independently reproduced

| Check | Result |
| --- | --- |
| Backend full real-MySQL gate (Docker MySQL 8.4 + Maven 21, full repo mounted) | **BUILD SUCCESS — 317 tests, 0 failures, 0 errors, 0 skipped** |
| The four `Q020…ReferenceListMySqlTests` (evidence/decision/action/outcome) | **4/4** — bounded (seed 202 → exactly 200), most-recent-first ordering, unknown key → empty, malformed key → module request-invalid (400), denied → 403, **content field absent**, **no access-log row**, scope-key index present |
| Frontend `npm ci` → `tsc --noEmit` → `vitest` → `vite build` | **0 type errors; 156/156; build PASS** |
| Migration boundary | **No migration** — Flyway applied only V1–V8; the four scope-key indexes already exist (V4–V7) |
| Additive-read-only boundary | **confirmed** — risk-case module untouched; no Recording/Correction/Resolution/Command/Association service or domain change; no new capability; no table/column |

## Code review (correct)

- **Endpoints:** each provenance controller gains `@GetMapping(params = "<scopeKey>")`
  (discriminated from `GET /{ref}` — no collision), delegating to a new
  `Xxx ReferenceListService` that `requireAllowed(M.READ)` first, parses the scope key
  via the module's value object (invalid → the module's request-invalid `ResultCode`),
  and returns `queryPort.findSummariesBy…(key, REFERENCE_LIST_MAX=200)`.
- **Queries (content-free):** the four new `SELECT`s read only
  `{ref, scope-key ref, status?, recorded_at}` `ORDER BY recorded_at DESC, id DESC
  LIMIT ?` — never `observation_text` / `conclusion_text` / `intent_text` /
  `outcome_text`. Decision/outcome omit `status` (no such column); evidence/action
  include it.
- **No content-access audit:** the list path does not inject or call the access-log
  port (unlike `GET /{ref}`); the evidence MySQL test asserts `evidence_access_log`
  stays empty.
- **Capabilities:** reuse `evidence:read` / `decision:read` / `action:read` /
  `action-outcome:read`; **no new capability**, no bootstrap/security change.
- **Console:** `ReferenceListRepository` (+ provider, reusing the authenticated
  `ApiClient`) + four conditional TanStack Query hooks; `ReferenceInput` gains a
  Browse|Manual `Segmented` toggle (Browse default when a scope key exists, manual
  fallback always available); picking a candidate flows through the existing
  fetch-by-ref preview and the **unchanged** Q-018 association request. Least-authority:
  `referenceBrowseScope(subjectRef, associations)` sources the scope strictly from the
  case's own subject + its current/first on-case decision + first on-case action (the
  Q-019 projection) — no global browse.

## Acceptance criteria — reviewer view

| AC | Result |
| --- | --- |
| 1 bounded scoped lists, unknown-key empty, `*:read`, malformed request-invalid | **PASS** — reproduced real-MySQL |
| 2 additive read only; no aggregate/table-column change; no migration (index-only only if shown) | **PASS** — no migration added; boundary clean |
| 3 content-free (no recorded text; no label) | **PASS** — SQL + DTO contract test + `observationText.doesNotExist()` |
| 4 console case-scoped browse/pick; picked ref drives the unchanged association op; manual fallback | **PASS** — code + tests |
| 5 backend + frontend tests + typecheck/build | **PASS** — 317/317 + 156/156 + build |

## The one interpretation I confirm

Codex validates a malformed scope key by constructing the module value object in the
list service (authorize-first, then parse → the module's request-invalid `ResultCode`)
rather than a controller `@Pattern`. This satisfies the Design's stated contract
("malformed key → the module's request-invalid `ResultCode`; reuse value objects") and
is proven by the real-MySQL HTTP test (`400 EVIDENCE_REQUEST_INVALID`). Correct.

## Minor notes (not defects)

- **Outcome browse scope:** where a case has multiple on-case actions, V1 scopes the
  outcome browse to the first action in the projection; manual entry covers the rest.
  Within the approved Design (which specifies "an on-case action", no chooser); a
  future action-chooser is a possible enhancement, not required for V1.
- Benign advisories (all observed, none affecting results): Flyway's 8.1-tested-ceiling
  warning on MySQL 8.4 (same as Q-019); one Vite chunk > 500 kB (no new major
  dependency); Mockito/Node experimental-runtime warnings.

## Recommendation

**Accept Q-020 V1.** Four additive, content-free, bounded scoped-list endpoints reusing
existing `*:read` (no new capability, no aggregate/write/migration change) plus a
case-scoped console browse/pick with manual fallback and unchanged association
contracts — independently verified (317 backend + 4/4 Q020 + 156 frontend + build).
The reviewer changed no code. Option B (the last deferred console gap) is closed.
