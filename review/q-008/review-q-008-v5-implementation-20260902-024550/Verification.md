# Q-008 Verification Evidence

## Environment

- Host: macOS 15.7.7, Apple Silicon (`aarch64`).
- Build runtime: JetBrains JBR Java 21.0.5.
- Maven: IntelliJ-bundled Apache Maven 3.9.9.
- Required database: disposable Docker `mysql:8.4`, server reported MySQL
  8.4.11, exposed only for tests on host port 33318.
- Test credentials were ephemeral and are intentionally redacted from this
  package. No mandatory MySQL test was skipped.
- MySQL binary logging was left enabled. The disposable container alone was
  configured with `log_bin_trust_function_creators=1` so the non-privileged
  test user could create test-only failure-injection triggers.

## Passing verification

### Q-008 non-MySQL suite

Command, from `backend/`:

```text
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress \
-Dtest='RiskCaseDomainTests,RiskCaseApplicationTests,RiskCaseArchitectureTests,RiskCaseRestContractTests,RiskCaseMetricsTests,RiskCaseReferenceAdapterTests' test
```

Result: **PASS — 35 tests, 0 failures, 0 errors, 0 skipped**.

### Q-008 real-MySQL migration and persistence suite

Command, from `backend/` (credential values redacted):

```text
Q008_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33318/brokeros_q008_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q008_MYSQL_TEST_USERNAME='<ephemeral-test-user>' \
Q008_MYSQL_TEST_PASSWORD='<redacted>' \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress \
-Dtest='Q008MySqlMigrationTests,Q008MySqlPersistenceTests' test
```

Result: **PASS — 19 tests, 0 failures, 0 errors, 0 skipped**.

- `Q008MySqlMigrationTests`: 7/7 PASS.
- `Q008MySqlPersistenceTests`: 12/12 PASS.
- Flyway warning: bundled Flyway support is reported as tested through MySQL
  8.1; actual MySQL 8.4.11 migration and persistence behavior passed.

### Full repository-wide Q-009 through Q-014 plus Q-008 MySQL gate

Command, from `backend/`; the same disposable URL/user was exported under each
module's exact variable names and password values are redacted:

```text
Q008_MYSQL_TEST_URL='<jdbc-mysql-33318-url>' Q008_MYSQL_TEST_USERNAME='<ephemeral-test-user>' Q008_MYSQL_TEST_PASSWORD='<redacted>' \
Q009_MYSQL_TEST_URL='<jdbc-mysql-33318-url>' Q009_MYSQL_TEST_USERNAME='<ephemeral-test-user>' Q009_MYSQL_TEST_PASSWORD='<redacted>' \
Q010_MYSQL_TEST_URL='<jdbc-mysql-33318-url>' Q010_MYSQL_TEST_USERNAME='<ephemeral-test-user>' Q010_MYSQL_TEST_PASSWORD='<redacted>' \
Q011_MYSQL_TEST_URL='<jdbc-mysql-33318-url>' Q011_MYSQL_TEST_USERNAME='<ephemeral-test-user>' Q011_MYSQL_TEST_PASSWORD='<redacted>' \
Q012_MYSQL_TEST_URL='<jdbc-mysql-33318-url>' Q012_MYSQL_TEST_USERNAME='<ephemeral-test-user>' Q012_MYSQL_TEST_PASSWORD='<redacted>' \
Q013_MYSQL_TEST_URL='<jdbc-mysql-33318-url>' Q013_MYSQL_TEST_USERNAME='<ephemeral-test-user>' Q013_MYSQL_TEST_PASSWORD='<redacted>' \
Q014_MYSQL_TEST_URL='<jdbc-mysql-33318-url>' Q014_MYSQL_TEST_USERNAME='<ephemeral-test-user>' Q014_MYSQL_TEST_PASSWORD='<redacted>' \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress test
```

Result: **PASS — 58 Surefire reports, 300 tests, 0 failures, 0 errors,
0 skipped**. All reports were refreshed after the command start. Completion was
captured from the persistent PTY with process exit code 0.

### Compile, package, and dependency checks

Commands, from `backend/`:

```text
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress -DskipTests test-compile

JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress -DskipTests package

JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress dependency:tree
```

Results: **PASS**. Test compilation and executable JAR packaging succeeded.
Dependency resolution succeeded; Q-008 adds no library.

### Static, deployment-render, and Git hygiene checks

Commands, from repository root:

```text
sh scripts/verify-static.sh
sh scripts/verify-kustomize.sh
git diff --check
rg -n '@DeleteMapping|RequestMethod.DELETE|setStatus\s*\(|REQUIRES_NEW|@Async|KafkaTemplate|RedisTemplate|Manager API|MT4|MT5|CRM|bridge|liquidity provider' \
  backend/src/main/java/com/brokeros/risk/riskcase \
  backend/src/main/java/com/brokeros/risk/audit
rg -n 'password|secret|token' \
  backend/src/main/java/com/brokeros/risk/riskcase \
  backend/src/main/java/com/brokeros/risk/audit
```

Results:

- Static verification: **PASS**. It asserts eight migrations, Q-008's exact
  13-table set, additive/schema-only V8, no forbidden execution/vendor terms,
  and dynamic post-baseline migration-count logic.
- Kustomize base/test/prod rendering and contract verification: **PASS**.
- Git whitespace validation: **PASS**.
- Forbidden behavior/secret scans: **PASS**; only legitimate authorization
  type/member names appeared in the broader security scan.

## Executed failures and corrections

These failures are retained rather than rewritten as passing checks:

1. Two initial Maven invocations were run from repository root, which has no
   POM, and returned `MissingProjectException`. All Maven commands were rerun
   from `backend/`.
2. The first sandbox non-MySQL run reported seven Byte Buddy attachment errors
   caused by sandbox process restrictions, plus one REST reflection assertion
   that did not account for Spring's empty `@PostMapping` value. The test-only
   assertion was corrected; the suite was rerun with the Java 21 host runtime
   and passed 35/35.
3. The first sandbox MySQL run reported 13 `SocketException: Operation not
   permitted` errors because sandbox loopback access was denied. The same
   mandatory tests were rerun with approved host access; none was skipped.
4. The first host persistence run had three errors because MySQL binary logging
   disallowed test-user trigger creation. The disposable container setting
   `log_bin_trust_function_creators=1` was enabled without granting elevated
   application privileges; the suite then passed 12/12.
5. Two full-suite tool calls yielded only partial output at the tool lifecycle
   boundary and were not counted as passes. The suite was rerun in a persistent
   PTY and completed with exit code 0; all 58 report timestamps refreshed.
6. Direct execution `./scripts/verify-static.sh` returned permission denied
   because the pre-existing script is not executable. The documented portable
   invocation `sh scripts/verify-static.sh` passed.

## Infrastructure verifier — genuine FAIL

First sandbox command:

```text
sh scripts/verify-infrastructure.sh
```

Result: preflight/configuration checks passed, then Docker socket access was
denied by the sandbox. Per policy, the same command was rerun with approved
Docker host access.

Host result: Docker pulled Redis/Kafka images and successfully built the
backend image, including Maven dependency resolution and package assembly.
Compose startup then failed:

```text
ports are not available: exposing port TCP 127.0.0.1:6379 ... address already in use
```

Final result: **FAIL at `compose-startup`, exit code 1**. The script attempted
failure cleanup; a subsequent Docker inventory confirmed no Q-004 project
container, volume, or network remained.

The verifier was not retried with a temporary Compose override because source
inspection found it additionally hard-codes only Flyway V1/V2/V3 and exactly
the Q-009/Q-010 seven-table schema. Those assertions cannot truthfully validate
the current V8 repository. Editing this Q-004 verifier is outside the approved
Q-008 boundary. This failure is a tooling condition, not claimed as Q-008
infrastructure PASS.

## Test environment cleanup

The failed Q-004 Compose project cleaned its isolated resources. The dedicated
Q-008 MySQL 8.4 container remained running only until final package evidence
and archive generation, after which it was stopped and removed. No persistent
test data or credentials are included in the repository or archive.
