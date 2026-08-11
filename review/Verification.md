# Q-004 Verification

## Verification Matrix

| Component | Status | Evidence |
|---|---|---|
| Git Baseline | PASS | Root commit `8bf42bc` exists; post-commit `git status --short` contained no unignored path. |
| Maven Build | PASS | `mvn package` exited 0 and produced the ignored executable JAR; package lifecycle ran all 12 tests. |
| Unit Tests | PASS | `mvn test`: 12 tests, 0 failures, 0 errors, 0 skipped. |
| CI | PARTIAL | Workflow exists, uses Java 21/read-only SHA-pinned Actions, and actionlint 1.7.12 exits 0; no remote runner execution exists. |
| Docker Compose Config | PASS | Checksum-verified Docker Compose v5.4.0 standalone `config --quiet` exited 0 and listed redis, kafka, mysql, backend. |
| Docker Startup | NOT EXECUTED | No Docker CLI/daemon is installed locally; infrastructure script exits 1 at its prerequisite check. |
| MySQL | NOT EXECUTED | Requires the unavailable isolated Docker runtime. |
| Flyway | NOT EXECUTED | Real MySQL history/checksum/restart assertions are implemented but have not run. |
| Redis | NOT EXECUTED | Runtime PING/DBSIZE assertions are implemented but have not run. |
| Kafka | NOT EXECUTED | Broker API connectivity assertion is implemented but has not run. |
| Kubernetes Base Render | PASS | Checksum-verified kubectl v1.36.3 / Kustomize v5.8.1 rendered base and contract checks passed. |
| Kubernetes Test Render | PASS | The same verified tool rendered test; test profile/resource/Secret checks passed. |
| Kubernetes Prod Render | PASS | The same verified tool rendered prod; prod profile/resource/Secret checks passed. |
| Static Checks | PASS | Range-aware `git diff --check`, shell syntax, migration boundary, YAML/POM parsing, secret scan, and actionlint passed. |

## Build Result

PASS

Executed:

```bash
cd backend
mvn test
mvn package
```

Both commands exited 0. Each run reported 12 tests, 0 failures, 0 errors, and 0
skipped. `mvn package` created
`backend/target/brokeros-risk-backend-0.1.0-SNAPSHOT.jar`; `target/` remains
ignored.

The first pre-baseline sandboxed `mvn test` failed because the environment
blocked Mockito/Byte Buddy JVM attachment and Surefire temporary files. The
unchanged command passed outside that sandbox. This is retained as environment
evidence, not recorded as a product-code defect.

## Initial Git Baseline

PASS

Executed before Q-004 implementation:

```bash
git status --short --ignored
git diff --check
git diff --cached --check
git commit -m "chore: establish BrokerOS Risk project baseline"
git status --short --ignored
git log -1 --oneline
git diff --stat
```

Result: `8bf42bc chore: establish BrokerOS Risk project baseline`. Ignored
`.DS_Store` files and `backend/target/` stayed outside the commit. The baseline
contains no detected credential, local environment file, IDE state, or build
artifact.

## CI Validation

PARTIAL

Executed locally:

```bash
actionlint .github/workflows/ci.yml
sh scripts/verify-static.sh 8bf42bc
```

The checksum-verified actionlint 1.7.12 binary exited 0. Static/range/shell and
migration-boundary checks pass. The selected GitHub Actions workflow has not run
because this repository has no remote, so CI is not marked PASS.

## Docker Validation

Semantic config: PASS. Runtime: NOT EXECUTED.

Executed with a checksum-verified official Docker Compose v5.4.0 standalone
binary because the host has no Docker installation:

```bash
docker-compose --profile app -f docker-compose.yml config --quiet
docker-compose --profile app -f docker-compose.yml config --services
sh scripts/verify-infrastructure.sh
```

The config command exited 0 and reported `redis`, `kafka`, `mysql`, and
`backend`. The infrastructure script exited 1 with `Docker with Compose v2 is
required.` before generating credentials or changing external state. Container
startup, health, volumes, logs, and restart behavior are NOT EXECUTED.

## MySQL and Flyway Validation

NOT EXECUTED — no local Docker daemon or real isolated MySQL instance is
available. The script contains blocking queries for version, description, type,
script, checksum, installed time, success, exactly one V1, no business table,
and exactly one V1 after backend restart. These assertions are not evidence
until executed.

DO NOT START FIRST BUSINESS MIGRATION.

## Redis and Kafka Validation

NOT EXECUTED — both require the unavailable isolated Docker runtime. The script
contains blocking Redis PING/empty-keyspace and Kafka broker API assertions and
creates no business key/topic/event.

## Kubernetes Validation

PASS for render; no cluster deployment was requested or attempted.

Executed:

```bash
kubectl version --client
sh scripts/verify-kustomize.sh
```

Official kubectl v1.36.3 was downloaded to `/private/tmp`, checked against the
official SHA256, and reported Kustomize v5.8.1. Base, test, and prod rendered and
all resource, profile, label, and Secret-reference assertions passed.

## Static Configuration and Security Checks

PASS for executable checks.

Executed:

```bash
git diff --check
sh -n scripts/*.sh
ruby -e '<parse all repository YAML files>'
ruby -e '<parse backend/pom.xml>'
rg '<secret/private-key/provider-token patterns>'
find '<local environment/certificate/private-key/build-artifact patterns>'
actionlint .github/workflows/ci.yml
```

Results: 15 YAML files and the POM parse; scripts are syntactically valid; no
business DDL or Hibernate schema generation is present; no committed credential
value/private key/certificate is detected. `gitleaks` and `trufflehog` are not
installed, so their scans were not executed.
