# Q-019 — Claude Code Independent Implementation Review (v2)

- Requirement: Q-019 — Risk Case Association Projection Endpoint, V1
- Reviewed: Codex v1 delivery (`review-q-019-v1-implementation-20260903-204945`)
- Baseline: `a9cc877`
- Reviewer: Claude Code (external Architect role) — Date: 2026-09-03
- **Gate Decision: PASS** (one live condition — the console-driven `resolve` slice —
  outstanding; all other gates reproduced green)

## Verdict

The first backend-inclusive delivery of the console arc, and it is clean. Codex
added exactly the authorized additive read — one endpoint, one query-service
method, two read-only queries, one DTO, and a real-MySQL test — with **no
aggregate/business-rule/migration change and no new capability**. I independently
reproduced the backend real-MySQL gate (309/0/0, incl. the Q-019 endpoint test 4/4)
and the frontend gate (150/150 + build), and code-reviewed the read-only boundary.
Codex's own gate was honest (PASS WITH CONDITIONS; it did not claim the live
`resolve` slice it could not run).

## Independently reproduced

| Check | Result |
| --- | --- |
| Backend full real-MySQL gate (Docker MySQL 8.4 + Maven 21) | **309 tests, 0 failures, 0 errors, 0 skipped** |
| `Q019RiskCaseAssociationsMySqlTests` | **4/4** (projection incl. evidence event refs + supersession/replacement, two decisions/one current, action outcome; `risk-case:read` `403`; not-found; server cap) |
| Frontend `npm ci` → `tsc --noEmit` → `vitest` → `vite build` | 309 pkgs; **0 type errors; 150/150; build PASS** |
| Additive-read-only boundary | **confirmed** — no Risk Case aggregate/domain-write/migration/capability change; `git diff -- backend/` is read endpoint/query/DTO/tests only |

(An initial backend run showed 13 *errors* in the ArchUnit/config tests —
`NoSuchFileException: /backend/src/main/java/...`, "Cannot locate repository root".
These were an artifact of mounting only `backend/` in my Docker run; re-running with
the full repo mounted gave a clean 309/0/0. Not a code issue.)

## Code review (correct)

- **Endpoint:** `GET /api/risk-cases/{caseNumber}/associations` on the existing
  controller → `RiskCaseQueryService.associations`, which `requireAuthorized`
  (existing **`risk-case:read`**, mirroring `detail`/`history` — **no new
  capability**), resolves the case (not-found → `RISK_CASE_NOT_FOUND`), and
  assembles a **bounded** projection (a `requireWithinAssociationLimit` server cap
  per collection).
- **Projection:** evidence associations carry the **`eventRef`** (the Q-018 D2 gap),
  `evidenceRef`, `eventType`/disposition, `source`, `replacementEvidenceRef`,
  `occurredAt`; decisions carry `decisionRef` + `current` (via
  `equals(currentDecisionRef)`); actions carry `actionRef` + `outcomeRefs`.
  Refs/enums/timestamps only — no external entity content.
- **Queries:** the two additions (`findAllEvidenceEvents`,
  `findAllDecisionAssociations`) are read-only `SELECT`s over existing tables; the
  effective-evidence/action reads are reused. No DDL.
- **Frontend:** typed projection + repository method + TanStack Query hook;
  `AssociationsPanel` now reads the projection as authoritative (the "reconstructed
  from history" caveat removed); the evidence-disposition target is an on-case
  `eventRef` picker (Q-018 B1 manual fallback removed). Invalidate/refetch after
  association writes.

## Acceptance criteria — reviewer view

| AC | Result |
| --- | --- |
| 1 endpoint + projection | **PASS** — reproduced real-MySQL 4/4 |
| 2 additive read only | **PASS** — no aggregate/migration/capability change |
| 3 console authoritative panel + disposition picker | **PASS** — code + 150 tests; D2 closed |
| 4 live resolve slice | **outstanding** — needs a seeded decision/action (below); the backend real-MySQL test already proves the resolvable/associated state is reachable |
| 5 backend + frontend tests | **PASS** — 309/0/0 + 150/150 + build |

## The one outstanding live item (AC 4)

The console-driven live slice (associate a decision → select current → associate an
action → drive Q-017 `resolve`/`close`) — the "first live resolve" — was honestly
skipped by Codex and not run here: it needs a seeded Risk Case plus real
decision/action references in a resolvable state (current decision with an
associated action). That state is exercised at the backend level by the Q-019
real-MySQL test; the console association operations reuse the Q-017 `useCaseAction`
runner already live-verified (Q-017 assign/priority/cancel), and resolve/close were
code-reviewed in Q-017. The live end-to-end confirmation remains to run when a
decision/action Core-Domain seed is set up (it depends on the same deep seed noted
since Q-017).

## Recommendation

**Accept Q-019 V1.** The additive read endpoint is correct, boundary-clean, and
independently verified (309 backend + 4/4 Q-019 + 150 frontend); the console closes
the Q-018 D2 gap (authoritative panel + real disposition picker). The reviewer
changed nothing. Schedule the live `resolve` slice (AC 4) with a decision/action
seed — it is the final live confirmation of the Q-016→Q-019 console arc.
