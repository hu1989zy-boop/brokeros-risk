# Q-019 Verification

## Gate Decision

**PASS WITH CONDITIONS.** Every locally executable backend, frontend, static,
contract, and boundary check passed. AC4 is conditional because the live
credentialed/seeded Playwright flow reported one skip and was not executed.

## Tool and environment record

```text
Maven:       3.9.9
Test JDK:    JetBrains 21.0.5
Node:        v26.5.0
npm:         11.13.0
Database:    mysql:8.4 in disposable brokeros-q019-mysql-20260903
Host port:   33319
Migrations:  V1 through V8 (8 total)
```

The database URL, test username, and test password were supplied only through
environment variables. Credential values are intentionally not copied here.

## Backend compile and targeted real-MySQL gate

Compile command:

```text
cd backend
JAVA_HOME=<JetBrains-JDK-21> <Maven-3.9.9> --batch-mode --no-transfer-progress \
  -DskipTests test-compile
```

Result: **PASS**. Final full-gate compilation contains 386 main source files and
61 test source files.

Targeted command (credential values redacted by security policy):

```text
cd backend
Q008_MYSQL_TEST_URL=jdbc:mysql://127.0.0.1:33319/brokeros_q019_test?... \
Q008_MYSQL_TEST_USERNAME=<redacted> Q008_MYSQL_TEST_PASSWORD=<redacted> \
JAVA_HOME=<JetBrains-JDK-21> <Maven-3.9.9> --batch-mode --no-transfer-progress \
  -Dtest=Q019RiskCaseAssociationsMySqlTests test
```

Final result: **PASS — 4 tests, 0 failures, 0 errors, 0 skipped**. Flyway cleaned,
validated, and applied all eight existing migrations against real MySQL 8.4.

The four tests cover:

1. attached + superseded evidence events, both event refs, source and replacement;
2. two decision associations with exactly one current;
3. one effective action with the exact referenced outcome;
4. 403 before target lookup, authorized 404, and the 501-item cap failure.

## Full repository real-MySQL gate

Command shape (all values were set explicitly; credentials redacted):

```text
cd backend
for Q in Q008 Q009 Q010 Q011 Q012 Q013 Q014; do
  export ${Q}_MYSQL_TEST_URL=jdbc:mysql://127.0.0.1:33319/brokeros_q019_test?...
  export ${Q}_MYSQL_TEST_USERNAME=<redacted>
  export ${Q}_MYSQL_TEST_PASSWORD=<redacted>
done
JAVA_HOME=<JetBrains-JDK-21> <Maven-3.9.9> --batch-mode --no-transfer-progress test
```

Final result: **BUILD SUCCESS — 309 tests, 0 failures, 0 errors, 0 skipped**
across 61 Surefire XML suites. Total time: 1:51.

Flyway emitted an honest compatibility warning: MySQL 8.4 is newer than the
configured Flyway version's stated tested ceiling of MySQL 8.1. Migration
validation and all MySQL tests still passed.

## Frontend gates

```text
cd frontend
npm ci
npm run typecheck
npm test
npm run build
```

Results:

- `npm ci`: **PASS** — 309 packages installed from the committed lockfile.
- TypeScript: **PASS** — `tsc --noEmit` exited 0.
- Vitest/RTL/MSW: **PASS — 12 files, 150 tests passed, 0 failed**.
- Production build: **PASS** — typecheck reran; Vite 7.3.6 transformed 1,578
  modules and emitted the production bundle.
- Vite reported a non-failing chunk-size warning: the base chunk was 774.41 kB
  minified / 246.06 kB gzip, above the default 500 kB threshold.
- Vitest emitted Node's non-failing experimental process-localStorage warning.

Additional focused regression after the conflict-refetch assertion:

```text
npm test -- --run tests/Q018AssociationActions.test.tsx \
  tests/AssociationsPanel.test.tsx tests/riskCaseRepository.test.ts \
  tests/apiContract.test.ts
```

Result: **PASS — 4 files, 53 tests**.

## Live Playwright slice

Discovery:

```text
cd frontend
npx playwright test --list
```

Result: **PASS — 4 repository Playwright tests discovered**, including the Q-019
resolve/close flow.

Attempted Q-019 execution:

```text
cd frontend
npx playwright test tests/e2e/q019AssociationProjectionResolve.spec.ts
```

Result: **NOT EXECUTED LIVE — 1 discovered, 1 skipped, 0 passed, 0 failed**.
None of `E2E_OPERATOR_PASSWORD`, `E2E_Q019_CASE_NUMBER`,
`E2E_Q019_DECISION_REF`, or `E2E_Q019_ACTION_REF` was available. The full stack
and real referenceable seed entities therefore could not be exercised. No value
was invented and no live resolve/close success is claimed. Trace, screenshot, and
video collection are disabled by the existing Playwright configuration.

## Static and boundary verification

```text
bash scripts/verify-static.sh
git diff --check
git diff --name-only -- \
  backend/src/main/java/com/brokeros/risk/riskcase/domain \
  backend/src/main/resources/db/migration \
  backend/src/main/java/com/brokeros/risk/riskcase/application/RiskCaseAssociationService.java \
  backend/src/main/java/com/brokeros/risk/riskcase/application/RiskCaseCommandService.java \
  backend/src/main/java/com/brokeros/risk/riskcase/application/RiskCaseResolutionService.java \
  backend/src/main/java/com/brokeros/risk/riskcase/application/RiskCaseCapabilities.java
```

Results: static verification **PASS**; whitespace check **PASS**; targeted
boundary command produced no output. Direct Git inspection also found no change
to `deploy/`, dependency manifests, other provenance modules, or existing
timestamped review directories.

## Development-run diagnostics retained honestly

1. The first targeted MySQL attempt inside the filesystem/network sandbox could
   not connect to localhost and produced four errors. The exact test was rerun
   outside that network restriction against the authorized disposable container.
2. The first network-enabled targeted run reported 3 passed / 1 error because the
   501-row test fixture generated decision refs colliding with the two seeded refs.
   Only the synthetic fixture prefix was corrected; the next run passed 4/4.
3. The first full repository run reported 304 passed / 5 errors. The five existing
   Q-008/Q-011 failure-injection tests create temporary triggers; MySQL 8.4 binary
   logging rejected that operation for the limited test user. The disposable
   server was configured with `log_bin_trust_function_creators=1`; the unchanged
   full command then passed 309/309. No production setting or application code was
   changed to mask the failure.

## Cleanup and Git actions

- `docker rm --force brokeros-q019-mysql-20260903`: **PASS**; the disposable
  database container was removed and its ephemeral data is not recoverable.
- Generated `node_modules`, `dist`, Playwright report, and Maven `target` outputs
  remain ignored and are absent from the review archive.
- Staged files: none. Commit: none. Push: none.
