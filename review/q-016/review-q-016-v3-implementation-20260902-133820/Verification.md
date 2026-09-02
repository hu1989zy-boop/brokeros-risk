# Q-016 Verification

## Gate Decision

**PASS WITH CONDITIONS** — backend and static verification passed; Flutter/browser
verification was not executable in this environment.

## Tool availability

```text
command -v flutter
flutter --version
command -v dart
dart --version
```

Result: `flutter` and `dart` were not found (exit 127). Consequently the following
were **not executed** and no pass result is claimed:

- `flutter pub get`
- `dart run build_runner build --delete-conflicting-outputs`
- `flutter analyze`
- `flutter test`
- `flutter build web`
- browser login/Bearer/refresh/logout execution
- full Flutter -> Keycloak -> backend -> MySQL end-to-end run

## Backend compilation and focused tests

Commands used Java 21 from IntelliJ's bundled runtime and Maven 3 from IntelliJ.

```text
cd backend
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress -DskipTests test-compile
```

Result: **PASS**.

The disposable database was MySQL 8.4 on `127.0.0.1:33316`. The ephemeral test
password is deliberately redacted from this artifact; every other command component
is reproduced below.

```text
cd backend
Q008_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
Q008_MYSQL_TEST_USERNAME='q016_test' \
Q008_MYSQL_TEST_PASSWORD='<redacted ephemeral test value>' \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress \
  -Dtest='Q016RiskCaseListApplicationTests,Q016RiskCaseListMySqlTests,RiskCaseRestContractTests' test
```

Result: **PASS — 9 tests, 0 failures, 0 errors, 0 skipped**. This includes two
real-MySQL list tests for filters/projection and 100-row server capping/stable order.

## Full repository real-MySQL backend gate

The final full run mapped all Q-008–Q-014 test aliases to the same isolated schema;
each `<redacted>` value was the same ephemeral test-only password.

```text
cd backend
Q008_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' Q008_MYSQL_TEST_USERNAME='q016_test' Q008_MYSQL_TEST_PASSWORD='<redacted>' \
Q009_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' Q009_MYSQL_TEST_USERNAME='q016_test' Q009_MYSQL_TEST_PASSWORD='<redacted>' \
Q010_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' Q010_MYSQL_TEST_USERNAME='q016_test' Q010_MYSQL_TEST_PASSWORD='<redacted>' \
Q011_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' Q011_MYSQL_TEST_USERNAME='q016_test' Q011_MYSQL_TEST_PASSWORD='<redacted>' \
Q012_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' Q012_MYSQL_TEST_USERNAME='q016_test' Q012_MYSQL_TEST_PASSWORD='<redacted>' \
Q013_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' Q013_MYSQL_TEST_USERNAME='q016_test' Q013_MYSQL_TEST_PASSWORD='<redacted>' \
Q014_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:33316/brokeros_q016_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' Q014_MYSQL_TEST_USERNAME='q016_test' Q014_MYSQL_TEST_PASSWORD='<redacted>' \
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress test
```

Result: **PASS — 305 tests, 0 failures, 0 errors, 0 skipped**; total Maven time
1:06. Flyway emitted its existing compatibility warning that this version is tested
through MySQL 8.1; migration and tests nevertheless completed against MySQL 8.4.

```text
cd backend
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress -DskipTests package
```

Result: **PASS**.

## Dev identity and configuration checks

- Ran the packaged `SecurityBootstrapCommand` with
  `deploy/keycloak/q016-security-bootstrap.json` against the disposable MySQL
  database: **PASS**, `createdActors=1`, `unchangedActors=0`.
- Ran `docker compose --profile console config --quiet` with temporary environment
  values: **PASS**.
- Ran JSON assertions over `deploy/keycloak/brokeros-realm.json`: **PASS** for public
  client, standard flow, disabled implicit/direct grants, PKCE S256, audience mapper,
  fixed user ID, and absence of a committed password.
- Ran `jq` validation over the security bootstrap: **PASS**; only
  `risk-case:read` and `risk-case:note` are granted.
- Ran `sh -n scripts/run-risk-console-dev.sh`: **PASS**.
- Ran `ConfigurationContractIntegrationTests`: **PASS — 7 tests, 0 failures,
  0 errors, 0 skipped**.

Keycloak itself was not started for a browser flow because the Flutter SDK was
unavailable. Compose rendering and static realm/bootstrap checks are not represented
as a live identity-provider test.

## Repository/static checks

```text
sh scripts/verify-static.sh
sh -n scripts/run-risk-console-dev.sh
git diff --check
```

Final result: **PASS** for all three. Directly executing the pre-existing
`./scripts/verify-static.sh` first returned permission denied because that existing
file lacks an executable bit; invoking the POSIX script through `sh` passed. The
Q-016 launcher itself is executable.

## Corrected diagnostics

An earlier broad test attempt exposed two environment/test-fixture issues rather than
product failures: configuration aliases added by Q-016 were absent from the governed
configuration catalog, and MySQL binary logging rejected Flyway trigger creation in
the disposable instance. The catalog was updated in scope, the disposable server was
given `log_bin_trust_function_creators=1`, and the final all-alias run above passed
305/305 with no skips. The unsuccessful diagnostic was not relabeled as passing.

## Cleanup

`docker rm -f brokeros-q016-mysql-test` removed the disposable Q-016 MySQL container.
A final filtered `docker ps -a` returned no matching container. No persistent test
container was left running.
