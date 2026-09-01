# Q-013 Verification

## Environment

- Host: macOS 15.7.7, aarch64
- Java: JetBrains Runtime OpenJDK 21.0.5
- Maven: 3.9.9
- Docker client/server: 29.7.2
- Database: disposable mysql:8.4.11
- MySQL log_bin_trust_function_creators: 1, required only by test failure
  triggers
- Flyway: 11.7.2
- Time zone: Asia/Kuala_Lumpur
- Disposable credentials: redacted and not written to repository files

Flyway emitted its standard warning that MySQL 8.4 is newer than its latest
explicitly tested version, MySQL 8.1. Actual migration and persistence behavior
was exercised on MySQL 8.4.11.

## Compilation

Command:

    JAVA_HOME=<JBR21> mvn --batch-mode --no-transfer-progress       -DskipTests test-compile

Result: PASS. Main and test sources compiled with Java release 21.

## Q-013 non-database tests

Command:

    JAVA_HOME=<JBR21> mvn --batch-mode --no-transfer-progress       -Dtest='ActionDomainTests,ActionApplicationTests,ActionArchitectureTests,ActionMetricsTests,ActionRestContractTests'       test

Result: PASS.

- Tests run: 22
- Failures: 0
- Errors: 0
- Skipped: 0

## Q-013 real MySQL 8.4.11 tests

Mandatory environment variables:

    Q013_MYSQL_TEST_URL=<redacted-loopback-MySQL-URL>
    Q013_MYSQL_TEST_USERNAME=<redacted>
    Q013_MYSQL_TEST_PASSWORD=<redacted>

Command:

    JAVA_HOME=<JBR21> mvn --batch-mode --no-transfer-progress       -Dtest='Q013MySqlMigrationTests,Q013MySqlPersistenceTests,Q013SecurityMySqlIntegrationTests'       test

Initial restricted-sandbox result: environmental failure.

- Tests run: 17
- Failures: 0
- Errors: 17
- Skipped: 0
- Root cause: java.net.SocketException: Operation not permitted while opening
  the loopback MySQL connection.
- Disposition: no code change; reran the identical tests in the approved host
  context against the same disposable container.

Approved host-context result: PASS.

- Tests run: 17
- Failures: 0
- Errors: 0
- Skipped: 0
- Q013SecurityMySqlIntegrationTests: 5 passed
- Q013MySqlPersistenceTests: 6 passed
- Q013MySqlMigrationTests: 6 passed

The suite proved V5-to-V6 upgrade, dynamic pending count, exactly three Action
tables, zero seeds, Flyway validate/restart, all Design §8.4 constraints,
foreign-key restrictions, indexed plans, durable replay, atomic rollback,
three-attempt ActionRef collision bound, one-commit/one-replay concurrency,
strict UTF-8 failure, audit-failure isolation, and real Q-009/Q-012 grants.

## Mandatory repository-wide Q009-Q013 gate

All five datasource families pointed to the same disposable MySQL 8.4.11
schema; each test suite cleans and remigrates its own fixture.

Command:

    Q009_MYSQL_TEST_URL=<redacted> Q009_MYSQL_TEST_USERNAME=<redacted> Q009_MYSQL_TEST_PASSWORD=<redacted>     Q010_MYSQL_TEST_URL=<redacted> Q010_MYSQL_TEST_USERNAME=<redacted> Q010_MYSQL_TEST_PASSWORD=<redacted>     Q011_MYSQL_TEST_URL=<redacted> Q011_MYSQL_TEST_USERNAME=<redacted> Q011_MYSQL_TEST_PASSWORD=<redacted>     Q012_MYSQL_TEST_URL=<redacted> Q012_MYSQL_TEST_USERNAME=<redacted> Q012_MYSQL_TEST_PASSWORD=<redacted>     Q013_MYSQL_TEST_URL=<redacted> Q013_MYSQL_TEST_USERNAME=<redacted> Q013_MYSQL_TEST_PASSWORD=<redacted>     JAVA_HOME=<JBR21> mvn --batch-mode --no-transfer-progress test

Result: FAIL.

- Tests run: 204
- Failures: 1
- Errors: 0
- Skipped: 0
- Q-013 tests within the same run: 39 passed
- Sole failure:
  Q012MySqlMigrationTests.migrationUpgradesV4CreatesExactlyFourTablesWithoutSeedsAndValidatesOnRestart
- Assertion: expected 1, but was 2 at line 45.
- Explanation: V5 and V6 are both pending after the targeted V4 baseline.
- No workaround or Q-012 modification was made.

## Dependency and static verification

Command:

    JAVA_HOME=<JBR21> mvn --batch-mode --no-transfer-progress dependency:tree

Result: PASS. backend/pom.xml has no diff and no dependency was added.

Commands:

    sh scripts/verify-static.sh
    git diff --check

Final result after all ten review files were written: PASS for both.

Additional scope checks:

- No diff under security, tradingaccount, evidence, or decision main/test
  packages.
- No diff to V1-V5 or backend/pom.xml.
- Main Action source prohibited-vocabulary scan returned no match.
- Q013MySqlMigrationTests contains flyway.info().pending().length and contains
  no hard-coded unrestricted flyway.migrate count.
- No mandatory test was skipped.

## Disposable environment cleanup

Container brokeros-q013-mysql-20260901 was force-removed after verification.
No database volume was created. Maven test output under backend/target and the
temporary host log are not review-package source artifacts.

## Verification decision

Q-013-specific verification: PASS, 39/39.

Mandatory repository-wide gate: FAIL, 204 run with one unchanged Q-012
failure.

Lifecycle Gate Decision: **BLOCKED**.
