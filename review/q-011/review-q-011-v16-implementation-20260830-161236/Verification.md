# Q-011 Verification

## Environment

- Date/time zone: 2026-08-30, Asia/Kuala_Lumpur (+08:00).
- Host: macOS 15.7.7, arm64.
- Java: JetBrains Runtime OpenJDK 21.0.5.
- Maven: Apache Maven 3.9.9, IntelliJ-bundled runtime.
- Docker image/container database: MySQL 8.4.11.
- JDBC driver: MySQL Connector/J 9.7.0 (existing dependency).
- Flyway: 11.7.2 (existing dependency).
- Disposable database: local container bound to `127.0.0.1:33311`, removed
  after verification. Test-only credential values are deliberately redacted
  from this committed artifact.

Flyway emitted a warning that this bundled Flyway version formally lists
tested MySQL support only through 8.1. It nevertheless migrated, validated,
restarted, and checksum-validated the required MySQL 8.4.11 instance. The
warning is retained as an outstanding tool-compatibility risk.

## Final commands and results

All Maven commands used:

```text
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress -DskipTests compile
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress -DskipTests test-compile
```

Result: PASS for compile and test-compile (159 main sources and 26 test
sources at the final compile stage).

```text
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress \
  -Dtest='EvidenceRestContractTests,EvidenceApplicationTests,EvidenceArchitectureTests,EvidenceDomainTests,EvidenceMetricsTests' test
```

Result: PASS, 20 tests, 0 failures, 0 errors, 0 skipped.

```text
Q011_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> \
Q011_MYSQL_TEST_USERNAME=<redacted-test-user> \
Q011_MYSQL_TEST_PASSWORD=<redacted-test-password> \
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress \
  -Dtest='EvidenceDomainTests,EvidenceApplicationTests,EvidenceArchitectureTests,EvidenceMetricsTests,Q011MySqlMigrationTests,Q011MySqlPersistenceTests,Q011SecurityMySqlIntegrationTests,FlywayMigrationTests' test
```

Result: PASS, 37 tests, 0 failures, 0 errors, 0 skipped. This command includes
all mandatory Q-011 real-MySQL classes; the four REST tests passed in the
preceding command.

```text
Q011_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
  -Dtest='Q011MySqlMigrationTests,Q011MySqlPersistenceTests' test
```

Result after final DDL fix: PASS, 12 tests, 0 failures/errors/skips.

```text
Q010_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
Q011_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
  -Dtest='Q011SecurityMySqlIntegrationTests,Q010BootstrapMySqlIntegrationTests' test
```

Result: PASS, 5 tests, 0 failures/errors/skips.

```text
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress test
```

Result: PASS, 124 tests, 0 failures, 0 errors, 29 conditional real-database
tests skipped because their datasource variables were intentionally absent.
This is not the mandatory Q-011 database gate; it is the environment-neutral
full regression.

```text
Q010_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
Q011_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress test
```

Result: PASS, 124 tests, 0 failures, 0 errors, 1 skipped. The only skip was
the Q-009 conditional integration test because Q009 datasource variables were
not supplied; every Q-010 and Q-011 database test executed.

```text
Q010_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
Q011_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress package
```

Result: PASS, 124 tests, 0 failures, 0 errors, 1 Q-009 conditional skip;
Spring Boot executable JAR successfully repackaged.

```text
Q009_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
Q010_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
Q011_MYSQL_TEST_URL=<local-mysql-8.4-jdbc-url> ... \
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress test
```

Result: **FAIL**, 124 tests, 1 failure, 0 errors, 0 skipped. Exact failure:

```text
Q009MySqlIntegrationTests.verifiesMigrationConstraintsQueryPlansAndPersistenceLifecycle:63
expected: 1
 but was: 3
```

The unchanged Q-009 test targets V1 and then makes an unrestricted `migrate()`
call. Its expected count was already stale once committed V3 existed; with
V4, V2/V3/V4 correctly produce three migrations. The Q-011 hard boundary
forbids editing Q-009, so this remains the sole repository-wide gate failure.

```text
JAVA_HOME=<JBR21> <INTELLIJ_MAVEN>/bin/mvn --batch-mode --no-transfer-progress dependency:tree
```

First attempt: environment failure because the filesystem sandbox denied
Maven tracking-file writes under the local repository. Rerun with approved
local Maven-cache access: PASS. `backend/pom.xml` has no diff, so the printed
dependency tree is unchanged by Q-011.

```text
sh scripts/verify-static.sh
sh -n scripts/verify-static.sh
git diff --check
```

Final result: PASS for all three. The first static run exposed that the script
still required exactly three migrations; the in-scope gate was extended to
require V1–V4 and verify V4's exact four-table, additive, schema-only shape,
then passed.

## Real MySQL evidence

Before container removal:

```text
SELECT VERSION();
8.4.11

flyway_schema_history:
1  V1__initial_schema.sql                              success=1
2  V2__create_security_actor_foundation.sql            success=1
3  V3__create_trading_account_reference_authority.sql  success=1
4  V4__create_evidence_provenance_foundation.sql       success=1

Evidence tables (all InnoDB):
evidence_access_log
evidence_operation
evidence_operation_history
evidence_record
```

The disposable container
`brokeros-q011-mysql-84-verification-20260830` was stopped and removed after
verification; `docker ps -a` returned no matching container.

## Defects found during verification and repaired

1. A history `CHECK` allowed CORRECT + NULL `before_status` via SQL
   three-valued logic; V4 now uses explicit `IS NOT NULL`, and MySQL proves it.
2. The Web controller affected Q-010's non-Web bootstrap; Servlet-Web
   conditional registration repaired the regression, with 5/5 targeted tests.
3. Stored malformed UTF-8 initially decoded with replacement characters;
   strict decoding now fails closed and is MySQL-tested.
4. Whitespace-only content is now rejected without altering valid content.
5. A metrics contract test initially inspected a record field instead of its
   accessor; the test was corrected and rerun 20/20.

No result above is inferred or fabricated. Failed intermediate runs are
retained here and in Lessons Learned.
