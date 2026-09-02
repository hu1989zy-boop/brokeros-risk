# Q-017 Case Lifecycle Operations — Implementation Design

## Document Status

- Requirement: Q-017 — V1, APPROVED — 2026-09-03 — Product Owner.
- Architecture: Q-017 — V1 (see its §12). ADR: **ADR-019 — Accepted — 2026-09-03
  — Product Owner**.
- Implementation Design submission: **V1 — live status in §11 (Gate),
  authoritative if this header disagrees** (Execution Protocol §16).
- Prepared by: Claude Code, external Architect role. Part of the §16.5-B bundle
  presented at the implementation-authorization gate. Builds on ADR-018 (React).

## 1. Scope

The authoritative build spec for Q-017 V1: surface Groups **A + B + D** case
lifecycle operations in the React Risk Console as guarded, version-safe actions,
reusing the Q-016 stack and the existing backend endpoints. **No backend
business-logic/aggregate/migration change**; the one backend-adjacent change is
the operator capability grant (§8).

## 2. Repository Layout (additions to `frontend/src/features/riskcase/`)

```
features/riskcase/
├── actions/
│   ├── actionDescriptors.ts   # the 11 descriptors (id,label,endpoint,inputs,allowedFrom,terminal,codes)
│   ├── useCaseAction.ts        # shared runner (TanStack Query useMutation over the repository)
│   └── actionInputs.ts         # typed input schemas + client-side validation per op
├── api/
│   ├── riskCaseTypes.ts        # + request/response types for each operation
│   └── riskCaseRepository.ts   # + one typed method per operation (reuses ApiClient.post)
└── ui/
    ├── CaseActionsBar.tsx       # renders status-valid actions
    ├── CaseActionDialog.tsx     # generic reason/inputs form; confirmation step when terminal
    ├── NotesPanel.tsx           # lists notes with a "Correct" action per note
    └── RiskCaseDetailPage.tsx   # wires the bar + dialog + notes panel
```

## 3. Action descriptor + runner

- `CaseActionDescriptor`: `{ id, label, method: 'POST', path(caseNumber, …params),
  inputs: FieldSpec[], allowedFrom: RiskCaseStatus[], terminal: boolean,
  messages?: Partial<Record<ResultCode,string>> }`.
- `useCaseAction(descriptor)`: returns `{ run(inputs), isPending, error }`.
  - Builds the request body from `inputs` + injects `expectedVersion` from the
    currently loaded case; calls the repository method via `ApiClient` (Bearer,
    `401` refresh, `403`→`AuthorizationError`, envelope parse — all from Q-016).
  - On success: `queryClient.invalidateQueries` for the case detail + history;
    close the dialog.
  - On `ApiError` with code `RISK_CASE_VERSION_CONFLICT`: refetch the case,
    update the in-dialog `expectedVersion`, keep the operator's field values, and
    show a "the case changed — review and retry" notice (no data loss).
  - On `AuthorizationError` (`403`) or other `ResultCode`: show a typed, readable
    message (descriptor `messages` override, else the backend `message`).

## 4. Operations (exact endpoints, methods, request bodies)

All `POST`; all bodies include `expectedVersion: number`. Paths are relative to
`/api/risk-cases`.

| id | path | body fields (besides expectedVersion) | allowedFrom | terminal |
| --- | --- | --- | --- | --- |
| assign | `/{c}/assignments` | assigneeRef, reason | OPEN, IN_REVIEW, ACTION_REQUIRED | no |
| changePriority | `/{c}/priority-changes` | priority (LOW\|NORMAL\|HIGH\|CRITICAL), reason | OPEN, IN_REVIEW, ACTION_REQUIRED | no |
| beginReview | `/{c}/review-start` | reason | OPEN | no |
| markActionRequired | `/{c}/action-required` | reason | IN_REVIEW | no |
| returnToReview | `/{c}/review-return` | reason | ACTION_REQUIRED | no |
| resolve | `/{c}/resolutions` | outcome, resolutionSummary, evidenceRefs?: string[], actionRefs?: string[] | IN_REVIEW, ACTION_REQUIRED | **yes** |
| close | `/{c}/closure` | reason | RESOLVED | **yes** |
| cancel | `/{c}/cancellation` | reason, duplicateCaseNumber?: string | OPEN, IN_REVIEW, ACTION_REQUIRED | **yes** |
| resume | `/{c}/resume` | reason, assigneeRef?: string | RESOLVED | no |
| reopen | `/{c}/reopen` | reason, assigneeRef?: string | CLOSED | no |
| correctNote | `/{c}/notes/{noteRef}/corrections` | content | (note exists) | no |

- `resolve.evidenceRefs`/`actionRefs` are **optional** in V1 and may be left
  empty (association pickers are deferred — do not build a reference browser).
  Provide optional free-text multi-entry, or omit; do not block resolve on them.
- The `allowedFrom` sets are the UX gate only (approach c); the backend is
  authoritative. Match the field regexes the backend enforces (e.g. priority
  enum, reason non-empty) for client-side pre-validation.

## 5. Confirmation for terminal actions

`resolve`, `close`, `cancel`: the dialog shows a distinct confirmation step
stating the consequence ("This will resolve/close/cancel the case") and requires
the mandatory reason (and, for resolve, outcome + summary) before enabling the
submit. Non-terminal actions submit directly from their form.

## 6. Detail page integration

- `RiskCaseDetailPage` renders `CaseActionsBar` (status-valid actions for the
  loaded case) beside the existing Reload/Add-note controls.
- Selecting an action opens `CaseActionDialog` bound to that descriptor.
- `NotesPanel` lists existing notes (from detail/history) and offers "Correct"
  per note → the `correctNote` descriptor with `{noteRef}` bound.
- After any successful action, detail + history refetch and the version updates.

## 7. Typed contract

Add request/response TypeScript types mirroring the backend DTOs
(`ChangeRiskCaseAssignmentRequest`, `ChangeRiskCasePriorityRequest`,
`ResolveRiskCaseRequest`, `CloseRiskCaseRequest`, `CancelRiskCaseRequest`,
`ResumeResolvedRiskCaseRequest`, `ReopenClosedRiskCaseRequest`,
`CorrectRiskCaseNoteRequest`, and the review-workflow reason bodies). Responses
reuse the existing detail/note/association response parsers. A contract test
asserts the request shapes and that unknown `ResultCode`s are not treated as
success (mirrors the Q-016 contract test).

## 8. Configuration: operator capability grant (the one backend-adjacent change)

Update the security bootstrap so the console-operator actor holds the V1 set:

```
capabilities: [ risk-case:read, risk-case:note, risk-case:assign,
                risk-case:review, risk-case:resolve, risk-case:close,
                risk-case:cancel, risk-case:reopen ]
```

Apply it in `deploy/keycloak/q016-security-bootstrap.json` (or a Q-017 successor
file the launcher references). This is authorization config; no code/migration
change. Record the same set as the production authorization expectation.

## 9. Testing (Design §9 of the Requirement)

- **Vitest + RTL + MSW**, per operation: success; pending/disabled-submit;
  client validation; ordinary `ResultCode` error; `403` → typed error;
  `RISK_CASE_VERSION_CONFLICT` → reload + inputs preserved. Plus: `CaseActionsBar`
  offers exactly the status-valid actions; terminal actions require the
  confirmation step; `correctNote` targets the right `noteRef`.
- **Playwright (extends the Q-016 live harness):** seed a case, then
  assign → begin-review → change-priority → resolve → close, asserting the
  status/version transitions and audit via the detail/history reads.
- Typecheck + `vite build` + Vitest all green; `git diff -- backend/**/*.java`
  and migrations empty (only the bootstrap JSON grant changes under `deploy/`).

## 10. AC Traceability

AC1→ §4 operations end-to-end; AC2→ §5 terminal confirmation + validation; AC3→
§3 version-conflict reload; AC4→ approach-(c) availability + backend `403`; AC5→
§8 (config-only) + no identity in body; AC6→ §9 tests + build.

## 11. Gate

This Implementation Design + the Q-017 Architecture + ADR-019 form the §16.5-B
bundle at the Q-017 implementation-authorization gate. Live status: Q-017
Requirement §17. No implementation begins until the Product Owner accepts the
bundle; then Codex implements and Claude Code independently reviews (including the
live Playwright lifecycle slice).
