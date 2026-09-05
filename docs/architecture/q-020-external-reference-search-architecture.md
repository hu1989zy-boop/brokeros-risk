# Q-020 Architecture — External-Reference Scoped Search / Browse (Option B)

Status: **V1**, part of the §16.5-B bundle (Architecture + ADR-022 + Implementation
Design) authorized at Q-020's implementation-authorization gate (Requirement §17,
APPROVED 2026-09-05). Authoritative over the Implementation Design where they differ;
subordinate to the Requirement and ADR-022.

Governed by `docs/engineering/AI-Engineering-Execution-Protocol.md` and
`docs/engineering/Architecture-and-Design-Decision-Principles.md`.

## 1. Context

Q-018 gave the console association management with **Option A** (manual `ev-/dec-/
act-/aoc-` entry + fetch-by-ref preview via the existing `GET /{ref}`). The gap is
**reference discovery**: the four provenance modules expose only `POST` (record) and
`GET /{ref}` (detail) — no way to *enumerate* candidate references for a subject.
Q-020 (Option B) adds **bounded, scoped, content-free list endpoints** so the console
can present a **browse/pick** control instead of requiring the operator to know a
reference's UUID.

The Requirement §5.3 is fully confirmed: scoped browse (no full-text), all four
modules, server-capped (no pagination), references + minimal metadata only, an
index-only migration allowed **only if needed**, and the console browse restricted to
the case in hand.

## 2. Architectural decision (summary; ADR-022 is authoritative)

Add, on each of the four provenance modules, **one additive read path** that mirrors
the existing detail-read path but returns a **bounded list keyed by the entity's
natural scope**, exposing **references + minimal metadata only**:

| Module | New endpoint | Scope key | Existing index |
| --- | --- | --- | --- |
| Evidence | `GET /api/evidence?subjectRef=ta-…` | `subject_ref` | `idx_evidence_record_subject` ✓ |
| Decision | `GET /api/decisions?subjectRef=ta-…` | `subject_ref` | `idx_decision_record_subject` ✓ |
| Action | `GET /api/actions?decisionRef=dec-…` | `decision_ref` | `idx_action_record_decision` ✓ |
| ActionOutcome | `GET /api/action-outcomes?actionRef=act-…` | `action_ref` | `idx_action_outcome_record_action` ✓ |

**Key finding — no migration is expected.** Every scope key already carries a
supporting index (V4–V7). The §5.3(5) index-only allowance therefore stays a unused
safety net; V1 should add **no Flyway migration** unless a measured need appears, and
the reviewer must confirm the backend diff contains none.

Each endpoint:

- is authorized by the module's **existing `*:read` capability** (`evidence:read`,
  `decision:read`, `action:read`, `action-outcome:read`) — **no new capability**; the
  committed console operator already holds all four;
- returns the standard `ApiResponse` envelope wrapping a **bounded** list (a sane
  server cap, no pagination) of `{ref, scope-key ref, recordedAt, status?}` — **no
  recorded free-text content** (`observationText` / `conclusionText` / `intentText` /
  `outcomeText` are never read or returned);
- treats an **unknown/empty scope key as an empty result**, not an error; a malformed
  key is the module's standard request-invalid `ResultCode`;
- does **not** write a full-detail access-log entry (unlike `GET /{ref}`): a
  metadata-only browse exposes no content, so it records at most a light browse
  metric, not a content-access audit.

## 3. Why this shape (alternatives considered)

- **Free-text content search** (rejected, §5.3(1)) — needs content indexing +
  relevance + exposes recorded text; disproportionate and against the content-free
  posture. Deferred indefinitely.
- **A single cross-module search endpoint** (rejected) — would blur module ownership
  and capabilities; each module owns and authorizes its own references. Per-module
  endpoints keep the `*:read` boundary crisp.
- **Pagination now** (rejected for V1, §5.3(3)) — a server cap matches the Q-016/Q-019
  bounded-projection discipline; scoped lists (per subject/decision/action) are small.
  Pagination can be added later without breaking the shape.
- **Reuse `RiskCaseQueryService`** (rejected) — discovery is about *external* module
  references, not the case's own associations (that is Q-019). The read belongs in
  each provenance module, mirroring its detail-read service.

## 4. Boundaries (hard)

- **Additive read only.** No aggregate/domain/write change on any module; no change to
  `record`/`correct`/association write paths. No new table or column; **no migration**
  (index-only permitted only if a measured need is shown and called out).
- **No new capability.** Reuse the four existing `*:read` capabilities.
- **Content-free.** The list DTOs carry references, the scope-key ref, `recordedAt`,
  and a status enum where the record has one (evidence, action). Never the recorded
  text; never a truncated label in V1.
- **No cross-module coupling.** Each module's list path uses only its own
  tables/port; no module reads another's content to build a list.
- **Console least-authority.** The browse control is scoped to the case in hand — the
  case's `subjectRef` (evidence/decision) and on-case decision/action refs
  (action/outcome). The endpoints stay general (key as a query parameter).

## 5. Console architecture

`ReferenceInput` (Q-018) currently takes `kind: ReferenceKind` + `value` and shows a
fetch-by-ref **preview** (Option A). Q-020 adds a **browse/pick** affordance beside
manual entry:

- a control (e.g. a searchable `Select` or a "Browse" popover) that, given the case
  context, calls the matching scoped-list endpoint — evidence/decision by the case's
  `subjectRef`; action by an on-case decision ref; outcome by an on-case action ref —
  and lists candidates as `{ref · recordedAt · status?}`;
- picking a candidate sets `value`, after which the **existing** fetch-by-ref preview
  confirms it (unchanged) and the **existing** Q-018 association request fires
  unchanged;
- **manual entry is retained** as a fallback for every kind.

New frontend surface: a `getEvidenceList/getDecisionList/getActionList/getOutcomeList`
repository method + a TanStack Query hook per kind, and the browse UI in
`ReferenceInput` (fed the case's subject + on-case decision/action refs by
`CaseActionDialog`). No change to the association action registry or its requests.

## 6. Testing posture

- Backend: a real-MySQL test per module — the scoped list returns the right bounded
  set for a key, an unknown key returns empty, `*:read` authorizes, content is absent,
  and the cap bounds the result; the full repository gate stays green; **no aggregate
  diff and no migration**.
- Frontend: `ReferenceInput` browse/pick (lists scoped candidates, selects into the
  association op, falls back to manual) — Vitest/RTL + MSW.

## 7. Deliverables

The Implementation Design (`q-020-external-reference-search-implementation-design.md`)
specifies the per-module query service, port method + JDBC `SELECT`, controller
method, list DTO, and the console browse mode. ADR-022 records the decision.
