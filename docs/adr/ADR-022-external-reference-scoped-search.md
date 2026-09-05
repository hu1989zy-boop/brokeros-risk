# ADR-022: External-Reference Scoped Search / Browse (Q-020, Option B)

- Status: **Accepted** — 2026-09-05 (Product Owner, as part of the Q-020 §16.5-B
  bundle at the implementation-authorization gate).
- Supersedes: none. Builds on ADR-020 (association management) and ADR-021
  (association projection).
- Context docs: Requirement `docs/requirements/Q-020-Risk-Console-External-Reference-Search.md`;
  Architecture `docs/architecture/q-020-external-reference-search-architecture.md`.

## Context

Associating an external reference to a Risk Case (Q-018) requires the operator to
already know the reference's UUID: the four provenance modules (Evidence, Decision,
Action, ActionOutcome) expose only `POST` (record) and `GET /{ref}` (detail), with no
way to enumerate candidate references for a subject. Q-018 §5.3 deferred this
discovery capability (Option B) to Q-020. The Product Owner confirmed Q-020 V1 as a
**scoped browse** (no full-text search), all four modules, server-capped, references +
minimal metadata only, an index-only migration only if needed, and a console browse
restricted to the case in hand.

## Decision

Add, per provenance module, **one additive read endpoint returning a bounded,
content-free list of references keyed by the entity's natural scope**, and give the
console a **browse/pick** mode over them.

1. **Endpoints (per module):**
   - `GET /api/evidence?subjectRef={ta-…}` → evidence for that subject
   - `GET /api/decisions?subjectRef={ta-…}` → decisions for that subject
   - `GET /api/actions?decisionRef={dec-…}` → actions for that decision
   - `GET /api/action-outcomes?actionRef={act-…}` → outcomes for that action
2. **Authorization:** each reuses the module's existing `*:read` capability. **No new
   capability.**
3. **Response:** the standard `ApiResponse` envelope wrapping a **bounded** list
   (server cap, no pagination) of items `{reference, <scope-key ref>, recordedAt,
   status?}`. **No recorded free-text content** and **no truncated label** in V1.
4. **Semantics:** an unknown/empty scope key → an **empty** bounded list (not an
   error); a malformed key → the module's standard request-invalid `ResultCode`. The
   list path writes **no full-detail access-log entry** (it exposes no content).
5. **Persistence:** read-only `SELECT` over each record table by its scope key.
   **No migration** — the scope-key indexes already exist (`idx_evidence_record_subject`,
   `idx_decision_record_subject`, `idx_action_record_decision`,
   `idx_action_outcome_record_action`). An additive **index-only** migration is
   permitted **only** if a measured need is shown, and must be called out; V1 is
   expected to add none.
6. **Console:** `ReferenceInput` gains a browse/pick mode scoped to the case
   (subjectRef for evidence/decision; on-case decision/action refs for
   action/outcome); manual entry is retained as a fallback; the association requests
   and registry are unchanged.

## Consequences

**Positive**
- Closes the reference-discovery gap while staying least-authority and content-free.
- No new capability, no aggregate/write change, no migration — a small, bounded,
  reviewable additive read, consistent with Q-019's discipline.
- Per-module endpoints keep the `*:read` ownership boundary crisp and let coverage be
  phased later if ever needed.

**Negative / trade-offs**
- Four near-identical additive reads (one shared pattern; per-module real-MySQL
  tests mitigate).
- A server cap (no pagination) can truncate a very large scoped list; acceptable for
  V1 (scoped lists are small) and revisitable without breaking the shape.
- Browsing shows references but not content; the operator still opens `GET /{ref}`
  (the existing preview) to confirm — by design (content-free browse).

**Neutral**
- The index-only migration allowance (§5.3(5)) is retained but expected unused.

## Alternatives rejected

- **Free-text content search** — heavier (indexing/relevance) and exposes content;
  out of scope (Requirement §5.2, §5.3(1)).
- **One cross-module search endpoint** — blurs module ownership/capabilities.
- **Pagination in V1** — unnecessary for bounded scoped lists; a server cap matches
  the house discipline.
- **Serving discovery from `RiskCaseQueryService`** — that owns the case's own
  associations (Q-019), not external-module discovery.

## Compliance / verification

- Backend real-MySQL test per module (scoped set, empty-for-unknown-key, `*:read`
  authorization, content absent, cap bounds); full repository gate green; no aggregate
  diff; no migration (index-only only if shown + called out).
- Frontend tests for the browse/pick mode + manual fallback.
