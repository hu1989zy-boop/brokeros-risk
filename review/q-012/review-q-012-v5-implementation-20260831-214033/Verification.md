# Q-012 Verification Evidence

## Environment

- Date/time zone: 2026-08-31, Asia/Kuala_Lumpur.
- Branch: `main` (no branch creation, staging, commit, or push).
- OS: macOS 15.7.7, arm64.
- Java used by builds: JetBrains Runtime OpenJDK 21.0.5.
- Maven: Apache Maven 3.9.9, IntelliJ bundled Maven.
- Docker client/server: 29.7.2 / 29.7.2.
- Disposable database: official `mysql:8.4.11`, server 8.4.11, container
  `brokeros-q012-mysql-20260831`, host port 33312, schema
  `brokeros_q012_test`. The disposable container was removed after all
  verification evidence was captured.
- Flyway: 11.7.2. It emitted its existing warning that explicitly tested
  support ends at MySQL 8.1; all Q-012 tests below ran on actual 8.4.11.

Disposable test-password values are deliberately redacted from this repository
artifact. Commands below retain their exact executable structure and replace
only the secret with `${Q012_MYSQL_TEST_PASSWORD}` (and analogous earlier-Q
variables).

## Successful checks

### Compilation and Q-012 non-database tests

Commands run from `backend/` with `JAVA_HOME` set to the Java 21 runtime:

```text
mvn --batch-mode --no-transfer-progress -DskipTests test-compile
mvn --batch-mode --no-transfer-progress -Dtest='DecisionDomainTests,DecisionApplicationTests,DecisionArchitectureTests,DecisionRestContractTests,DecisionMetricsTests' test
```

Results:

- main/test compilation: PASS.
- selected Q-012 non-database tests: 22 run, 0 failures, 0 errors, 0
  skipped; BUILD SUCCESS.

### Q-012 mandatory real-MySQL gate

Final command run from `backend/`:

```text
Q012_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33312/brokeros_q012_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q012_MYSQL_TEST_USERNAME='q012_test' \
Q012_MYSQL_TEST_PASSWORD="${Q012_MYSQL_TEST_PASSWORD}" \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress \
-Dtest='Q012MySqlMigrationTests,Q012MySqlPersistenceTests,Q012SecurityMySqlIntegrationTests' test
```

Final result at 2026-08-31 21:39:58 +08:00:

```text
Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The tests applied and validated V1–V5 on MySQL 8.4.11. Coverage includes
every Design §8.5 row, exactly four unseeded new tables, only three
intra-module foreign keys, checks/uniques/indexes, indexed query plans,
generated-ref collision retry, atomic forced rollback, concurrent replay,
audited detail access, security integration, and audit failure isolation.

### All non-database regressions plus Q-012 MySQL

Command run from `backend/`, with only the three Q012 MySQL variables and Java
21 supplied:

```text
mvn --quiet test
```

Result: exit 0; 165 tests, 0 failures, 0 errors, 29 skipped. The 29 skips are
the older Q-009/Q-010/Q-011 database classes because this diagnostic command
intentionally supplied only `Q012_MYSQL_TEST_*`. Q-012's 19 database tests were
not skipped.

### Packaging

Host-side command run from `backend/`:

```text
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress package
```

Result: BUILD SUCCESS; 165 tests, 0 failures, 0 errors, 48 skipped because no
database environment variables were supplied for this packaging-only check.
Created `target/brokeros-risk-backend-0.1.0-SNAPSHOT.jar`.

### Static and dependency checks

Commands:

```text
sh scripts/verify-static.sh
git diff --check
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress dependency:tree
```

Results:

- static verification: `Static verification PASS`, exit 0.
- diff whitespace check: no output, exit 0.
- dependency tree: BUILD SUCCESS. `backend/pom.xml` is unchanged; no dependency
  was added.

## Failed checks retained honestly

### Initial real-MySQL trigger privilege failure

The first Q-012 database run completed 17 of 19 tests and errored in two
test-only trigger cases with MySQL error 1419: the disposable non-root user was
not permitted to create a trigger while binary logging was enabled. The
application migration did not require that privilege. On the disposable server
only, this was enabled:

```text
docker exec brokeros-q012-mysql-20260831 mysql \
  --user=root --password='<redacted-disposable-secret>' \
  --execute='SET GLOBAL log_bin_trust_function_creators=1'
```

The unchanged 19-test Q-012 suite then passed twice, including the final run
reported above. No mandatory test was skipped or weakened.

### Erroneous repository-root Maven invocation

One Q-012-only diagnostic command was first invoked from the repository root,
which has no `pom.xml`. Maven returned `MissingProjectException`, exit 1. It was
immediately rerun from `backend/` and passed with 165 tests, 0 failures/errors,
29 older database skips. This was a working-directory error, not a code result.

### Sandboxed package attempt

The first `mvn --quiet package` inside the restricted sandbox failed with 24
Mockito initialization errors because the sandbox prevented the Java agent from
dynamically attaching. Result: 165 tests, 0 failures, 24 errors, 48 skipped.
Per the environment escalation rule, the same package was rerun in the approved
host context and succeeded as documented above.

### Mandatory all-Q009/Q010/Q011/Q012 real-MySQL gate

Command run from `backend/` (test secrets redacted):

```text
Q009_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33312/brokeros_q012_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q009_MYSQL_TEST_USERNAME='q012_test' Q009_MYSQL_TEST_PASSWORD="${Q009_MYSQL_TEST_PASSWORD}" \
Q010_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33312/brokeros_q012_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q010_MYSQL_TEST_USERNAME='q012_test' Q010_MYSQL_TEST_PASSWORD="${Q010_MYSQL_TEST_PASSWORD}" \
Q011_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33312/brokeros_q012_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q011_MYSQL_TEST_USERNAME='q012_test' Q011_MYSQL_TEST_PASSWORD="${Q011_MYSQL_TEST_PASSWORD}" \
Q012_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33312/brokeros_q012_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q012_MYSQL_TEST_USERNAME='q012_test' Q012_MYSQL_TEST_PASSWORD="${Q012_MYSQL_TEST_PASSWORD}" \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress test
```

Result:

```text
Tests run: 165, Failures: 1, Errors: 0, Skipped: 0
Q011MySqlMigrationTests.migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart
expected: 1
 but was: 2
at Q011MySqlMigrationTests.java:45
```

Flyway correctly executed V4 and V5 after the test's V3 target. The Q-012 Prompt
forbids modifying this unchanged Q-011 file. Therefore the mandatory full gate
remains failed and Q-012 Acceptance Criterion 11 is not satisfied. No weaker
command is substituted for that conclusion.

## Verification decision

Q-012-specific implementation verification: PASS.

Overall authorized implementation-stage gate: **BLOCKED**, because the
mandatory unchanged all-module MySQL regression gate is not green and resolving
the incompatible Q-011 assertion requires authority outside Q-012's hard
boundary.
