# Verification

## Environment

- Host: macOS 15.7.7, arm64
- Java: JetBrains Runtime 21.0.5
- Maven: 3.9.9
- Database: disposable Docker container using MySQL 8.4.11
- Database migrations: Flyway V1, V2, V3, and V4 applied successfully
- Teardown: the disposable verification container was stopped and removed

## Full Real-MySQL Gate

Executed the repository-wide Maven test suite with all three real-MySQL
datasource groups configured:

```text
Q009_MYSQL_TEST_URL=<local MySQL 8.4.11 datasource> \
Q010_MYSQL_TEST_URL=<local MySQL 8.4.11 datasource> \
Q011_MYSQL_TEST_URL=<local MySQL 8.4.11 datasource> \
JAVA_HOME=<JetBrains Runtime 21.0.5> mvn --batch-mode --no-transfer-progress test
```

Result:

```text
Tests run: 124, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 15.236 s
```

Specific persisted-clock coverage included:

- `Q009MySqlIntegrationTests`: 1 passed, 0 skipped
- `Q010BootstrapMySqlIntegrationTests`: 1 passed, 0 skipped
- `Q011MySqlMigrationTests`: 5 passed, 0 skipped
- `Q011MySqlPersistenceTests`: 8 passed, 0 skipped
- `Q011SecurityMySqlIntegrationTests`: 4 passed, 0 skipped

The test compilation covered 159 production source files and 26 test source
files at Java release 21.

## Static and Diff Gates

```text
sh scripts/verify-static.sh
Static verification PASS

git diff --check
PASS (no output)
```

## Authorized-Boundary Evidence

Before-and-after SHA-256 comparisons confirmed:

- all eight production Clock consumer files unchanged;
- all 26 test files unchanged;
- Flyway migrations V1–V4 unchanged;
- `backend/pom.xml` unchanged.

Source inspection found exactly one production Clock bean:
`SecurityModuleConfiguration.securityClock()`. Its task-scoped production
diff is limited to the `java.time.Duration` import and the replacement of
`Clock.systemUTC()` with
`Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000))`.

## Environment-Dependent Proof Limitation

The old code also passed on this macOS/JBR21 host because its observed JVM
clock did not expose the sub-microsecond entropy needed to trigger the known
mismatch. Therefore this host's green result alone does **not** fully prove the
fix. Claude Code must independently re-run the full suite in the Linux/Docker
Java 21 environment where the old implementation is known to reproduce the
failure deterministically.

## Tooling Note

Flyway emitted its existing warning that MySQL 8.4 is newer than the version
for which the bundled Flyway release reports formal support (8.1). All four
migrations and all tests nevertheless completed successfully. This warning is
recorded for review and was not addressed because dependency changes are
outside the authorized scope.
