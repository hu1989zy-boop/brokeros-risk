# Q-018 Association Management — Implementation Design

## Document Status

- Requirement: Q-018 — V1, APPROVED — 2026-09-03 — Product Owner.
- Architecture: Q-018 — V1 (see its §12). ADR: **ADR-020 — Accepted — 2026-09-03 — Product Owner**.
- Implementation Design submission: **V1 — live status in §11 (Gate),
  authoritative if this header disagrees** (Execution Protocol §16).
- Prepared by: Claude Code, external Architect role. §16.5-B bundle. Builds on
  ADR-018 (React) + ADR-019 (action framework).

## 1. Scope

Build spec for Q-018 V1: the six Group C association operations in the console,
reusing the Q-017 action registry/runner, with **manual reference entry +
fetch-by-ref preview** (Option A) for external references and on-case selection
for the rest. **No new backend endpoint**; the only backend-adjacent change is the
capability grant (§7).

## 2. Repository additions (`frontend/src/features/riskcase/`)

```
actions/actionDescriptors.ts     # + 6 association descriptors (reuse useCaseAction)
api/riskCaseRepository.ts         # + associateEvidence, changeEvidenceDisposition,
                                  #   associateDecision, selectDecision,
                                  #   associateAction, referenceActionOutcome
api/referencePreview.ts           # getEvidence/getDecision/getAction/getActionOutcome by ref
api/riskCaseTypes.ts              # + association request types + preview response types
model/useReferencePreview.ts      # TanStack Query: debounced GET /{ref}, typed states
ui/ReferenceInput.tsx             # ref field -> preview card -> confirm
ui/AssociationsPanel.tsx          # renders current evidence/decision/action associations
ui/RiskCaseDetailPage.tsx         # wires the panel + 6 actions + on-case pickers
```

## 3. Operations (exact endpoints/bodies; all POST, all include `expectedVersion`)

Paths relative to `/api/risk-cases`.

| id | path | body (besides expectedVersion) | external ref (preview) | on-case ref |
| --- | --- | --- | --- | --- |
| associateEvidence | `/{c}/evidence-associations` | evidenceRef, source, reason | evidenceRef (`evidence:read`) | — |
| changeEvidenceDisposition | `/{c}/evidence-associations/{eventRef}/dispositions` | disposition (SUPERSEDED\|INVALIDATED\|WITHDRAWN), replacementEvidenceRef?, source, reason | replacementEvidenceRef? (`evidence:read`) | eventRef (existing evidence association) |
| associateDecision | `/{c}/decision-associations` | decisionRef, reason | decisionRef (`decision:read`) | — |
| selectDecision | `/{c}/decision-selection` | decisionRef, reason | — | decisionRef (associated decisions) |
| associateAction | `/{c}/action-associations` | actionRef, reason | actionRef (`action:read`) | — |
| referenceActionOutcome | `/{c}/action-associations/{actionRef}/outcomes` | outcomeRef, reason | outcomeRef (`action-outcome:read`) | actionRef (associated actions) |

Path segments are `encodeURIComponent`-escaped. `disposition` is an enum select;
`source` is a free-text field (≤64).

## 4. Reference preview (Option A)

- `referencePreview.ts`: `getEvidence(ref)`, `getDecision(ref)`, `getAction(ref)`,
  `getActionOutcome(ref)` → the existing `GET /api/{evidence|decisions|actions|
  action-outcomes}/{ref}`, parsed to a small typed preview (key identifying
  fields only — do not dump full entity content).
- `useReferencePreview(kind, ref)`: TanStack Query, enabled only when `ref`
  passes the client format check (`^(ev|dc|ac|ao)-<uuid v4>$` per kind), debounced;
  states: idle / loading / valid(preview) / not-found(404→typed) / forbidden(403) /
  invalid-format. Reuses `ApiClient` (Bearer, envelope, error mapping).
- `ReferenceInput`: renders the field + a preview card; exposes a `confirmedRef`
  only when a valid preview is shown; the enclosing action dialog's submit is
  disabled until an external ref is confirmed.

## 5. On-case pickers

From the case detail/history already loaded:
- disposition `eventRef` — select among the case's existing evidence association
  events;
- `selectDecision.decisionRef` — select among the decisions associated to the case;
- outcome `actionRef` — select among the actions associated to the case.

**Verification point (Architecture §7):** confirm the detail/history payloads
expose these refs; if a specific ref is not present, that picker falls back to a
`ReferenceInput` (manual + preview) — still no new backend endpoint. Record the
outcome in `OutstandingItems.md`.

## 6. Detail-page integration

`AssociationsPanel` renders current associations (effective evidence; associated
decisions with the current one marked; associated actions with outcomes) and hosts
the six actions (external-ref actions via `ReferenceInput`; on-case actions via
pickers). After any success, detail + history refetch; version + associations
update. Version conflicts reload with input preserved (runner).

## 7. Configuration: capability grant (only backend-adjacent change)

Grant the console-operator actor, in `deploy/keycloak/q016-security-bootstrap.json`
(or a Q-018 successor the launcher references), the additional capabilities:

```
risk-case:associate, evidence:read, decision:read, action:read, action-outcome:read
```

(added to the existing Q-016/Q-017 set). Authorization config only; no code or
migration change. Record the same set as the production authorization expectation.

## 8. Typed contract

Add request types (`AssociateRiskCaseEvidenceRequest`,
`ChangeEvidenceAssociationDispositionRequest`, `AssociateRiskCaseDecisionRequest`,
`SelectRiskCaseDecisionRequest`, `AssociateRiskCaseActionRequest`,
`ReferenceActionOutcomeRequest`) and preview response types mirroring the module
`GET /{ref}` DTOs (identifying fields only). A contract test asserts the request
shapes and that unknown `ResultCode`s are not treated as success.

## 9. Testing (Requirement §13)

- **Vitest + RTL + MSW**, per operation: success; pending/disabled submit; client
  validation; ordinary `ResultCode` error; `403`; `RISK_CASE_VERSION_CONFLICT`
  reload + input preserved. Plus `ReferenceInput`/`useReferencePreview`: valid
  preview enables submit; 404/403/invalid-format block it; on-case pickers list
  the right candidates.
- **Live Playwright:** on a seeded case, associate a decision (preview an
  `dc-…` ref) → select it as current → associate an action (`ac-…`) → assert the
  associations + version; this leaves the case in a state where Q-017 `resolve`
  is reachable (AC 4).
- Typecheck + `vite build` + Vitest green; `git diff -- backend/` empty; the only
  non-frontend change is the bootstrap JSON grant under `deploy/`.

## 10. AC Traceability

AC1→ §3 operations end-to-end; AC2→ §4 preview + §5 on-case pickers; AC3→ runner
version conflict; AC4→ §9 live decision→select→action unblocks resolve; AC5→ §7
config-only + no identity in body; AC6→ §9 tests + build.

## 11. Gate

This Implementation Design + the Q-018 Architecture + ADR-020 form the §16.5-B
bundle at the Q-018 implementation-authorization gate. Live status: Q-018
Requirement §17. No implementation begins until the Product Owner accepts the
bundle; then Codex implements and Claude Code independently reviews (including the
live association slice and the resulting resolve reachability).
