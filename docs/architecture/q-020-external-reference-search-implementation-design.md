# Q-020 Implementation Design — External-Reference Scoped Search / Browse (Option B)

Status: **V1**, part of the §16.5-B bundle. The authoritative build spec; subordinate
to the Requirement, ADR-022, and the Architecture. Additive read only.

## 0. Ground rules (from ADR-022 / Architecture)

- Additive **read** only: per module, one query-service method + one port method +
  one JDBC `SELECT` + one controller `GET` + one list DTO, plus tests. **No**
  aggregate/domain/write change, **no** new capability, **no** migration (the
  scope-key indexes already exist; an index-only migration only if a measured need is
  shown and called out).
- **Content-free:** never select or return `observation_text` / `conclusion_text` /
  `intent_text` / `outcome_text`. No full-detail access-log entry on the list path.
- Reuse each module's existing detail-read pattern (`Xxx DetailReadService`,
  `XxxQueryPort`, `Jdbc XxxQueryAdapter`, `XxxController`).

## 1. Shared shape

Each module M ∈ {Evidence, Decision, Action, ActionOutcome} gets a **scoped list**:

- **Scope key** (query parameter, required, validated to the module's ref/subject
  regex): Evidence & Decision → `subjectRef` (`ta-…`); Action → `decisionRef`
  (`dec-…`); ActionOutcome → `actionRef` (`act-…`).
- **Authorization:** `authorizationGuard.requireAllowed(actorContext, M.READ)` first
  (mirror the detail service; on denial record `recordAuthorizationDenied(M.READ)` and
  rethrow) — **no new capability**.
- **Query:** read-only `SELECT` over `M_record` `WHERE <scope_col> = ?`
  `ORDER BY recorded_at DESC, id DESC` `LIMIT <cap>`.
- **Bounding:** a single shared server cap constant `REFERENCE_LIST_MAX` (propose
  **200**). The result is silently capped at the cap (§5.3(3): no pagination, no
  “more” flag in V1).
- **Semantics:** malformed key → the module's existing request-invalid `ResultCode`
  (as the detail service throws for a bad ref); **valid-but-unknown key → an empty
  list** (HTTP 200, `items: []`), not a not-found error.
- **No access log:** unlike `GET /{ref}`, the list does **not** call
  `accessLogPort.recordFullDetailAccess`. A light metric (e.g. a browse counter) is
  optional; do not add a content-access audit row.

## 2. Backend, per module

### 2.1 Evidence — `GET /api/evidence?subjectRef={ta-…}`

- **Controller** (`EvidenceController`): add
  `@GetMapping(params = "subjectRef")` `list(@RequestParam @Pattern(ta-regex) String
  subjectRef)` → `EvidenceReferenceListResponse.from(listService.listBySubject(ctx,
  subjectRef))`. (Keep `GET /{evidenceRef}` unchanged; the `params=subjectRef`
  discriminator avoids collision.)
- **Service:** a `listBySubject(ActorContext, String subjectRef)` — new method on a
  small read service (mirror `EvidenceDetailReadService`; may be the same class or a
  new `EvidenceReferenceListService`). Steps: `requireAllowed(READ)` → parse
  `new TradingAccountRef(subjectRef)` (invalid → `EVIDENCE_REQUEST_INVALID`) →
  `queryPort.findSummariesBySubject(ref, REFERENCE_LIST_MAX)` → return the list.
- **Port** (`EvidenceQueryPort`): add
  `List<EvidenceReferenceSummary> findSummariesBySubject(TradingAccountRef subject,
  int limit)`.
- **Adapter** (`JdbcEvidenceQueryAdapter`):
  `SELECT evidence_ref, subject_ref, status, recorded_at FROM evidence_record
   WHERE subject_ref = ? ORDER BY recorded_at DESC, id DESC LIMIT ?`.
- **DTO item:** `{ evidenceRef, subjectRef, status /* ACTIVE|SUPERSEDED */,
  recordedAt }`. **Response:** `EvidenceReferenceListResponse { items: [...] }`.

### 2.2 Decision — `GET /api/decisions?subjectRef={ta-…}`

- Controller `@GetMapping(params = "subjectRef")` mirroring 2.1.
- Query: `SELECT decision_ref, subject_ref, recorded_at FROM decision_record
  WHERE subject_ref = ? ORDER BY recorded_at DESC, id DESC LIMIT ?`.
- Item: `{ decisionRef, subjectRef, recordedAt }` (decision_record has **no status**
  column — omit it).

### 2.3 Action — `GET /api/actions?decisionRef={dec-…}`

- Controller `@GetMapping(params = "decisionRef")`, `@Pattern(dec-regex)`.
- Parse `new DecisionRef(decisionRef)` (invalid → the action module's request-invalid
  `ResultCode`).
- Query: `SELECT action_ref, decision_ref, status, recorded_at FROM action_record
  WHERE decision_ref = ? ORDER BY recorded_at DESC, id DESC LIMIT ?`.
- Item: `{ actionRef, decisionRef, status /* PROPOSED */, recordedAt }`.

### 2.4 ActionOutcome — `GET /api/action-outcomes?actionRef={act-…}`

- Controller `@GetMapping(params = "actionRef")`, `@Pattern(act-regex)`.
- Query: `SELECT action_outcome_ref, action_ref, recorded_at FROM
  action_outcome_record WHERE action_ref = ? ORDER BY recorded_at DESC, id DESC
  LIMIT ?`. **Never select `outcome_text`.**
- Item: `{ actionOutcomeRef, actionRef, recordedAt }` (no status column).

### 2.5 Notes

- All four `record` tables and their scope-key indexes already exist (V4–V7); confirm
  with `SHOW INDEX` in the test if helpful. **Add no migration.**
- Validation regexes reuse the module value objects (`TradingAccountRef`,
  `DecisionRef`, `ActionRef`) — do not hand-roll.
- `recordedAt` serializes as the existing ISO-8601 instant used by the detail DTOs.

## 3. Console (frontend)

### 3.1 Data layer

- `frontend/src/features/riskcase/api/referenceList.ts`: a `ReferenceListRepository`
  with `listEvidence(subjectRef)`, `listDecisions(subjectRef)`, `listActions(decisionRef)`,
  `listOutcomes(actionRef)` → `GET` the endpoints in §2 via the existing `ApiClient`,
  returning typed `ReferenceListItem[]` (kind-specific: evidence/action carry `status`).
- A TanStack Query hook per kind (`useEvidenceList` etc.), **enabled only when the
  scope key is present**; bounded, cached, invalidated on association writes if shown.

### 3.2 `ReferenceInput` browse/pick mode

- Extend `ReferenceInput` (Q-018) with an optional `browseScope` prop:
  `{ subjectRef?: string; decisionRef?: string; actionRef?: string }`. When the scope
  key for the input's `kind` is present, render a **browse control** beside the manual
  field: a searchable Ant `Select` (or a "Browse" popover) listing candidates as
  `{reference · recordedAt · status?}`, most-recent first.
- Selecting a candidate calls the existing `onChange(reference)`; the existing
  fetch-by-ref **preview** (`useReferencePreview`) then confirms it unchanged, and the
  existing Q-018 association request fires unchanged.
- **Manual entry is always retained** as a fallback (e.g. a segmented "Browse |
  Enter manually" toggle, defaulting to Browse when a scope key is available, else
  manual).

### 3.3 Scope-key sourcing (`CaseActionDialog`)

Supply `browseScope` per reference kind from the case context, **restricted to the
case in hand** (§5.3(6)):

- **evidence / decision** association inputs → `subjectRef` = the case detail's
  `subjectRef`.
- **action** association input → `decisionRef` = an on-case decision (prefer the
  current decision; else any associated decision) from the Q-019 associations
  projection.
- **action-outcome** reference input → `actionRef` = an on-case action from the
  Q-019 projection.

No change to `actionDescriptors` / `useCaseAction` / the association request bodies —
Option B changes only how a reference value is chosen.

## 4. Tests

### 4.1 Backend (real-MySQL, per module)

For each module, a `Q020…MySqlTests` asserting:
- seed N records under a scope key → the endpoint returns exactly those N (refs +
  metadata; **content absent**), most-recent first;
- a **different / unknown** scope key → empty list (HTTP 200, `items: []`);
- the module's `*:read` capability is required (denied → `AUTHORIZATION_DENIED`);
- the cap bounds the result (seed > `REFERENCE_LIST_MAX` → exactly the cap returned);
- (optional) `SHOW INDEX` confirms the scope-key index exists → no migration needed.
The full repository gate stays green; assert no aggregate diff / no migration.

### 4.2 Frontend (Vitest + RTL + MSW)

- `ReferenceInput` browse mode: with a `browseScope`, it lists mocked candidates,
  selecting one sets the value and shows the existing preview; with no scope key it
  falls back to manual entry; manual entry still works and validates prefixes.
- `ReferenceListRepository` + hooks: correct URL/params, typed items, empty result.
- `CaseActionDialog`: passes the right scope key per kind from the case + projection.

## 5. Traceability

| FR | Code |
| --- | --- |
| Q020-FR-01 | §2.1–2.4 endpoints + queries |
| Q020-FR-02 | `requireAllowed(M.READ)`; unknown key → empty (§1) |
| Q020-FR-03 | additive read; no migration (§0, §2.5) |
| Q020-FR-04 | content-free DTOs (§2); cap (§1) |
| Q020-FR-05 | `ReferenceInput` browse + manual fallback (§3.2) |
| Q020-FR-06 | unchanged association requests/registry (§3.3) |

## 6. Out of scope (reaffirmed)

Free-text/content search; pagination; labels/content in results; new capabilities;
aggregate/write/migration changes (index-only migration only if measured + called
out); cross-subject/global browse; Group E; dashboards.
