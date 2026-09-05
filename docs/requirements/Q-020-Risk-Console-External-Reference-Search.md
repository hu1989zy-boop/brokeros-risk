# Q-020: Risk Console External-Reference Search / Browse (Option B)

## Status

V1 draft, by Claude Code holding the external Architect role, per the two
`docs/engineering/` governance documents. **The authoritative live status is §17
(Current Gate)** (Execution Protocol §16). Requirement stage; the §5.3 scope
questions are surfaced for explicit Product Owner confirmation (Decision Authority
§16.2) rather than silently assumed.

- Requirement ID: `Q-020`
- Type: **backend additive read (multi-module) + a console follow-through.** New
  bounded list/browse read endpoints on the four provenance modules
  (Evidence / Decision / Action / ActionOutcome), plus a browse/search mode in the
  Q-018 console `ReferenceInput`. This is the deferred **Option B** from Q-018 §5.3.
- Depends on: the committed Q-011..Q-014 provenance modules, Q-009 (JWT
  verification), and the Q-016→Q-019 console (Q-018 association UI, Q-019 projection).

## 1. Background

Q-018 delivered association management with **Option A**: to associate an external
reference (evidence / decision / action / action-outcome) the operator **types the
`ev-/dec-/act-/aoc-` reference by hand** and the console fetches it via the existing
`GET /{ref}` for a confirmation preview. Q-018 §5.3 explicitly **deferred Option B**
— browse/search pickers backed by new list endpoints — to a future Requirement
(candidate Q-020). This is that Requirement.

The gap Option A leaves open: **reference discovery.** To associate a reference the
operator must already know its UUID. The four provenance modules expose only
`POST` (record) and `GET /{ref}` (detail-by-ref); there is **no list or search
endpoint**, so nothing lets a client *enumerate* the candidate references for a
subject. In practice an operator working a case about a trading account cannot ask
"which evidence / decisions exist for this account?" from the console.

## 2. Existing Capability and Gap Analysis

| Module | Record | Detail by ref | **List / browse / search** |
| --- | --- | --- | --- |
| Evidence (`/api/evidence`) | `POST` | `GET /{evidenceRef}` | **No** |
| Decision (`/api/decisions`) | `POST` | `GET /{decisionRef}` | **No** |
| Action (`/api/actions`) | `POST` | `GET /{actionRef}` | **No** |
| ActionOutcome (`/api/action-outcomes`) | `POST` | `GET /{actionOutcomeRef}` | **No** |

Each module already records a natural scoping key that makes a **bounded** browse
safe and cheap:

- Evidence and Decision are recorded against a **`subjectRef`** (a `ta-…` trading
  account) — the same subject a Risk Case carries. → browse "by subject".
- Action is recorded against a **`decisionRef`**. → browse "by decision".
- ActionOutcome is recorded against an **`actionRef`**. → browse "by action".

So Option B does not need free-text search or content indexing to be useful: a
bounded **list keyed by the entity the operator is already looking at** (the case's
subject, or a decision/action already on the case) closes the discovery gap while
staying least-authority and content-free.

## 3. Problem Statement

The console (and any client) needs a **bounded, authorized way to discover candidate
external references** — evidence and decisions for a given trading-account subject,
actions for a given decision, outcomes for a given action — so the operator can
**pick** a reference to associate instead of knowing and typing its UUID. It must
expose **references and minimal metadata only** (not the external entities' recorded
content), and reuse each module's existing read capability.

## 4. Goals

1. Add **additive, bounded, authorized list/browse read endpoints** on the four
   provenance modules, each keyed by its natural scope (subject / decision / action),
   returning references + minimal metadata — no recorded content.
2. Reuse each module's existing **`*:read`** capability (`evidence:read`,
   `decision:read`, `action:read`, `action-outcome:read`) — **no new capability**.
   (The committed console operator already holds all four.)
3. Keep them **pure reads**: no aggregate/business-rule change, no write path, and no
   table/column change (additive read queries; an index-only migration only if §5.3
   confirms it).
4. Give the Q-018 console `ReferenceInput` a **browse/pick mode** — scoped to the
   case's own subject (and to on-case decisions/actions) — that replaces manual UUID
   entry with selection, while keeping manual entry as a fallback.
5. Return **references + minimal metadata only** (privacy: no `observationText` /
   `conclusionText` / `intentText` / `outcomeText`).

## 5. Scope and Non-Goals

### 5.1 In Scope (V1 — CONFIRMED §5.3: scoped browse; all four modules; server-capped; refs+metadata; index-only migration if needed; case-scoped console)

- Bounded list/browse read endpoints on **all four** modules (Evidence / Decision /
  Action / ActionOutcome), each keyed by its natural scope (evidence & decisions by
  `subjectRef`, actions by `decisionRef`, outcomes by `actionRef`), authorized by the
  module's existing `*:read` capability, returning the standard `ApiResponse`
  envelope with **references + minimal metadata only** (refs, parent/subject ref,
  `occurredAt`, status/outcome enums — no recorded content), **server-capped, no
  pagination**.
- The console follow-through: `ReferenceInput` gains a **browse/pick** mode over
  these endpoints, **scoped to the case's own subject** (evidence/decisions) and to
  on-case decisions/actions (actions/outcomes), replacing manual entry as the default
  while retaining it as a fallback.
- If a scoped-list query needs a supporting index, a single **additive index-only**
  Flyway migration is permitted (no table/column/data change).

### 5.2 Non-Goals (defer)

- **Free-text / content search** over recorded evidence/decision/action text, and any
  content indexing (search infrastructure, relevance ranking). Out unless §5.3(1)
  chooses it — not recommended for V1.
- Any **aggregate/business-rule/migration** change beyond an optional additive
  **index-only** migration (§5.3(5)); any write operation; corrections/dispositions
  (those exist already).
- Group E (case creation); dashboards / cross-account analytics (need Q-015).
- Cross-subject or global browse not tied to the case in hand (least-authority — see
  §5.3(6)).

### 5.3 Scope decisions — OPEN, for Product Owner confirmation

1. **Browse key vs. free-text search — CONFIRMED (Product Owner, 2026-09-05):
   scoped browse, no full-text search.** List evidence & decisions by
   **`subjectRef`**, actions by **`decisionRef`**, outcomes by **`actionRef`**. This
   closes the discovery gap, stays bounded and content-free, and needs no search
   infrastructure. Free-text content search is **out of scope** for V1 (see §5.2).
2. **Module coverage in V1 — CONFIRMED (Product Owner, 2026-09-05): all four**
   modules (Evidence, Decision, Action, ActionOutcome). Consistent picker everywhere;
   action/outcome browse is cheap given the decision/action key.
3. **Bounding — CONFIRMED (recommended default): server-capped bounded list, no
   pagination** (match the Q-016/Q-019 bounded-projection discipline); revisit
   pagination only if a cap proves too small.
4. **Return fields — CONFIRMED (recommended default): references + minimal metadata**
   — subject/parent ref, `occurredAt`, and status/outcome enums where they exist;
   **no** recorded free-text content and **no** label in V1 (add a capped label later
   only if operators need it).
5. **Read index (performance) — CONFIRMED (Product Owner, 2026-09-05): an additive,
   index-only Flyway migration is permitted** (no table/column/data change) **only
   if** the implementer/reviewer shows the scoped-list query needs it; otherwise none.
   A deliberate, bounded relaxation of the Q-016→Q-019 "no migration" norm, limited to
   index-only.
6. **Least-authority scope in the console — CONFIRMED (recommended default): the
   console browse is restricted to the case's own subject** (and to decisions/actions
   already on the case) — the operator discovers only references relevant to the case
   in hand, not a global catalogue. The endpoint stays general (takes the key as a
   parameter); the console supplies the case's subject.

## 6. Definitions

- **External reference** — an `ev-/dec-/act-/aoc-` reference owned by another module,
  candidate for association to a Risk Case.
- **Scoped browse** — a bounded list of references for a given key (subject /
  decision / action), returning refs + minimal metadata, not recorded content.
- **Reference discovery gap** — the Q-018 Option A limitation: association requires
  knowing the ref UUID; Option B removes it.

## 7. Functional Requirements

- **Q020-FR-01** Each in-scope module exposes a bounded list/browse read endpoint
  keyed by its natural scope (evidence & decisions by `subjectRef`; actions by
  `decisionRef`; outcomes by `actionRef`), returning references + minimal metadata
  (§5.3(4)) in the standard `ApiResponse` envelope.
- **Q020-FR-02** Each endpoint is authorized by that module's existing `*:read`
  capability; **no new capability**. An unauthorized caller gets the standard
  authorization denial; an unknown key yields an empty bounded result (not an error).
- **Q020-FR-03** Endpoints are **pure reads** — no aggregate/business-rule change, no
  write path, no table/column change (additive read queries; index-only migration
  only per §5.3(5)).
- **Q020-FR-04** Results are bounded (§5.3(3)) and expose **no recorded free-text
  content** (§5.3(4)); refs, parent/subject refs, timestamps, and status/outcome
  enums only (plus a capped label only if §5.3(4) chooses it).
- **Q020-FR-05** The console `ReferenceInput` offers a **browse/pick** mode over these
  endpoints, scoped per §5.3(6) (the case's subject; on-case decisions/actions),
  selectable into the existing Q-018 association operations, with **manual entry
  retained as a fallback**.
- **Q020-FR-06** Picking a browsed reference feeds the **same** Q-018 association
  request (evidence/decision/action association, outcome reference) — Option B changes
  only *how a ref is chosen*, not the association contract or its authorization.

## 8. Security Requirements

- Authorization server-side via the existing per-module `*:read` capabilities; **no
  new capability**. Endpoints return references + metadata, never recorded content.
  Least-authority: the console browse is scoped to the case in hand (§5.3(6)). No
  identity in requests beyond the Bearer JWT; no refs/content in logs.

## 9. Data / Contract Requirements

- Up to four additive REST read endpoints + bounded list response DTOs; typed console
  models + a browse mode in `ReferenceInput`. **No new table or column**; additive
  read queries only (an index-only migration only per §5.3(5)).

## 10. Acceptance Criteria

1. Each in-scope module's list/browse endpoint returns the correct bounded set of
   references + minimal metadata for a given key, authorized by the module's
   `*:read`, against real MySQL; an unknown key returns an empty bounded result.
2. No aggregate/business-rule change and no table/column change; `git diff` on
   aggregates is empty and there is no schema migration beyond an optional
   index-only one (§5.3(5)); only additive read query/endpoint/DTO code is added.
3. No recorded free-text content is exposed by any endpoint (refs + timestamps +
   enums, plus a capped label only if §5.3(4) chose it).
4. The console `ReferenceInput` browse/pick mode lists references scoped to the case
   (§5.3(6)), a picked ref drives the existing Q-018 association operation
   end to end, and manual entry still works as a fallback.
5. Backend tests (real-MySQL for each endpoint incl. authorization + bounding +
   empty-key) + frontend tests (browse/pick + fallback) pass; typecheck/build clean.

## 11. Technical Constraints

- Additive read only; per module, add one list query keyed by the natural scope +
  one controller read method + one bounded list DTO. Reuse the module's existing
  `*:read` capability and query-service pattern (mirror the `GET /{ref}` detail
  services). Bounded results; no content.
- Console: React (ADR-018); reuse the Q-016 `ApiClient` and the Q-018 `ReferenceInput`
  / association framework — add a browse/pick mode, do not fork the association flow.

## 12. Dependencies

- Q-011..Q-014 provenance modules (record + detail + their subject/parent keys);
  Q-009; the Q-016→Q-019 console. Independent of Q-015.

## 13. Verification Plan

- Backend: a real-MySQL test per in-scope module asserting the bounded scoped list
  (correct set for a key; empty for an unknown key; `*:read` authorization; content
  absent); the full repository gate stays green; confirm no aggregate diff and no
  migration beyond an optional index-only one.
- Frontend: unit/component tests for the `ReferenceInput` browse/pick mode (lists
  scoped refs, selects into the association op, falls back to manual entry).
- A console slice: browse the case's subject → pick an evidence/decision → associate,
  reusing the Q-018 flow.

## 14. Risks and Inputs

- **Scope creep into free-text search** — mitigated by §5.3(1) (recommend scoped
  browse) and §5.2.
- **Content leakage** — mitigated by FR-04 (refs + metadata only; content-free).
- **Multi-module surface** — four near-identical additive reads; mitigated by one
  shared pattern and per-module real-MySQL tests. §5.3(2) may phase coverage.
- **Performance of scoped lists** — mitigated by bounding (§5.3(3)) and an optional
  index-only migration (§5.3(5)).

## 15. Deliverables

- This Requirement; then (after approval + §5.3) the Architecture / ADR /
  Implementation Design bundle (§16.5-B) and a Codex prompt; then implementation and
  independent review.

## 16. Review Checklist

- [ ] §5.3 decisions confirmed (browse vs. search; module coverage; bounding; return
      fields; index-only migration; least-authority console scope).
- [ ] Additive-read-only / no aggregate change / no table-column change confirmed.
- [ ] No new capability (reuse per-module `*:read`) confirmed.
- [ ] Product Owner Gate Decision recorded (§17).

## 17. Current Gate

Q-020 Requirement status: **APPROVED — V1 — 2026-09-05 — Product Owner.**
Gate Decision: **PASS.** All six §5.3 decisions confirmed (below). The Product Owner
also **authorized the §16.5-B bundle** (Architecture + ADR-022 + Implementation
Design produced together at the one implementation-authorization gate); the bundle
documents follow, and each records its own gate. This section remains the Requirement's
single live status.

All six §5.3 decisions are confirmed: (1) **scoped browse** by
subject/decision/action — **no full-text search**; (2) **all four** modules; (3)
**server-capped bounded list, no pagination**; (4) **references + minimal metadata
only** (refs, parent/subject ref, `occurredAt`, status/outcome enums — no recorded
content, no label in V1); (5) an **additive index-only** Flyway migration permitted
**only if** the scoped-list query is shown to need it; (6) the console browse is
**restricted to the case's own subject** (and on-case decisions/actions).

Confirmed V1 scope: a **multi-module additive read** — four bounded scoped-list
endpoints (Evidence & Decision by `subjectRef`, Action by `decisionRef`,
ActionOutcome by `actionRef`) reusing the existing per-module `*:read` capabilities
(**no new capability**), **no aggregate/business-rule change, no table/column change**
(index-only migration only if needed) — plus a console `ReferenceInput` browse/pick
mode scoped to the case, with manual entry retained as a fallback.

Q-020 Architecture V1 / ADR-022 / Implementation Design V1: **ACCEPTED (bundle) —
2026-09-05 — Product Owner** at the implementation-authorization gate (§16.5-B).
ADR-022 is **Accepted**. **Implementation AUTHORIZED — 2026-09-05 — Product Owner.**
The Codex implementation prompt (`prompts/Q-020-Implementation-Prompt.md`) is
**CLEARED FOR USE**.

Confirmed build shape: four additive scoped-list read endpoints on the existing
provenance-module controllers, each `*:read`-authorized, reading `{ref, scope-key ref,
recordedAt, status?}` from the existing `*_record` tables by their existing scope-key
indexes (**no migration**), returning a bounded content-free `ApiResponse` list;
console `ReferenceInput` gains a browse/pick mode (scoped to the case) with manual
entry retained. **No aggregate/business-rule change, no table/column change, no new
capability.** Key finding: all four scope-key indexes already exist (V4–V7), so V1 is
expected to add no migration at all.

Q-020 implementation (Codex, v1
`review/q-020/review-q-020-v1-implementation-20260905-223756`): four additive
scoped-list endpoints + list services + query-port/JDBC read methods + list DTOs +
module wiring + a real-MySQL test per module, and the console `ReferenceListRepository`
+ provider + four conditional hooks + `ReferenceInput` browse/pick mode +
`CaseActionDialog`/`RiskCaseDetailPage` case-scoped wiring + tests. No
aggregate/business-rule/migration change; no new capability.

Claude Code independent review: **PASS — 2026-09-05** — see
`review/q-020/review-q-020-v2-claude-code-independent-review-20260905-225541/`.
Independently reproduced: backend full real-MySQL gate **317/0/0** (incl. the four
`Q020…ReferenceListMySqlTests` **4/4** — bounded 202→200, most-recent-first, unknown
key → empty, malformed → request-invalid 400, denied → 403, content field absent, no
access-log row, scope-key index present); frontend **156/156** + typecheck 0 +
`vite build`; boundary confirmed (additive read only, **no migration** — Flyway V1–V8,
no aggregate/write change, **no new capability**, content-free SQL/DTO, least-authority
console scope). Codex's one interpretation (validate the scope key via the module value
object → request-invalid, vs a controller `@Pattern`) confirmed correct.

Q-020 (V1) acceptance: **ACCEPTED — 2026-09-06 — Product Owner**; committed with the
implementation + Codex v1 review package + the v2 independent review package.

Q-020 status: **COMPLETE — 2026-09-06** (External-Reference Scoped Search / Browse,
Option B: four content-free scoped-list endpoints reusing existing `*:read`; console
`ReferenceInput` case-scoped browse/pick with manual fallback). The Q-016→Q-019 console
arc's last deferred gap (Option B) is closed. Minor V1 note: outcome browse scopes to
the first on-case action when several exist (manual entry covers the rest). Q-015
remains parked awaiting the MT4/MT5 SDK.
