# Q-020 Verification

## Environment and tool availability

- Checkout: full repository at `/Users/lukeh/Documents/workspace/codex/brokeros-risk`.
- Git: 2.39.5 (Apple Git-154).
- Docker: 29.7.2; disposable `mysql:8.4` container exposed only on localhost port
  33320 with `log_bin_trust_function_creators=1` for the existing integration harness.
- Java: JetBrains Runtime 21.0.5.
- Maven: IntelliJ bundled Maven 3.9.9.
- Node: 26.5.0.
- npm: 11.13.0.
- Backend tests were run from the full checkout on the host, with the disposable
  Docker container supplying real MySQL. This preserves full-repository visibility
  for ArchUnit and configuration tests.

Local database passwords are redacted below. They were synthetic, limited to the
disposable test database, and are not retained in this package.

## Successful commands

### Backend compilation

```text
cd backend
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress -DskipTests test-compile
```

Result: PASS. Main and test sources compiled.

### Focused Q-020 real-MySQL gate

```text
cd backend
Q011_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33320/brokeros_q020_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q011_MYSQL_TEST_USERNAME='q020_test' \
Q011_MYSQL_TEST_PASSWORD='<redacted-disposable-password>' \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress \
  -Dtest='Q020EvidenceReferenceListMySqlTests,Q020DecisionReferenceListMySqlTests,Q020ActionReferenceListMySqlTests,Q020ActionOutcomeReferenceListMySqlTests' test
```

Result: PASS — 4 tests, 0 failures, 0 errors, 0 skipped. Each module migrated a
clean real-MySQL schema through V8, seeded 202 scoped records with timestamp ties,
and verified:

- 200-row cap and `recorded_at DESC, id DESC` order;
- refs, scope, ISO-8601 `recordedAt`, and optional status metadata;
- content property absent;
- valid unknown scope returns HTTP 200 with `items: []`;
- malformed scope returns the module request-invalid code;
- denied existing `READ` capability returns `AUTHORIZATION_DENIED` and records the
  existing denial metric;
- list requests create zero full-detail access-log rows;
- V4-V7 natural-scope index exists.

### Full backend repository gate

```text
cd backend
Q008_MYSQL_TEST_URL='<same-disposable-MySQL-URL>' Q008_MYSQL_TEST_USERNAME='q020_test' Q008_MYSQL_TEST_PASSWORD='<redacted>' \
Q009_MYSQL_TEST_URL='<same-disposable-MySQL-URL>' Q009_MYSQL_TEST_USERNAME='q020_test' Q009_MYSQL_TEST_PASSWORD='<redacted>' \
Q010_MYSQL_TEST_URL='<same-disposable-MySQL-URL>' Q010_MYSQL_TEST_USERNAME='q020_test' Q010_MYSQL_TEST_PASSWORD='<redacted>' \
Q011_MYSQL_TEST_URL='<same-disposable-MySQL-URL>' Q011_MYSQL_TEST_USERNAME='q020_test' Q011_MYSQL_TEST_PASSWORD='<redacted>' \
Q012_MYSQL_TEST_URL='<same-disposable-MySQL-URL>' Q012_MYSQL_TEST_USERNAME='q020_test' Q012_MYSQL_TEST_PASSWORD='<redacted>' \
Q013_MYSQL_TEST_URL='<same-disposable-MySQL-URL>' Q013_MYSQL_TEST_USERNAME='q020_test' Q013_MYSQL_TEST_PASSWORD='<redacted>' \
Q014_MYSQL_TEST_URL='<same-disposable-MySQL-URL>' Q014_MYSQL_TEST_USERNAME='q020_test' Q014_MYSQL_TEST_PASSWORD='<redacted>' \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress test
```

Final-state result: PASS — 317 tests, 0 failures, 0 errors, 0 skipped; Maven build
success in 45.754 seconds.

### Frontend

```text
cd frontend
npm ci
npm run typecheck
npm test -- ReferenceInput.test.tsx referenceListRepository.test.ts AssociationsPanel.test.tsx Q018AssociationActions.test.tsx
npm test
npm run build
```

Results:

- `npm ci`: PASS; clean installation completed (309 packages).
- `npm run typecheck`: PASS; no TypeScript errors.
- Focused suite: PASS — 4 files, 52 tests.
- Full suite: PASS — 13 files, 156 tests.
- Production build: PASS — typecheck plus Vite 7.3.6; 1,581 modules transformed.
  Vite reported a non-failing advisory for a minified chunk over 500 kB.

### Static and source-boundary checks

```text
bash scripts/verify-static.sh
git diff --check
git diff --name-only -- backend/src/main/java/com/brokeros/risk/evidence/domain backend/src/main/java/com/brokeros/risk/decision/domain backend/src/main/java/com/brokeros/risk/action/domain backend/src/main/java/com/brokeros/risk/actionoutcome/domain backend/src/main/java/com/brokeros/risk/riskcase/domain
find backend/src/main/java/com/brokeros/risk/{evidence,decision,action,actionoutcome,riskcase}/domain -type f -newermt '2026-09-05 20:00:00' -print
git diff --name-only -- backend/src/main/resources/db/migration
find backend/src/main/resources/db/migration -type f -newermt '2026-09-05 20:00:00' -print
git diff --name-only | rg 'Capabilities\.java|RecordingService\.java|CorrectionService\.java|actionDescriptors\.ts|useCaseAction\.ts|riskCaseRepository\.ts' || true
git ls-files --others --exclude-standard | rg 'Capabilities\.java|RecordingService\.java|CorrectionService\.java|db/migration|actionDescriptors\.ts|useCaseAction\.ts|riskCaseRepository\.ts' || true
```

Results:

- Static verification: PASS.
- Whitespace/error check: PASS.
- Aggregate/domain boundary: PASS; no tracked or untracked output.
- Migration boundary: PASS; no tracked or untracked output; migration inventory
  remains V1-V8.
- Capability/write/association-contract boundary: PASS; no tracked or untracked
  output.

## Intermediate failures and resolutions

- The first backend test compilation failed because existing explicit query-port
  test doubles did not yet implement the additive list methods. The stubs were
  updated; compilation and all tests then passed.
- The first focused frontend run had one assertion using a stale label. The test was
  corrected to the actual accessible label; the focused and full suites passed.
- One local-database rerun attempted inside the restricted sandbox and ended with
  4 connection errors (`Operation not permitted`). The identical bounded test was
  rerun with approved localhost access.
- The first strengthened timestamp-shape run reached MySQL but failed 4 assertions
  because standalone MockMvc's default mapper emitted numeric `Instant` values.
  The test harness was aligned with Spring Boot's ISO Java-time mapper; the same
  four tests then passed. No production code was changed for this harness issue.

## Warnings

- Flyway warned that MySQL 8.4 is newer than its tested support ceiling of 8.1.
  All migrations and tests passed; this is an existing dependency-compatibility
  advisory.
- Mockito warned about future JDK restrictions on dynamic agent loading.
- Vitest workers emitted Node's experimental localStorage warning.
- Vite emitted the existing large-chunk advisory.

No check was reported as passed unless its final command completed successfully.

## Disposable environment cleanup

```text
docker rm --force brokeros-q020-mysql-20260905
```

Result: PASS. The named disposable container and its tmpfs-only synthetic database
were removed after verification; that ephemeral test data is intentionally not
recoverable. No repository file was deleted.
