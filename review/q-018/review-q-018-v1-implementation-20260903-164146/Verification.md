# Q-018 Verification

## Gate Decision

**BLOCKED** — all locally executable Node, static, build, contract, and scope
checks pass. The live slice is skipped, and direct inspection proves that three
approved reference prefixes and required association projection fields do not
match/exist in the committed backend.

## Tool availability

```text
node --version                         -> v26.5.0
npm --version                          -> 11.13.0
frontend/node_modules/.bin/playwright  -> Version 1.62.1
```

## Dependency installation

```text
cd frontend
npm ci
```

Result: **PASS** — 309 packages installed from the committed lockfile in about
5 seconds. No dependency manifest or lockfile changed.

## Final typecheck, tests, and build

```text
cd frontend
npm run typecheck
npm test
npm run build
```

Results:

- Typecheck: **PASS** — `tsc --noEmit` exited 0.
- Vitest/RTL/MSW: **PASS — 12 files, 148 tests passed, 0 failed**.
- Build: **PASS** — build reran typecheck; Vite 7.3.6 transformed 1,578 modules
  and emitted the production bundle.
- Vite warning: base chunk 772.91 kB minified / 245.82 kB gzip exceeds the
  default 500 kB warning. The build still exited 0; the warning is not hidden.
- Node emitted a non-failing experimental process-localStorage warning during
  Vitest. Application Q-018 code does not use localStorage.

## Live Playwright

```text
cd frontend
npm run test:e2e -- q018AssociationLifecycle.spec.ts
```

Result: **NOT EXECUTED LIVE — 1 test discovered, 1 skipped, 0 passed, 0 failed**.
No `E2E_OPERATOR_PASSWORD`, `E2E_Q018_CASE_NUMBER`,
`E2E_Q018_DECISION_REF`, or `E2E_Q018_ACTION_REF` was supplied. No credential or
seed reference was invented. No browser trace, screenshot, or video was created.

Even with live inputs, current backend source modules accept `dec-/act-/aoc-`
while the approved Q-018 client requires `dc-/ac-/ao-`; the governance contract
must be reconciled before this exact live flow can pass.

## Test-development diagnostics

Failed development runs are retained rather than relabelled:

1. An early `npm run typecheck` found that a generic select field's options could
   be undefined. The render path was made null-safe; the next typecheck passed.
2. After the first Q-018 test addition, `npm test -- --run` executed 147 tests and
   reported **142 passed, 5 failed**. All five failures were inherited Q-017
   `resolve` cases: the new Q-018 `ac-` action regex had inadvertently replaced
   Q-017's established `act-` resolution regex. Separate validators restored the
   Q-017 contract.
3. The targeted regression rerun passed **125/125** across six affected files.
4. The final required exact `npm test` passed **148/148** after adding the panel
   action-hosting assertion.

No test was deleted, weakened, or skipped to obtain the final Node result.

## Static, capability, and scope verification

```text
cd <repository-root>
bash scripts/verify-static.sh
git diff --check
git diff --stat -- backend/
git status --short -- backend/src/main/java backend/src/main/resources/db/migration
jq -e '.actors[0].capabilities |
  (index("risk-case:associate") != null and
   index("evidence:read") != null and
   index("decision:read") != null and
   index("action:read") != null and
   index("action-outcome:read") != null)' \
  deploy/keycloak/q016-security-bootstrap.json
```

Result: **PASS** — static verification printed `Static verification PASS`;
whitespace check was clean; both backend/Flyway checks produced no output; the
capability expression returned `true`.

Source inspection found no `dangerouslySetInnerHTML`, dynamic evaluation, JWT
claim/capability parsing, console logging, or new browser persistence in Q-018.
A broad credential-pattern scan matched one pre-existing synthetic test-only
Bearer marker; it is not a usable credential and was not introduced by Q-018.

## Direct backend contract inspection

Read-only inspection of existing source (no backend edit) confirmed:

```text
EvidenceRef.java       -> startsWith("ev-")
DecisionRef.java       -> startsWith("dec-")
ActionRef.java         -> startsWith("act-")
ActionOutcomeRef.java  -> startsWith("aoc-")
```

`RiskCaseHistoryEntryResponse` exposes only version, eventType, affectedRef,
actorRef, and occurredAt. The SQL history union maps evidence disposition to
`evidence_ref` and action outcome to `action_ref`; it does not expose evidence
event ID/replacement or action outcome ref. These are static facts, not live-test
claims.

## Backend verification boundary

Backend compilation/tests were not rerun because no backend code, test,
migration, or resource changed and the prompt explicitly restricts Q-018 to
consuming existing endpoints. This package claims an empty backend task diff,
not a new backend test result.

## Cleanup and Git actions

- No Docker container, Vite server, or live browser was started; no runtime
  cleanup was required.
- `node_modules` and reproducible `dist` output are ignored and absent from Git
  status/review archive.
- Staged files: none. Commit: none. Push: none.
- The pre-existing Q-016 and Q-017 untracked review ZIPs were not modified.
