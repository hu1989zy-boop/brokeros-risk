# Verification

## Environment

- Host: macOS 15.7.7, arm64
- Java: JetBrains Runtime OpenJDK 21.0.5
- Maven: 3.9.9 (IntelliJ-bundled Maven)
- Docker: 29.7.2
- Disposable database: MySQL 8.4.11 on host port 33314
- Flyway: 11.7.2; emitted the documented warning that 8.4 is newer than its explicitly tested maximum 8.1

Secrets are intentionally redacted below. Disposable test credentials were supplied only through process environment variables and were not written to this package.

## Commands and outcomes

1. Compile and test-compile:

   ```text
   JAVA_HOME=<IntelliJ-JBR> <IntelliJ-Maven>/mvn --batch-mode --no-transfer-progress -DskipTests test-compile
   ```

   Final result: PASS; 280 main sources and 50 repository test sources compiled.

   One intermediate Q-014-only test compilation found a malformed generated newline literal in `ActionOutcomeArchitectureTests`; the new test was corrected and the final command passed.

2. Q-014 non-MySQL suites:

   ```text
   mvn --batch-mode --no-transfer-progress \
     -Dtest='ActionOutcomeDomainTests,ActionOutcomeApplicationTests,ActionOutcomeArchitectureTests,ActionOutcomeRestContractTests,ActionOutcomeMetricsTests' test
   ```

   Result: PASS — 23 tests, 0 failures, 0 errors, 0 skipped.

3. Q-014 real-MySQL suites:

   ```text
   Q014_MYSQL_TEST_URL=<disposable-jdbc-url> \
   Q014_MYSQL_TEST_USERNAME=<disposable-test-user> \
   Q014_MYSQL_TEST_PASSWORD=<disposable-test-value> \
   mvn --batch-mode --no-transfer-progress \
     -Dtest='Q014MySqlMigrationTests,Q014MySqlPersistenceTests,Q014SecurityMySqlIntegrationTests' test
   ```

   The sandboxed attempt produced 19 `SocketException: Operation not permitted` errors because local socket access was denied; this was an environment restriction, not a code failure. The host-authorized retry initially passed 17 and errored 2 because the constrained MySQL user could not create test triggers with binary logging enabled (MySQL error 1419). The disposable container alone was configured with `SET GLOBAL log_bin_trust_function_creators=1`, then the same tests passed: 19 tests, 0 failures, 0 errors, 0 skipped.

4. Final complete Q-014 suite against real MySQL:

   ```text
   Q014_MYSQL_TEST_URL=<disposable-jdbc-url> \
   Q014_MYSQL_TEST_USERNAME=<disposable-test-user> \
   Q014_MYSQL_TEST_PASSWORD=<disposable-test-value> \
   mvn --batch-mode --no-transfer-progress \
     -Dtest='ActionOutcomeDomainTests,ActionOutcomeApplicationTests,ActionOutcomeArchitectureTests,ActionOutcomeRestContractTests,ActionOutcomeMetricsTests,Q014MySqlMigrationTests,Q014MySqlPersistenceTests,Q014SecurityMySqlIntegrationTests' test
   ```

   Result: PASS — 42 tests, 0 failures, 0 errors, 0 skipped.

5. Mandatory Q-009–Q-014 full real-MySQL gate:

   ```text
   Q009_MYSQL_TEST_URL=<disposable-jdbc-url> Q009_MYSQL_TEST_USERNAME=<test-user> Q009_MYSQL_TEST_PASSWORD=<test-value> \
   Q010_MYSQL_TEST_URL=<disposable-jdbc-url> Q010_MYSQL_TEST_USERNAME=<test-user> Q010_MYSQL_TEST_PASSWORD=<test-value> \
   Q011_MYSQL_TEST_URL=<disposable-jdbc-url> Q011_MYSQL_TEST_USERNAME=<test-user> Q011_MYSQL_TEST_PASSWORD=<test-value> \
   Q014_MYSQL_TEST_URL=<disposable-jdbc-url> Q014_MYSQL_TEST_USERNAME=<test-user> Q014_MYSQL_TEST_PASSWORD=<test-value> \
   mvn --batch-mode --no-transfer-progress test
   ```

   Result: FAIL — 246 tests run, 3 failures, 0 errors, 0 skipped (243 passed).

   Failing existing Q-013 tests:

   - `Q013MySqlMigrationTests.migrationUpgradesV5CreatesExactlyThreeTablesWithoutSeedsAndValidatesOnRestart`
   - `Q013MySqlMigrationTests.metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex`
   - `ActionRestContractTests.resultCodesExposeExactlyTheApprovedActionHttpContract`

   Cause: Q-013 assertions use broad `action_%` / `ACTION_` prefix ownership and therefore include valid Q-014 tables, foreign keys, and result codes. Q-014's prompt forbids modifying those Q-013 tests.

6. Static and repository checks:

   ```text
   bash scripts/verify-static.sh
   git diff --check
   mvn --batch-mode --no-transfer-progress dependency:tree
   ```

   Results: PASS, PASS, PASS. `pom.xml` was not changed and no new dependency was introduced.

7. Cleanup:

   ```text
   docker stop brokeros-q014-mysql-20260902
   docker ps -a --filter name=brokeros-q014-mysql-20260902
   ```

   Result: the container was started with `--rm`, stopped successfully, and the final query was empty.

## Gate conclusion

Q-014-specific verification is green. The mandatory lifecycle regression gate is not green; overall Gate Decision remains **BLOCKED**.
