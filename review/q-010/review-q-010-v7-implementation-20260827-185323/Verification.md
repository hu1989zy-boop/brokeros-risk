# Q-010 V7 Verification

## Final verification results

| Command / gate | Result | Evidence |
| --- | --- | --- |
| `mvn -f backend/pom.xml -DskipTests package` | PASS | 111 production sources compiled; application jar built |
| `mvn -f backend/pom.xml -Dtest='com.brokeros.risk.tradingaccount.**' test` | PASS | 27 Q-010 tests discovered; 16 non-MySQL executed, 11 environment-gated in this invocation |
| disposable MySQL 8.4 + `mvn ... -Dtest='*Q010*MySqlIntegrationTests' test` | PASS | 11 tests executed, 0 failures/errors/skips |
| `mvn -f backend/pom.xml verify` outside restricted sandbox | PASS | 86 tests, 0 failures/errors, 12 environment-gated skips; Q-010 real-MySQL tests separately executed above |
| `mvn -f backend/pom.xml dependency:tree ...` | PASS | one Boot-managed Spring Security/Nimbus stack; existing JDBC/MySQL/Flyway only; no new dependency |
| `sh scripts/verify-kustomize.sh` | PASS | base/test/prod render and contract checks passed |
| infrastructure script with temporary no-host-port Compose overlay | PASS | build, MySQL/Redis/Kafka/backend health, V1/V2/V3, seven tables, restart, health APIs, log scan, cleanup |
| `git diff --check` | PASS | no tracked whitespace errors |
| `git diff --cached --check` | PASS | index empty/clean |
| `sh scripts/verify-static.sh` | KNOWN PRE-EXISTING FAILURE ONLY | unchanged Q-009 Prompt lines 67/68 and EOF blank line; no new Q-010 issue |

## Real MySQL inventory

The 10 persistence tests and one full command test prove:

- V1→V2→V3 migration, exact four-Q-010/seven-total tables, constraints,
  collations, VARBINARY external key, Flyway validate/restart;
- registration, exact replay, compatible duplicate, conflicting replay and
  mapping provenance conflict;
- generated ref collision capped at three attempts;
- concurrent same-identity registration and concurrent same-operation delivery;
- lifecycle CAS lost-update prevention and historical eligibility resolution;
- operation failure and history failure rollback;
- exact MySQL CHECK vendor error 3819 / SQLState HY000; and
- actual Q-009 service mapping/grants, strict non-Web command success/replay,
  safe output, revoked-grant denial, and zero mutation after denial.

## Baseline/environment notes

The initial sandboxed baseline Maven invocation failed because Mockito could
not self-attach and Surefire could not create its host temp directory. The same
full suite passed when rerun with the required host permissions. The first
infrastructure attempt encountered an already occupied host port 3306; rerun
used a temporary Compose overlay removing host publishing while preserving all
internal isolated-stack checks, and passed. The overlay and disposable MySQL
containers were removed.

Flyway 11.7.2 emits its existing advisory that MySQL 8.4 is newer than Flyway's
latest tested 8.1 target. Migrations and all MySQL 8.4 checks nevertheless
passed; no dependency change was authorized or made.
