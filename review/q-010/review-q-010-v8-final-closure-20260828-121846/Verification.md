# Q-010 V8 Final Verification

## Environment

- Verification date/time zone: 2026-08-28 / Asia/Shanghai
- Host: macOS 15.7.4 arm64
- Host Maven: 3.9.9
- Host JDK: 23.0.2; project compiler release: 21
- Approved/CI JDK proof: Eclipse Temurin 21.0.7 container
- Docker client/server: 29.7.2
- MySQL: isolated `mysql:8.4`, actual 8.4.11
- Spring Boot: 3.5.16
- Flyway: 11.7.2
- kubectl: 1.36.1; Kustomize: 5.8.1

## Results

| Exact command / gate | Exit | Result / evidence |
| --- | ---: | --- |
| `shasum -a 256 -c review/q-010/review-q-010-v7-implementation-20260827-185323/ArtifactHashes.txt` | 0 | all 68 V7 entries OK before V8 edits |
| `cmp -s V3__create_trading_account_reference_authority.sql <V7>/MigrationSnapshot.sql` | 0 | exact migration snapshot match |
| `unzip -t review/q-010/review-q-010-v7-implementation-20260827-185323.zip` | 0 | V7 archive integrity PASS |
| `mvn -f backend/pom.xml verify` | 0 | host: 86 tests, 0 failures, 0 errors, 12 environment skips |
| `Q010_MYSQL_TEST_* ... mvn -f backend/pom.xml -Dtest='*Q010*MySqlIntegrationTests' test` | 0 | MySQL 8.4.11: 11 tests, 0 failures/errors/skips |
| `mvn -f backend/pom.xml dependency:tree -Dscope=runtime` | 0 | no dependency drift; one Boot Security/Nimbus stack |
| `sh scripts/verify-kustomize.sh` | 0 | base/test/prod render and contract PASS |
| `sh scripts/verify-static.sh` | 2 | only unchanged historical Q-009 whitespace lines 67/68/EOF |
| `sh scripts/verify-infrastructure.sh` | 1 | first attempt only: host port 3306 already occupied; isolated project cleaned |
| `COMPOSE_FILE=docker-compose.yml:<temporary-no-host-port-overlay> sh scripts/verify-infrastructure.sh` | 0 | full isolated stack, V1/V2/V3, 7 tables, restart, Redis/Kafka/backend, log scan, cleanup PASS |
| Java 21 builder from backend-only context | 1 | superseded attempt: 5 repository-root path errors; product tests otherwise ran |
| `docker start -a brokeros-risk-q010-v8-java21-repo-verify` running `mvn -f backend/pom.xml verify` | 0 | Temurin 21.0.7; 111 production + 18 test sources compiled; 86 tests, 0 failures/errors, 12 environment skips |
| `git diff --check` / `git diff --cached --check` | 0 | tracked/index whitespace clean; index empty |

The 12 full-suite skips are environment-gated Q-009/Q-010 MySQL tests. They do
not hide Q-010 evidence: all 11 Q-010 MySQL tests ran separately against the
isolated 8.4.11 database with zero skips. Q-009 was already runtime-closed in
V9/V10 and its existing integration test was not changed by Q-010.

## MySQL/Flyway result

The clean schema applied exactly V1, V2, and V3; restart retained three
successful rows and reported no pending migration. The actual schema contained
the three Q-009 plus four Q-010 application tables and every approved named
PK/FK/CHECK/unique/index. The tests verified exact external-key bytes, CHECK
3819/HY000, duplicate 1062/23000 classification, collisions, races, CAS,
history/outcome rollback, and the real non-Web command/security path.

Flyway 11.7.2 emits a support advisory because MySQL 8.4 is newer than its
latest tested 8.1 target. The repository's explicit Q-010 baseline is MySQL
8.4, and all required 8.4.11 behavior passed; this remains a documented
non-blocking tooling warning, not an inferred compatibility claim.

## ZIP verification protocol

After all review files are final, the archive must pass `unzip -t`, exact
manifest/file-count comparison, non-empty-file checks, secret/content scans,
and exclusion scans for source/build/.git/IDE/historical-review content. The
final archive SHA-256 is recorded in the sibling `.sha256` sidecar and final
handoff. Embedding an archive's own final hash inside itself would invalidate
that hash, so the sidecar is the authoritative non-circular checksum record.
