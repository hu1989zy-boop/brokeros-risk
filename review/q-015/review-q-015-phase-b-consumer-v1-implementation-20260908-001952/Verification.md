# Verification — Q-015 Phase B consumer

## Final results

| Check | Result | Evidence |
| --- | --- | --- |
| Host Java 21 compile | PASS | 427 production sources; Maven compile succeeded |
| Focused canonical/config/Kafka suite | PASS | 12 tests, 0 failures/errors/skips; includes 4 real MySQL + embedded broker cases |
| Full backend test | PASS | 356 tests, 0 failures/errors/skips with Q008…Q015 database variables enabled |
| Final Maven package | PASS | Final code rebuilt after output clearing; 356 tests, 0 failures/errors/skips, executable Spring Boot JAR produced |
| Final Q-015 subset | PASS | 38 tests, including Q015MySqlTests 5/5 and Q015CanonicalKafkaMySqlTests 4/4 |
| Static | PASS | sh scripts/verify-static.sh; exit 0 |
| Kustomize | PASS | sh scripts/verify-kustomize.sh; base/test/prod renders and contract, exit 0 |
| Diff/unchanged boundaries | PASS | git diff --check and unchanged migration/store/envelope/golden/deploy comparisons, exit 0 |
| Test environment | PASS | MySQL 8.4.11, all V1…V9 migrations success; embedded Kafka test contexts; cleaned up |
| Production Kafka / Windows gateway / MT5 | NOT EXECUTED | Outside portable scope; no live broker, Manager or SDK was accessed |
| Hosted CI / Kubernetes deploy / whole Compose stack | NOT EXECUTED | Local full backend and render evidence only; no infrastructure/deployment change |

Final package finished 2026-09-07 16:20:59 UTC (2026-09-08 00:20:59 Asia/Kuala_Lumpur),
Maven elapsed 1:34. Artifact: backend/target/brokeros-risk-backend-0.1.0-SNAPSHOT.jar.
Seventy-six executed suites and per-Q-015 test names are in TestInventory.txt.

## Runtime and isolation

- Linux aarch64 Docker, Maven 3.9.9, Eclipse Adoptium Java 21.0.7.
- Maven image: maven:3.9.9-eclipse-temurin-21-alpine.
- Disposable database image: mysql:8.4.11, version query 8.4.11.
- Spring Boot 3.5.16; BOM-managed spring-kafka / spring-kafka-test 3.3.16.
- Full repository mounted read-only at /workspace, with only backend/target
  writable. Existing brokeros-q011-verify-m2 dependency cache reused, retained.
- Unique task network/container q015-consumer-20260908; no existing service touched,
  no published database port or production credentials. Credentials generated with
  openssl and held in a mode-600 temporary env file, deleted after verification.
- log_bin_trust_function_creators=1 is set only on disposable MySQL to support the
  existing trigger-based failure-injection suites. Full database tests clean only
  that disposable schema. Final migration metadata is in FlywayEvidence.txt.

## Commands actually executed

Host compile and initial reader tests used:

~~~sh
JAVA_HOME='/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home' \
  '/Applications/IntelliJ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  --batch-mode --no-transfer-progress -f backend/pom.xml -DskipTests compile
# Reader test selection:
# ... -Dtest=CanonicalTradingDataMessageReaderTests test
~~~

The disposable environment was created by the scoped temporary setup.sh; run.sh
expanded to this Docker invocation (env file contained only generated test secrets,
Q008…Q015_MYSQL_TEST_URL/USERNAME/PASSWORD and disposable MySQL setup settings):

~~~sh
docker run --rm --name q015-consumer-20260908-maven \
  --network q015-consumer-20260908 \
  --env-file /private/tmp/q015-consumer-verification/test.env \
  -v /Users/lukeh/Documents/workspace/codex/brokeros-risk:/workspace:ro \
  -v /Users/lukeh/Documents/workspace/codex/brokeros-risk/backend/target:/workspace/backend/target \
  -v brokeros-q011-verify-m2:/root/.m2 -w /workspace/backend \
  maven:3.9.9-eclipse-temurin-21-alpine mvn --batch-mode --no-transfer-progress <arguments>
~~~

Executed arguments in order:

1. -Dtest=CanonicalTradingDataMessageReaderTests,TradingDataKafkaConfigurationTests,Q015CanonicalKafkaMySqlTests test
2. The same focused selection after its compile repair.
3. test
4. clean package (failed during clean; see attempt ledger).
5. package (final full suite and JAR, successful).

All Q008 through Q015 URLs pointed to jdbc:mysql://mysql:3306/q015_consumer_test
with useSSL=false, allowPublicKeyRetrieval=true and serverTimezone=UTC. Username
was synthetic q015_test; no credential value is included here. Reproduction uses
the executable ReproduceVerification.sh in this package, generating fresh isolated
names/credentials and running test then package.

Additional commands: sh scripts/verify-static.sh; sh scripts/verify-kustomize.sh;
git diff --check; git diff --exit-code -- backend/src/main/resources/db/migration
backend/src/main/java/com/brokeros/risk/tradingdata/infrastructure/persistence
backend/src/main/java/com/brokeros/risk/tradingdata/domain/TradingDataEnvelope.java
backend/src/test/resources/q015/mt4-canonical-golden.jsonl; deployment and historical
review tracked-diff checks; node source/manifest/credential scan.

## Attempt ledger and warnings

- Initial compile: PASS. Initial seven reader tests: one failure because Jackson
  accepted a numeric accountRef as text; strict textual coercion repaired it.
- First Docker focused run: test compile failure from abstract/raw container type;
  inspection corrected to concurrent container with wildcard generics.
- Focused retry: 12/12 PASS; full backend test: 356/356 PASS.
- Final review added strict UTF-8/raw-NUL checks and UTF-16/depth/boundary cases;
  moved listener suppression onto the test classpath to keep deployed profiles active.
- clean package: clean failed deleting a bind mount under the read-only repository;
  generated contents had been cleared. package then rebuilt and reran all 356 tests
  successfully on the final code. No assertion was removed to obtain PASS.
- Expected error-level logs appear during injected database/quarantine failures and
  existing negative tests. These are not test failures. Flyway reports its MySQL
  8.4 support-ceiling warning; existing suites also emit framework shutdown/testing
  warnings. No live infrastructure guarantee follows from this result.

## Scope/security and cleanup

V9 and all migrations unchanged; no schema/index addition; payload remains BLOB
and byte[] without content parsing in service/JDBC. No SDK/native library reference
in changed Java, no other business-module Java or capability/bootstrap change.
Golden fixture is unchanged. SourceManifest.sha256 pins all 19 changed task files.

Temporary database credential was scanned against changed files, review files and
execution logs and was absent. Package token/private-key/symlink checks and archive
integrity are recorded in PackageValidation.txt. Surefire XML properties and raw
logs are not bundled because they may contain environment context; only selected
non-sensitive counts/migration evidence are included.

Cleanup succeeded: task MySQL container plus anonymous volume removed, task network
removed, Maven --rm containers exited, embedded broker contexts destroyed, temporary
credential file deleted. Existing Maven cache intentionally retained. No Git
staging/commit/push or modification to older timestamped review packages occurred.
