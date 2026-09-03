# Q-019 Association Projection — Implementation Design

## Document Status

- Requirement: Q-019 — V1, APPROVED — 2026-09-03 — Product Owner.
- Architecture: Q-019 — V1 (see its §11). ADR: **ADR-021 — Accepted — 2026-09-03 — Product Owner**.
- Implementation Design submission: **V1 — live status in §10 (Gate),
  authoritative if this header disagrees** (Execution Protocol §16).
- Prepared by: Claude Code, external Architect role. §16.5-B bundle.

## 1. Scope

Build spec for Q-019 V1: one additive, bounded, `risk-case:read`-authorized
backend read endpoint returning a Risk Case's current-associations projection, and
the Q-018 console switch to consume it (authoritative panel + evidence-disposition
on-case picker). **No aggregate/business-rule/migration change; no new capability.**

## 2. Backend

### 2.1 Endpoint

On `RiskCaseController` (existing), add:

```
@GetMapping("/{caseNumber}/associations")
ApiResponse<RiskCaseAssociationsResponse> associations(@PathVariable String caseNumber)
```

Delegates to a new `RiskCaseQueryService.associations(actorContext, caseNumber)`
that (a) `requireAllowed(actorContext, RiskCaseCapabilities.READ)` — mirroring
`detail`/`history`; (b) resolves the case (not-found → `RISK_CASE_NOT_FOUND`);
(c) assembles and returns the projection. Bounded: a server cap
(e.g. 500 per collection) guards pathological cases; return the case's associations
in full otherwise (no pagination).

### 2.2 Projection assembly (read-only)

- **Evidence:** list the case's evidence association events. Add a read-only
  repository query `List<EvidenceAssociationEvent> findAllEvidenceEvents(RiskCaseId)`
  if not present (the rows exist; this only lists them with `eventRef`). Combine
  with `findAllEffectiveEvidence` for effective state as needed.
- **Decisions:** add a read-only `List<DecisionAssociation>
  findAllDecisionAssociations(RiskCaseId)` over `risk_case_decision_association`
  (not currently exposed); mark `currentDecisionRef` (from the aggregate/detail) as
  current.
- **Actions:** `findAllEffectiveActions(RiskCaseId)` (existing) → action refs +
  referenced outcome refs.

All additive queries are plain `SELECT`s over existing tables; **no schema change.**

### 2.3 Response DTO

`RiskCaseAssociationsResponse`:
```
caseNumber: String, version: long,
evidenceAssociations: [ { eventRef, evidenceRef, disposition (eventType),
                          source, replacementEvidenceRef?, occurredAt } ],
decisions:            [ { decisionRef, current: boolean } ],
actions:              [ { actionRef, outcomeRefs: [String] } ]
```
Refs/enums/timestamps only — no external entity content.

## 3. Frontend (console follow-through)

- `riskCaseTypes.ts`: add `RiskCaseAssociations` types mirroring the DTO.
- `riskCaseRepository.ts`: add `getAssociations(caseNumber): Promise<RiskCaseAssociations>`
  → `GET /api/risk-cases/{c}/associations` via the existing `ApiClient`.
- `riskCaseQueries.ts`: a TanStack Query hook keyed by case number; invalidated
  after any Q-018 association write.
- `AssociationsPanel.tsx`: consume the projection as the **authoritative** source;
  remove the Q-018 "reconstructed from history" caveat.
- Evidence-disposition action: the target becomes an **on-case picker** over
  `evidenceAssociations[]` (option value = `eventRef`), replacing the Q-018 B1
  manual-UUID fallback. Decision-selection / action-for-outcome pickers also read
  from the projection.

## 4. Testing

- **Backend (real MySQL):** seed (or drive) a case with an attached-then-superseded
  evidence association (two events incl. a disposition + replacement), two associated
  decisions (one current), and an action with a referenced outcome; assert the
  projection returns the correct `eventRef`s, disposition, current-decision marking,
  outcomes, and bounding. Plus `risk-case:read` authorization (`403` without) and
  not-found. Run the full repository real-MySQL gate — it must stay green.
- **Frontend:** `AssociationsPanel` + disposition picker consume the projection
  (unit + component, MSW); version-conflict/refetch after writes.
- **Live slice (Playwright):** seed a decision + action; via the console associate
  the decision → select current → associate the action; read the projection
  (panel + picker show them); then drive Q-017 `resolve` and `close` — the **first
  end-to-end `resolve` verification**, confirming Q-018/Q-019 unblock the lifecycle.
- Typecheck + `vite build` + Vitest green; `git diff` shows **no aggregate or
  migration change** (only additive read query/endpoint/DTO under backend + the
  console consumption).

## 5. Boundaries

- No aggregate/business-rule change; no Flyway migration; no new table/column; no
  new capability (`risk-case:read` only). No write path. No Option B (external-ref
  search). The added backend code is read-only query + endpoint + DTO + tests.

## 6. Typed contract

Backend DTO + a real-MySQL projection test doubling as the contract check;
frontend types mirror the DTO; a frontend contract test asserts parsing and that
unknown `ResultCode`s aren't treated as success.

## 7. AC Traceability

AC1→ §2 endpoint + §4 backend test; AC2→ §5 no aggregate/migration; AC3→ §3 panel +
disposition picker; AC4→ §4 live resolve slice; AC5→ §4 backend + frontend tests.

## 8. Deferred

Option B (Q-020); write operations; sub-resource endpoints.

## 9. Verification honesty

Backend runs in the project's real-MySQL gate (disposable MySQL). The live resolve
slice needs the full local stack + a seeded decision/action; if unavailable, the
implementer delivers the spec marked "not executed" and never claims a pass that
did not run. Node unit/component and the backend real-MySQL endpoint test must be
genuinely executed.

## 10. Gate

This Implementation Design + the Q-019 Architecture + ADR-021 form the §16.5-B
bundle at the Q-019 implementation-authorization gate. Live status: Q-019
Requirement §17. No implementation begins until the Product Owner accepts the
bundle; then Codex implements and Claude Code independently reviews (including the
first live `resolve` slice).
