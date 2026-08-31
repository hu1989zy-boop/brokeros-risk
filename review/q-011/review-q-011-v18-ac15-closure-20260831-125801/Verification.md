# Q-011 V18 AC15 Closure Verification

## Environment

- Host: macOS 15.7.7, arm64.
- Java: JetBrains Runtime OpenJDK 21.0.5.
- Maven: Apache Maven 3.9.9, IntelliJ-bundled runtime.
- Docker client/server: 29.7.2.
- Disposable database: official MySQL 8.4.11 image, host-bound only on
  `127.0.0.1:33311`.
- Existing Flyway: 11.7.2; no dependency change.

The existing Flyway version emitted its known warning that formally tested
MySQL support is listed only through 8.1. It nevertheless migrated and validated
V1–V4 successfully on the required MySQL 8.4.11 instance.

Test-only credential values are deliberately redacted from this artifact.

## Authorized diff verification

Task-scoped command:

```text
git diff --stat -- backend/src/test/java/com/brokeros/risk/security/infrastructure/persistence/Q009MySqlIntegrationTests.java
```

Result:

```text
.../Q009MySqlIntegrationTests.java | 3 ++-
1 file changed, 2 insertions(+), 1 deletion(-)
```

The only code/test change replaces the literal `1` with an `int` captured from
`flyway.info().pending().length` and compares the migration result to that
value. The rest of the test is byte-for-byte unchanged relative to the normal
Git diff context.

Before/after SHA-256 inventories confirmed:

- all 62 Q-010 main/test files unchanged;
- all 56 Q-011 main/test files unchanged;
- V1, V2, V3, and V4 migration files unchanged; and
- no other Q-009 main/test file has a Git diff.

## Disposable MySQL startup

```text
docker run --rm -d \
  --name brokeros-q011-ac15-mysql-84-verification-20260831 \
  -p 127.0.0.1:33311:3306 \
  -e MYSQL_ROOT_PASSWORD=<redacted-test-password> \
  -e MYSQL_DATABASE=brokeros_q011_test \
  -e MYSQL_USER=<redacted-test-user> \
  -e MYSQL_PASSWORD=<redacted-test-password> \
  mysql:8.4.11 --log-bin-trust-function-creators=1
```

`SELECT VERSION()` returned `8.4.11`. A first shell-loop readiness wrapper was
blocked by the managed sandbox's Docker-socket segmentation; a direct approved
`docker exec ... mysqladmin ping` call then returned `mysqld is alive`. This was
an orchestration permission artifact, not a database or test failure.

## Required repository-wide real-MySQL gate

Exact command structure (test credentials redacted):

```text
Q009_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33311/brokeros_q011_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q009_MYSQL_TEST_USERNAME=<redacted-test-user> \
Q009_MYSQL_TEST_PASSWORD=<redacted-test-password> \
Q010_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33311/brokeros_q011_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q010_MYSQL_TEST_USERNAME=<redacted-test-user> \
Q010_MYSQL_TEST_PASSWORD=<redacted-test-password> \
Q011_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33311/brokeros_q011_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q011_MYSQL_TEST_USERNAME=<redacted-test-user> \
Q011_MYSQL_TEST_PASSWORD=<redacted-test-password> \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress test
```

Result: **PASS**.

```text
Tests run: 124, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Compilation occurred within the same command: 159 main sources and 26 test
sources compiled with Java release 21.

Specific required outcomes:

- `Q009MySqlIntegrationTests`: 1 test, 0 failures/errors/skips — dynamic
  migration-count assertion passed.
- `Q010BootstrapMySqlIntegrationTests`: 1 test, 0 failures/errors/skips on this
  host. The known timestamp-precision issue did not reproduce here and was not
  investigated or modified.
- Every Q-011-owned and shared migration test passed unchanged.

Post-run `flyway_schema_history` showed successful V1, V2, V3, and V4 rows.
The disposable container was stopped; because it used `--rm`, a subsequent
`docker ps -a` filter returned no matching container.

## Static and diff gates

```text
sh scripts/verify-static.sh
git diff --check
```

Results: `Static verification PASS`; `git diff --check` exited 0 with no
output. These commands were rerun after generating the final closure artifacts.

## AC15 result

AC15: **PASS in this execution**. The mandatory full Maven, MySQL/Flyway,
security, and static gates all passed with no mandatory test skipped. Final
closure acceptance remains an independent-review/Product Owner decision.
