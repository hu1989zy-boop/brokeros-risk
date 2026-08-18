# Q-005 Verification

## Final Verdict

PARTIAL

Implementation, Maven, dependency, static, security, and Kubernetes checks
pass. Docker/Compose and real MySQL/Flyway/Redis/Kafka/backend runtime checks are
`NOT EXECUTED` because the local host has no Docker CLI or daemon. Existing
Q-004 CI success proves the gate itself, not the uncommitted Q-005 revision.

## Environment

- Date: 2026-08-13 (Asia/Shanghai)
- Branch: `main`
- Baseline HEAD: `77229a2`
- Project target: Java 21
- Local Maven runtime: Java 23.0.2, compiling with `release 21`
- Docker: unavailable
- Local kubectl: unavailable; temporary official v1.36.3 binary was downloaded,
  SHA-256 verified, used, and removed

## Verification Matrix

| Component | Status | Evidence |
|---|---|---|
| Compilation | PASS | `mvn package` compiled 9 production and 4 test source files for Java 21. |
| Maven tests | PASS | 19 tests, 0 failures, 0 errors, 0 skipped. |
| Maven package | PASS | Executable `brokeros-risk-backend-0.1.0-SNAPSHOT.jar` produced under ignored `backend/target/`. |
| Request ID | PASS | Generation, valid preservation, malformed/control, oversized, multi-value, success, and error cases passed. |
| W3C Trace Context | PASS | Known inbound Trace ID continued with a distinct server Span ID. |
| MDC cleanup/isolation | PASS | Sequential and barrier-backed 4-request concurrency tests passed; worker MDC values were null after completion. |
| Dependency boundary | PASS | One `micrometer-tracing-bridge-otel:1.5.12`; no Zipkin/Jaeger/OTLP/exporter/ELK/Prometheus/Grafana dependency. |
| API regression | PASS | Existing unified API/error, Actuator, OpenAPI, and Swagger tests passed unchanged. |
| Flyway static boundary | PASS | Unchanged V1 is still the only migration and contains no business DDL. |
| Static checks | PASS | Repository whitespace, untracked-text, shell syntax, migration, and Hibernate checks passed. |
| Secret scan | PASS | Private-key, token, literal-password, and credential-pattern checks found no introduced Secret. |
| Kubernetes base | PASS | Render/resource/Secret-reference contract passed. |
| Kubernetes test | PASS | Render/profile/resource/Secret-reference contract passed. |
| Kubernetes prod | PASS | Render/profile/resource/Secret-reference contract passed. |
| Docker Compose config | NOT EXECUTED | Docker CLI is not installed. |
| Docker startup/backend image | NOT EXECUTED | Docker CLI/daemon is unavailable. |
| Real MySQL/Flyway | NOT EXECUTED | Requires the isolated Docker gate for the Q-005 image. |
| Real Redis/Kafka | NOT EXECUTED | Requires the isolated Docker gate for the Q-005 image. |
| Q-005 GitHub Actions | NOT EXECUTED | No commit/push was requested or performed. |

## Commands Executed

### Maven tests

```bash
cd backend
mvn --batch-mode --no-transfer-progress test
```

Result: PASS — 19 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS.

### Maven package

```bash
cd backend
mvn --batch-mode --no-transfer-progress package
```

Result: PASS — 19 tests passed; executable JAR produced; BUILD SUCCESS.

### Dependency and exporter boundary

```bash
cd backend
mvn --batch-mode --no-transfer-progress dependency:tree \
  -Dincludes=io.micrometer:micrometer-tracing-bridge-otel,io.micrometer:micrometer-tracing,io.opentelemetry
mvn --batch-mode --no-transfer-progress dependency:tree | \
  rg -i 'zipkin|jaeger|otlp|opentelemetry-exporter|logstash|elasticsearch|prometheus|grafana'
```

Result: PASS. Resolved bridge/tracing version is 1.5.12 with OpenTelemetry 1.49.0
API/SDK context components. The prohibited exporter/backend scan returned no
match.

### Static checks

```bash
GIT_CONFIG_COUNT=1 \
GIT_CONFIG_KEY_0=core.excludesFile \
GIT_CONFIG_VALUE_0=/private/tmp/brokeros-risk-q005-git-excludes \
sh scripts/verify-static.sh
git diff --check
```

Result: PASS. The temporary exclude contained only
`review/review-history/`, ensuring the user-owned zip archive was not read by
the untracked-text check. The archive itself was not inspected or changed.

Additional migration/dependency/secret checks passed:

```bash
git diff --exit-code -- backend/src/main/resources/db/migration
rg <prohibited-observability-artifact-patterns> backend/pom.xml
rg <private-key-token-credential-patterns> <repository excluding .git, target, review-history>
```

### Kubernetes rendering

```bash
# Official darwin/arm64 kubectl v1.36.3 plus kubectl.sha256 downloaded to /private/tmp
shasum -a 256 <temporary-kubectl>
PATH=<verified-temporary-kubectl-directory>:$PATH sh scripts/verify-kustomize.sh
```

Result: PASS — checksum verification plus base, test, prod, and contract renders
all passed. Temporary files were removed.

### Infrastructure gate

```bash
sh scripts/verify-infrastructure.sh
```

Result: NOT EXECUTED. Preflight emitted
`FAIL [preflight] Docker with Compose v2 is required.` and exited 1 before any
resource was created. This is an environment limitation, not a product test
failure and not a PASS.

## Test Inventory

- `RequestCorrelationIntegrationTests`: 7
- `BrokerOsRiskApplicationTests`: 7
- `FlywayMigrationTests`: 1
- `GlobalExceptionHandlerTests`: 4
- Total: 19

The known Mockito/Byte Buddy dynamic-agent warning is non-blocking but remains
technical debt for a future test-foundation task.

## Closure Condition

Run the existing GitHub Actions workflow or the same repository scripts on a
Docker-capable approved host for the current Q-005 revision. Required PASS
evidence includes Compose config/startup, backend image/health, MySQL/Flyway V1
and no-business-table checks, Redis empty-keyspace check, Kafka broker API
connectivity without topic creation, fatal-log scan, and isolated cleanup.
