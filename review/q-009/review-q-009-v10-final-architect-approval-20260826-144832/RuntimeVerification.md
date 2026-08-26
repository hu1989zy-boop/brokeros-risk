# Runtime Verification

## Fresh V10 Execution

| Check | Result |
| --- | --- |
| Disposable database | PASS — task-only Docker container |
| MySQL server version | PASS — 8.4.11 |
| Flyway migration/validation | PASS — V1 to V2 |
| `Q009MySqlIntegrationTests` | PASS — 1/1, 0 skipped |
| Full `mvn verify` | PASS — 58/58, 0 skipped |
| Disposable resource cleanup | PASS — no labeled container, volume, or network remains |
| Host MySQL 5.7 isolation | PASS — not targeted |

The database used unique ephemeral credentials, a loopback-only random host
port, and a Q-009 V10 verification label. Cleanup was checked after Maven
completed.

## Reconciled V9 Runtime Evidence

The immutable V9 review package was opened and reconciled. It records PASS for
Docker Compose application startup, MySQL/Redis/Kafka/backend health, Flyway
restart behavior, Redis/Kafka/API/log checks, full cleanup, and Kustomize
base/test/prod rendering. V10 did not rerun those unaffected gates; it reran the
database-sensitive and complete Maven gates affected by the prior runtime
closure.

Flyway reports its existing MySQL 8.4 support advisory because its latest
tested version is 8.1. Actual migration and verification on 8.4.11 pass, so the
advisory is recorded as non-blocking maintenance rather than suppressed.
