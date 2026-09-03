# Q-017 Verification

## Gate Decision

**PASS WITH CONDITIONS** — all locally executable Node, build, static, contract,
and boundary checks pass. The live authenticated lifecycle slice is skipped for
missing environment inputs.

## Tool availability

```text
node --version           -> v26.5.0
npm --version            -> 11.13.0
npx --version            -> 11.13.0
npx playwright --version -> Version 1.62.1
```

## Dependency installation

```text
cd frontend
npm ci
```

Result: **PASS** — 309 packages installed from the committed lockfile. No
dependency version or lockfile change was made by Q-017.

## Final build and test run

```text
cd frontend
npm run build
npm test
npm run test:e2e
```

Results:

- Build: **PASS** — strict `tsc --noEmit`; Vite 7.3.6 transformed 1,572 modules
  and produced the production bundle.
- Vitest/RTL/MSW: **PASS — 9 files, 103 tests passed, 0 failed**.
- Playwright: **NOT EXECUTED LIVE — 2 tests discovered, 2 skipped**. The Q-017
  spec requires `E2E_OPERATOR_PASSWORD` plus `E2E_Q017_CASE_NUMBER` for a seeded,
  eligible OPEN case. The inherited Q-016 spec separately requires
  `E2E_CASE_NUMBER`. No external value was invented.
- Vite warning: base chunk 768.28 kB minified / 245.00 kB gzip exceeded the
  default 500 kB warning. This is non-failing and recorded as outstanding.
- Node printed a non-failing experimental warning about process localStorage;
  tests ran in jsdom and completed without an unhandled test error.

## Test-development diagnostics

These failed attempts are not relabelled as passes:

1. `npm run typecheck && npm test -- --runInBand`: typecheck passed, but Vitest
   rejected the Jest-only `--runInBand` option before tests started.
2. The first valid full `npm test` after adding the 11-operation matrix produced
   **85 passed, 9 failed**. All failures were pending-button queries because Ant
   Design prefixes the accessible name with a loading icon. The selector was
   corrected to assert the modal primary control's disabled state.
3. A later build stopped at TypeScript because a test passed an `Element` to
   `within`, which requires `HTMLElement`. The test used typed `closest` and the
   final build/test run passed.
4. An initial capability `jq` assertion targeted nonexistent `.bindings` and
   returned `false`. The corrected `.actors[0].capabilities` assertion returned
   `true` and is the accepted evidence.

No production behavior was weakened and no test was skipped or deleted to make
the Node suite pass.

## Static and scope verification

```text
cd <repository-root>
sh scripts/verify-static.sh
git diff --check
git diff --stat -- backend/
git status --short -- backend/
jq -e '.actors[0].capabilities == [
  "risk-case:assign","risk-case:cancel","risk-case:close","risk-case:note",
  "risk-case:read","risk-case:reopen","risk-case:resolve","risk-case:review"
]' deploy/keycloak/q016-security-bootstrap.json
```

Result: **PASS** — static verification printed `Static verification PASS`;
whitespace check was clean; both backend commands produced no output; the exact
capability assertion returned `true`. No Flyway path is present in Git status.

Targeted source scans found no private-key/JWT literal, `dangerouslySetInnerHTML`,
`eval`, console logging, localStorage use, JWT claim parsing, or capability probe
in the Q-017 feature scope. Test-only Bearer markers and opaque references are
synthetic and not usable credentials.

## Backend verification boundary

Backend compilation/tests were not rerun because no backend file, test, or
migration changed and the governing prompt directs Q-017 to consume the
committed endpoints unchanged. This package claims an empty backend task diff,
not a new backend validation result.

## Live execution prerequisites

To close the live condition, apply the updated development bootstrap, start the
full local stack and Vite preview, supply the password only through the process
environment, and provide a seeded OPEN case that is eligible to resolve (with a
current decision and required backend preconditions). Run `npm run test:e2e`
with tracing, screenshots, and video still disabled.

## Cleanup and Git actions

- No Docker container or Vite server was started by this task, so no live
  process cleanup was required.
- Ignored `frontend/node_modules` and reproducible `frontend/dist` output remain
  local; neither is part of Git status or the review archive.
- No file is staged. No commit or push was performed.
- The pre-existing untracked Q-016 review ZIP was not modified.
