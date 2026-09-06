# Q-015 Phase A Verification

## Environment and test boundary

- Checkout: full repository at
  `/Users/lukeh/Documents/workspace/codex/brokeros-risk`.
- Baseline: `a783e48` on `main`; nothing staged or committed.
- Git: 2.39.5 (Apple Git-154).
- Docker: 29.7.2.
- Final full gate runtime: `maven:3.9.9-eclipse-temurin-21-alpine`, Java 21.
- Focused host runtime: IntelliJ bundled Maven 3.9.9 with JetBrains Runtime 21.0.5.
- Database: disposable `mysql:8.4`, synthetic database/user only; no host volume.
- Kafka: no broker was available/required by the cleared prompt. The adapter was
  tested with a narrow sender future test double; no embedded Kafka or Mockito was
  used for Q-015 tests.
- Frontend gates were not run because no frontend file changed and the prompt says
  to run them only if frontend is touched.

Synthetic disposable passwords are redacted below and are not retained in this
package.

## Successful commands

### Focused Q-015 and Flyway contract selection

```text
cd backend
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress \
  -Dtest='TradingDataDomainTests,TradingDataApplicationTests,TradingDataRestContractTests,KafkaTradingDataEventPublisherTests,TradingDataMetricsTests,TradingDataArchitectureTests,TradingDataBootstrapContractTests,FlywayMigrationTests' test
```

Final result: **PASS — 24 tests, 0 failures, 0 errors, 0 skipped**. This selection
contains all 19 non-database Q-015 tests and the five shared Flyway contract tests,
including the new V9 assertion.

### Final full repository backend gate

The disposable MySQL service was prepared with one synthetic schema/user and
`log_bin_trust_function_creators=1` for existing trigger-based failure-injection
tests. The final test ran inside a Java 21 Maven image with the **full repository
mounted**, not a copied backend subtree:

```text
docker run --rm --link brokeros-q015-mysql:mysql \
  -v /Users/lukeh/Documents/workspace/codex/brokeros-risk:/workspace \
  -v brokeros-q015-m2:/root/.m2 \
  -w /workspace/backend \
  -e Q008_MYSQL_TEST_URL='<disposable-mysql-url>' \
  -e Q008_MYSQL_TEST_USERNAME='<synthetic-user>' \
  -e Q008_MYSQL_TEST_PASSWORD='<redacted>' \
  -e Q009_MYSQL_TEST_URL='<same-url>' -e Q009_MYSQL_TEST_USERNAME='<same-user>' -e Q009_MYSQL_TEST_PASSWORD='<redacted>' \
  -e Q010_MYSQL_TEST_URL='<same-url>' -e Q010_MYSQL_TEST_USERNAME='<same-user>' -e Q010_MYSQL_TEST_PASSWORD='<redacted>' \
  -e Q011_MYSQL_TEST_URL='<same-url>' -e Q011_MYSQL_TEST_USERNAME='<same-user>' -e Q011_MYSQL_TEST_PASSWORD='<redacted>' \
  -e Q012_MYSQL_TEST_URL='<same-url>' -e Q012_MYSQL_TEST_USERNAME='<same-user>' -e Q012_MYSQL_TEST_PASSWORD='<redacted>' \
  -e Q013_MYSQL_TEST_URL='<same-url>' -e Q013_MYSQL_TEST_USERNAME='<same-user>' -e Q013_MYSQL_TEST_PASSWORD='<redacted>' \
  -e Q014_MYSQL_TEST_URL='<same-url>' -e Q014_MYSQL_TEST_USERNAME='<same-user>' -e Q014_MYSQL_TEST_PASSWORD='<redacted>' \
  -e Q015_MYSQL_TEST_URL='<same-url>' -e Q015_MYSQL_TEST_USERNAME='<same-user>' -e Q015_MYSQL_TEST_PASSWORD='<redacted>' \
  maven:3.9.9-eclipse-temurin-21-alpine \
  mvn --batch-mode --no-transfer-progress test
```

Final-state result: **BUILD SUCCESS — 342 tests, 0 failures, 0 errors, 0 skipped**
in 1:03. `Q015MySqlTests` contributed 5/5 passing real-MySQL tests. Every enabled
Q-008 through Q-015 MySQL suite used the disposable schema and Flyway V1-to-V9.

The Q-015 real-MySQL coverage verified:

- clean V1-to-V9 and dynamic V8-to-V9 pending count, validation, and no-op rerun;
- exactly two new empty trading-data tables;
- explicit non-partitioned V1 fallback plus global unique key;
- exact arbitrary-byte `BLOB` round trip and metadata history query;
- the account/time index chosen by `EXPLAIN`;
- duplicate endpoint success/no republish and durable gap marker;
- database checks for version, platform, sequence/payload constraints;
- event and gap rollback when publication fails.

### Static and source-boundary checks

```text
bash scripts/verify-static.sh
git diff --check
git diff --exit-code -- deploy/keycloak/q016-security-bootstrap.json
find backend/src/main/java/com/brokeros/risk/tradingdata -type d -name gateway -print
rg -n -i 'manager api|native adapter|native field|order ticket|deal ticket|position ticket|payload\.toString|logger\.(info|debug).*payload' \
  backend/src/main/java/com/brokeros/risk/tradingdata
jq empty deploy/keycloak/q015-ingestion-bootstrap.json
```

Results:

- Static migration and whitespace gate: **PASS** (`Static verification PASS`).
- `git diff --check`: **PASS**, no output.
- Operator bootstrap diff: **PASS**, no output; it is untouched.
- Gateway/native/SDK/canonical-field/payload-log scans: **PASS**, no output.
- Q-015 bootstrap JSON parse: **PASS**.
- Source inspection confirmed no Q-008 through Q-014 module or schema change.

## Kafka test approach

`KafkaTradingDataEventPublisherTests` injects a package-local `KafkaSender` backed by
bounded `CompletableFuture` results. It asserts the exact topic, exact
`tradingAccountId` key, version/metadata JSON, Base64-preserved arbitrary bytes,
timeout→`TRADING_DATA_BACKPRESSURE`, and failed-send→authority-unavailable mapping.
It does not claim broker connectivity, ACL, partition count, replication, or
end-to-end Kafka durability.

## Intermediate failures and resolutions

- The first focused run used Mockito-based doubles and failed because the sandboxed
  JDK could not self-attach its agent (18 tests: 2 failures, 6 errors). Q-015 tests
  were refactored to hand-written, narrow doubles; the final focused and full gates
  passed.
- The first full Docker run compiled 341 tests but failed with 1 failure and 37
  errors. Q-015's Kafka publisher had two constructors without an explicit Spring
  choice, and existing Q-010 through Q-014 trigger fixtures were blocked by MySQL
  binary-log policy. `@Autowired` now marks the production constructor; the
  disposable container alone received `log_bin_trust_function_creators=1`.
- An attempted `maven:3.9.11-eclipse-temurin-21-alpine` image was unavailable and
  its credential-helper lookup was interrupted. The locally available
  `maven:3.9.9-eclipse-temurin-21-alpine` image ran the final gate.
- Static verification first found two trailing-space lines in the A5 note, then
  exposed its pre-Q-015 hard-coded migration total of eight. The whitespace was
  removed and the gate was updated to nine migrations plus V9-specific additive,
  two-table, SDK-independent checks. The final static run passed.
- A focused compile after converting required numeric request fields to nullable
  wrapper types found one test literal typed as `int` instead of `long`; the test
  fixture was corrected to `1L`, and the final focused/full gates passed.

## Warnings

- Flyway warned that MySQL 8.4 is newer than its tested ceiling of 8.1. All V1-to-V9
  migration and persistence tests passed; this remains a dependency compatibility
  advisory.
- Docker warned that default-bridge `--link` is deprecated. It was used only by the
  disposable verification harness.
- Existing non-Q-015 Mockito tests warned about future JDK dynamic-agent loading.
  Q-015 tests themselves no longer depend on runtime self-attachment.

No check is reported as passed unless its final command completed successfully.

## Disposable environment cleanup

```text
docker rm -f brokeros-q015-mysql
docker volume rm brokeros-q015-m2
```

Result: **PASS**. The task-created container, tmpfs/container-layer synthetic
database, and Maven cache volume were removed. That disposable data is intentionally
not recoverable; no repository file was deleted.
