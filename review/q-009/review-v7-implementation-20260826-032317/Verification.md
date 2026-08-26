# Verification

## Overall Result

Verification: **FAIL**

The build and all available non-MySQL tests pass. Required real MySQL 8.4,
Kustomize, and isolated infrastructure verification are unavailable on this
host, so the Definition of Done is not satisfied.

## Commands and Results

| Command | Result | Evidence |
| --- | --- | --- |
| `mvn verify` | PASS | Build success; 58 tests, 0 failures, 0 errors, 1 MySQL skip |
| `mvn -DskipTests package` | PASS | Executable Spring Boot JAR generated |
| Spring Security/Nimbus dependency tree | PASS | Boot 3.5.16; Spring Security 6.5.11; one transitive Nimbus 9.37.4; no direct pin |
| `git diff --check` | PASS | No tracked diff whitespace errors |
| Scoped no-index checks for new Q-009 files | PASS | No new source/test/migration/skill/lesson whitespace errors |
| Shell syntax checks | PASS | Static, Kustomize, and infrastructure scripts parse with `sh -n` |
| `sh scripts/verify-static.sh` | FAIL | Pre-existing untracked V6 staging prompt has trailing whitespace and blank EOF; protected from modification by scope |
| `sh scripts/verify-kustomize.sh` | FAIL at preflight | `kubectl` is not installed |
| `sh scripts/verify-infrastructure.sh` | FAIL at preflight | Docker with Compose v2 is not installed |
| `Q009MySqlIntegrationTests` on MySQL 8.4 | NOT RUN | No Docker, MySQL client/server, or reachable `127.0.0.1:3306` |

## Required Completion Commands

On a host with Docker Compose v2 and `kubectl`:

1. Run `sh scripts/verify-infrastructure.sh`.
2. Start an isolated disposable MySQL 8.4 database dedicated to the test.
3. Set `Q009_MYSQL_TEST_URL`, `Q009_MYSQL_TEST_USERNAME`, and
   `Q009_MYSQL_TEST_PASSWORD` to that disposable database.
4. Run
   `mvn -Dtest=Q009MySqlIntegrationTests test` from `backend/`.
5. Run `sh scripts/verify-kustomize.sh`.
6. Regenerate a new Review Package; do not overwrite V7.

The MySQL test invokes Flyway clean and must never target a shared or production
database.
