# Verification

## Environment

- Date/time zone: 2026-08-31, Asia/Kuala_Lumpur.
- Branch: `main`.
- Java: JetBrains Runtime OpenJDK 21.0.5.
- Maven: Apache Maven 3.9.9.
- Docker client/server: 29.7.2 / 29.7.2.
- Database: disposable official `mysql:8.4.11`, schema
  `brokeros_q011_fix_test`, bound to `127.0.0.1:33313` during verification.
- `@@log_bin_trust_function_creators=1` was supplied to the disposable server
  so mandatory Q-012 test-only failure triggers could run.
- Container `brokeros-q011-fix-mysql-20260831` was removed after verification.

Disposable passwords are redacted below; no credential was written into the
repository.

## Full repository real-MySQL gate

Executed from `backend/` with Java 21:

```text
Q009_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33313/brokeros_q011_fix_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q009_MYSQL_TEST_USERNAME='q011_fix_test' Q009_MYSQL_TEST_PASSWORD='<redacted>' \
Q010_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33313/brokeros_q011_fix_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q010_MYSQL_TEST_USERNAME='q011_fix_test' Q010_MYSQL_TEST_PASSWORD='<redacted>' \
Q011_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33313/brokeros_q011_fix_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q011_MYSQL_TEST_USERNAME='q011_fix_test' Q011_MYSQL_TEST_PASSWORD='<redacted>' \
Q012_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33313/brokeros_q011_fix_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q012_MYSQL_TEST_USERNAME='q011_fix_test' Q012_MYSQL_TEST_PASSWORD='<redacted>' \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
'/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
--batch-mode --no-transfer-progress test
```

Result at 2026-08-31 22:53:20 +08:00:

```text
Tests run: 165, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 29.814 s
```

Surefire XML independently summed to the same 165/0/0/0 result, with no report
containing `<failure>` or `<error>`. The directly affected class reported:

```text
Q011MySqlMigrationTests: tests=5, failures=0, errors=0, skipped=0
```

Flyway 11.7.2 emitted its existing warning that MySQL 8.4 is newer than the
latest explicitly tested version (8.1). Migration, validation, restart,
constraints, persistence, concurrency, security, and all-module tests still
executed successfully on actual MySQL 8.4.11; nothing was skipped.

## Static and diff gates

Commands executed from the repository root:

```text
sh scripts/verify-static.sh
git diff --check
git diff --unified=8 -- backend/src/test/java/com/brokeros/risk/evidence/infrastructure/persistence/Q011MySqlMigrationTests.java
git diff --numstat -- backend/src/test/java/com/brokeros/risk/evidence/infrastructure/persistence/Q011MySqlMigrationTests.java
git diff --name-only -- backend/src/main/java/com/brokeros/risk/evidence backend/src/test/java/com/brokeros/risk/evidence
```

Results:

- Static verification: `Static verification PASS`.
- Whitespace/error check: no output, exit 0.
- Authorized test diff: `2  1` for exactly
  `Q011MySqlMigrationTests.java`.
- Q-011 Evidence source/test diff name scan: exactly that same one file.
- The displayed hunk confirms no other assertion changed.

## Read-only recurrence scan

Commands:

```text
rg -n -C 8 'target\("[0-9]+"\)' backend/src/test/java -g '*.java'
rg -n 'migrate\(\)\.migrationsExecuted\)\.isEqualTo\([0-9]+\)' backend/src/test/java -g '*.java'
```

Finding: `Q012MySqlMigrationTests` has the same latent post-V4 hard-coded
`isEqualTo(1)` pattern. It currently passes because V5 is the sole migration
after V4. It was not modified because Q-012 files are explicitly out of scope.
The Q-009 post-baseline flow already uses pending metadata. Fixed target-version
baseline assertions were distinguished from the unstable unrestricted
post-baseline pattern.

## Verification decision

All mandatory checks for this narrow fix passed. No failed command or skipped
mandatory test remains. Independent review is still required before any
approval, completion, or Git action.
