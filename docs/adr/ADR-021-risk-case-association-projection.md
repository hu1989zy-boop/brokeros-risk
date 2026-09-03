# ADR-021: Risk Case Association Projection (additive read endpoint)

- Status: **Accepted — 2026-09-03 — Product Owner** — at the  Q-019 implementation-authorization gate (Decision Authority §16.5-B).
- Date: 2026-09-03
- Requirement: Q-019 — V1, APPROVED — 2026-09-03.
- Builds on: the committed Q-008 Risk Case aggregate + association tables, Q-009,
  and the Q-018 console (ADR-018/019/020). No stack change.
- Supersedes: None.

## Context

Q-018 could not build the evidence-disposition on-case picker or an authoritative
associations view (defect D2): the Risk Case reads expose neither an evidence
association's **event ref** (`EvidenceAssociationEventRef`) nor a projected
current-associations view — only `currentDecisionRef` and a generic history
timeline (`{version, eventType, affectedRef, actorRef}`). The data is already
persisted and internally queried (`findAllEffectiveEvidence`,
`findAllEffectiveActions`, evidence event rows with their refs, the
`risk_case_decision_association` table). The Product Owner approved Q-019 to expose
it via a bounded, authorized read — the first deliberate backend addition since
Q-016's list endpoint.

## Decision

### 1. One additive, bounded, `risk-case:read` projection endpoint

Add `GET /api/risk-cases/{caseNumber}/associations` to the existing
`RiskCaseController`, delegating to a new `RiskCaseQueryService.associations`
guarded by the existing **`risk-case:read`** capability (mirroring
`detail`/`history`/`listCases`). It returns a bounded
`RiskCaseAssociationsResponse`: evidence associations (each `eventRef`,
`evidenceRef`, disposition/`eventType`, `source`, `replacementEvidenceRef`),
associated decisions (each `decisionRef`, `isCurrent`), and effective actions
(each `actionRef` + referenced `outcomeRefs`). Refs and enums only — no external
entity content. Missing case → the standard not-found `ResultCode`.

### 2. Read-only backing queries; the aggregate is untouched

The projection reuses `findAllEffectiveEvidence` / `findAllEffectiveActions` and
adds **read-only** `SELECT` queries for the evidence association events (with their
`eventRef`) and the associated decisions (over `risk_case_decision_association`,
not currently exposed). **No aggregate/business-rule change, no migration, no new
table/column, no new capability.**

### 3. Console consumes the projection (D2 closed)

The Q-018 `AssociationsPanel` reads the projection as authoritative (dropping the
"reconstructed from history" caveat), and the evidence-disposition target becomes
an **on-case picker** over the projection's evidence associations (replacing the
Q-018 B1 manual-UUID fallback). This also enables the first live end-to-end
`resolve` verification (a decision + action can be associated and seen).

### 4. Option B deferred

External-reference browse/search across the four modules (discovery of *new* refs)
is a separate concern and Requirement (Q-020); Q-019 stays a single-case
association projection.

## Alternatives Considered

- **Split sub-resource endpoints** (`/evidence-associations`, `/decision-…`,
  `/action-…`) — rejected for V1: one authoritative projection is simpler for the
  panel and one round trip; sub-resources can come later if needed.
- **Keep the Q-018 history reconstruction / manual fallback** — rejected: it can't
  supply the evidence association event ref and isn't authoritative.
- **Expose the event ref by widening the existing history entry** — rejected:
  history is an event timeline, not a current-associations projection; a dedicated
  read is clearer and avoids overloading history's contract.
- **A materialized projection / new table** — rejected: unnecessary; the data is
  already queryable, so a read assembly suffices (no migration).

## Consequences

Positive: the console gets an authoritative associations view and a real
disposition picker (D2 closed); the first live `resolve` becomes verifiable; no
aggregate/migration/capability change; matches the Q-016 additive-bounded-read
precedent.

Costs/constraints: a small backend surface returns (the first backend change in
the console arc); it must stay a pure read consistent with the aggregate's
effective-association semantics (guarded by a real-MySQL test). Full-projection
(no pagination) relies on a case's associations being bounded; a server cap guards
pathological cases.

## Security Implications

`risk-case:read` only (no new grant); server-side authorization unchanged. The
projection returns the case's own association refs/states, not external content.
No identity beyond the Bearer JWT; no sensitive content logged.

## Data / Contract Implications

One additive endpoint + a bounded projection DTO + read-only queries. No schema
change. Console adds a typed projection model + hook.

## Operational Implications

No new infrastructure, capability, or migration. One additive read endpoint on an
existing controller.

## Deferred Decisions

Option B (Q-020); any write/aggregate change; sub-resource decomposition if later
needed.

## Approval Boundary

**Accepted by explicit Product Owner decision on 2026-09-03** at the Q-019 implementation-authorization gate, together with the Q-019 Architecture and Implementation Design (§16.5-B). Codex is authorized to implement Q-019 V1 as specified.
