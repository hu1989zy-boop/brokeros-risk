# Q-016 React Outstanding Items

## Blocking acceptance condition

1. Supply repository-root ignored `.env` values and an existing bounded-list
   Risk Case number, start the dev profile, and execute:
   `E2E_OPERATOR_PASSWORD=<external local value> E2E_CASE_NUMBER=<existing case> npm run test:e2e`.
   Verify live Keycloak login, list paging, detail/history, add note, backend JWT
   enforcement, and logout. The value must remain outside source, shell history,
   reports, screenshots, traces, video, and review artifacts.

This one missing live execution keeps AC 2, AC 6, and AC 7 from PASS. Chromium
availability is not a blocker: Playwright downloaded Chrome for Testing 151 and
launched it successfully on arm64.

## Assumptions and deliberate choices

- Add investigation note is the one complete operation authorized by the
  Foundation. No assignment or broader lifecycle UI was added.
- List page size is fixed at 20; backend cap 100 remains authoritative. History
  requests use the existing maximum 100 and disclose when `nextCursor` exists.
- `RiskCaseDetailResponse` directly exposes only `currentDecisionRef`; the
  unchanged backend has no complete current evidence/action-association read
  projection. The UI therefore labels other affected refs as "Association
  references in history" and does not pretend they are complete current state.
- Access/user state is memory-only. A page reload may require sign-in again;
  this is accepted for the security-first foundation.
- Frontend runtime config contains non-secret endpoints/public client identity.
  Dev port 4173 intentionally reuses the existing exact Keycloak/CORS contract.
- Backend tests were not rerun because this task made zero backend change and
  the governing prompt says to confirm, not re-run, the delivered backend gate.

## Non-blocking technical observations

- The production build passes but Vite warns that the lazy-loaded base chunk is
  766.04 kB (244.46 kB gzip). No performance target exists for Q-016. Measure
  real load behavior before introducing manual vendor chunk policy.
- Node 26.5 emits an experimental localStorage warning from the jsdom workers;
  27 tests still pass with no unhandled errors. The application OIDC test proves
  its user store is memory-only.
- The live E2E appends an immutable investigation note to the selected local-dev
  case. Use disposable/local data appropriate for that operation.

## Independent review handoff

Claude Code should review Requirement → ADR/addendum → implementation → runtime
evidence independently. This Codex package does not self-approve Q-016. Product
Owner acceptance and Git commit/push remain future gates.
