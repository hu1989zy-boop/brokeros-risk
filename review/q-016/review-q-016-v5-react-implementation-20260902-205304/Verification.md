# Q-016 React Verification

## Gate Decision

**PASS WITH CONDITIONS** — all executable Node/build/static/browser-smoke
checks pass; the one live Keycloak/backend Playwright test is skipped for
missing external inputs.

## Tool availability

```text
node --version        -> v26.5.0
npm --version         -> 11.13.0
npx --version         -> 11.13.0
uname -m              -> arm64
```

Pinned direct packages were checked with `npm view` and resolved with:

```text
cd frontend
npm install --ignore-scripts --no-audit --no-fund
```

Result: **PASS — 309 packages installed; `package-lock.json` generated.** No
install script was trusted implicitly. Playwright browser installation was run
explicitly later.

## Final TypeScript and unit/component/contract verification

```text
cd frontend
npm run typecheck
npm test -- --reporter=verbose
```

Final result: **PASS**.

- TypeScript: strict `tsc --noEmit`, 0 errors.
- Vitest: **7 files passed; 27 tests passed; 0 failed; 0 skipped; 0 unhandled
  errors**.
- Runtime: Node/jsdom; no external browser required.
- Node 26 printed a non-failing experimental warning that its process-level
  localStorage has no file; application tests use jsdom sessionStorage and a
  private memory OIDC user store.

### Corrected test failures (not relabelled as initial passes)

1. The first sandboxed `npm test` could not create
   `frontend/node_modules/.vite-temp` (`EPERM`). Re-running the identical
   authorized command with the required workspace permission executed tests.
2. The first executable suite: **16 passed, 2 failed**. It exposed refreshed
   Bearer header overwrite on the one-time 401 replay and jsdom modal exit
   lifecycle timing. Both were corrected; the then-current suite passed 20/20.
3. Expanded configuration coverage then passed 23/23.
4. The added note-state matrix first ran **26 passed, 1 failed, 1 unhandled
   rejection**. It exposed uncaught Ant Form validation rejection; the code now
   recognizes field-validation failure and disables input/actions while pending.
5. The final expanded run passed **27/27** with no unhandled errors.

No test was deleted, weakened, or converted to a pass-only stub to obtain the
final result.

## Production build

```text
cd frontend
npm run build
```

Result: **PASS** — strict typecheck plus Vite 7.3.6 production bundle; 1,566
modules transformed. Final chunks included lazy detail (50.25 kB), shared
format/UI (80.63 kB), lazy list (132.77 kB), and base (766.04 kB); sizes are
pre-gzip. Vite emitted its non-blocking >500 kB base-chunk warning. The warning
is recorded in OutstandingItems; it was not hidden with a larger threshold.

## Dependency advisory and resolved-tree checks

```text
cd frontend
npm audit --audit-level=high
npm ls --depth=0
```

Result: **PASS** — 0 vulnerabilities; all 21 direct runtime/development
packages resolved at their pinned versions with no missing/invalid entry.

## Playwright and browser evidence

```text
cd frontend
npx playwright install chromium
```

Result: **PASS** — Chrome for Testing 151.0.7922.34, headless shell, and FFmpeg
downloaded for macOS arm64.

```text
node --input-type=module -e "<launch Chromium; render local heading; close>"
```

Result: **PASS** — a real headless Chromium process launched, rendered
`Q-016 browser smoke`, and closed.

```text
npm run test:e2e -- --list
npm run test:e2e
```

Result: spec discovery **PASS — 1 test in 1 file**. Execution: **1 skipped**.
The spec requires `E2E_OPERATOR_PASSWORD` and `E2E_CASE_NUMBER`; the root
`.env` was absent and no existing case input was supplied. This is **not** a
live E2E PASS. Trace, screenshot, and video recording are disabled by config.

## Live frontend runtime smoke

```text
cd frontend
npm run dev -- --host 127.0.0.1
node --input-type=module -e "<launch Chromium; GET http://localhost:4173/login; assert heading/button and browser errors; close>"
```

Final result: **PASS — HTTP 200, Risk Console heading/sign-in button visible,
0 console/page errors**. The Vite process was stopped with Ctrl-C afterward.

## Live dev-stack availability

Read-only preflight found the repository-root `.env` absent. `docker version`
reported client 29.7.2, while the sandboxed server connection was denied;
Compose rendering also rejected the intentionally missing required MySQL and
Keycloak variables. Because the required external values were unavailable, no
container was started and no fake value/case was invented. The live Keycloak →
backend → MySQL browser slice remains the stated acceptance condition.

## Repository/static/backend-scope checks

```text
cd <repository-root>
git diff --check
sh scripts/verify-static.sh
sh -n scripts/run-risk-console-dev.sh
test -z "$(git diff --name-only -- backend/)"
```

Final result: **PASS** — static verification printed `Static verification
PASS`; launcher syntax passed; backend task diff is empty.

Two invocation diagnostics preceded the final command: direct execution of the
non-executable `scripts/verify-static.sh` returned permission denied, and one
later `sh scripts/verify-static.sh` was mistakenly invoked from `frontend/`
where that relative path does not exist. The script was then run correctly via
`sh` from repository root and passed. These invocation errors are not reported
as product passes.

A credential scan was initially invoked with a pattern beginning `-` and `rg`
treated it as an option. The corrected command used `--`; it returned no private
key or JWT-shaped literal. Source scan also found no `dangerouslySetInnerHTML`,
`eval`, application console logging, implicit/password grant, or localStorage
use.

The first combined pre-archive scan also placed `-g` filters after search paths,
so `rg` treated them as files and printed errors; its surrounding shell command
was therefore not accepted as evidence despite a misleading final echo. The
scan was re-issued with filters before `--`, together with explicit required-file
and symlink checks, and returned `Review package completeness/symlink/credential
scan PASS` with no match.

During the final pre-archive repetition, a broad credential expression matched
the documented placeholder `E2E_OPERATOR_PASSWORD='<local-dev-password>'` in
`frontend/README.md`; this is not a credential and that broad result was not
accepted as a clean scan. A subsequent zsh command passed a whitespace-delimited
path list as one filename, and another attempt named the runtime configuration
as `frontend/runtime-config.js` instead of
`frontend/public/runtime-config.js`; neither attempt was accepted as evidence.
The final command used Bash plus explicit existing source/configuration paths and
returned `credential-scan: PASS (explicit source/config paths; no matches)`.

## Backend verification boundary

No backend compile/test was rerun. The governing prompt says to reuse the
delivered endpoint/gate and confirm it was untouched. Both
`git diff --stat -- backend/` and `git diff --name-only -- backend/` are empty;
no Flyway migration is present in the task diff.

## Cleanup and Git actions

- Vite dev server: stopped.
- Docker containers: none started by this task, so none required cleanup.
- Removed: stale Flutter build cache and generated Vite dist/Playwright report/
  test-result directories. These were generated/ignored and are reproducible.
- Retained: ignored `frontend/node_modules` for local review reproducibility;
  installed Playwright browser cache in the user's standard tool cache.
- Git staging: none. Commit: none. Push: none.
