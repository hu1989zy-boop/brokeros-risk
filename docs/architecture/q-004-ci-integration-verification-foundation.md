# Q-004 CI and Integration Verification Architecture

## Purpose

Q-004 closes verification gaps in the existing engineering foundation. It
adds no business capability and does not change BrokerOS Risk from a single
Spring Boot modular monolith.

## Current Verification Gaps

Before Q-004, Maven tests and packaging had passed, but the repository lacked a
Git baseline, automated CI, real Compose semantic/startup evidence, real
MySQL/Flyway evidence, Redis/Kafka connectivity evidence, and Kustomize render
evidence. The initial baseline is now commit `8bf42bc`.

The current local host does not provide Docker, kubectl, or kustomize. Local
results must therefore distinguish executable checks from checks delegated to
the configured CI runner.

## CI Architecture

Use one GitHub Actions verification workflow for push and pull request events.
It uses an Ubuntu runner, Java 21, Maven dependency caching, Docker Compose
provided by the runner, and a pinned kubectl setup action. The workflow runs:

1. repository/commit-range whitespace and shell syntax checks;
2. `mvn test`;
3. `mvn package`;
4. base/test/prod Kustomize rendering;
5. isolated Docker Compose and infrastructure verification.

Every core step is blocking. The workflow has read-only repository permission,
does not persist checkout credentials, accepts no production Secret, and never
deploys. Third-party action revisions are pinned to reviewed commit SHAs, with
release labels retained as comments for maintainability.

## Local Validation Flow

Repository-owned POSIX shell scripts are the common local/CI entry points:

- `scripts/verify-static.sh` checks whitespace for the working tree and, when
  supplied, the relevant commit range; it also parses all repository shell
  scripts with `sh -n`.
- `scripts/verify-kustomize.sh` renders base, test, and production overlays to a
  temporary directory and verifies the expected resource and Secret contracts.
- `scripts/verify-infrastructure.sh` owns one isolated Compose project, creates
  ephemeral credentials in memory, verifies the stack, and removes only that
  project's containers and volumes on exit.

The scripts fail when a required command is unavailable or a core assertion
fails. Documentation and the Review Package translate local missing-tool
failures to NOT EXECUTED rather than silently ignoring them.

## Docker Verification Flow

The infrastructure script first runs `docker compose config`, then builds and
starts the existing optional backend `app` profile. It waits for the MySQL,
Redis, Kafka, and backend health checks and reports service logs on failure.
Host port bindings are limited to loopback for local/test safety. Named volumes
remain part of the Compose contract; the script uses a unique Compose project
and removes only its isolated volumes after verification.

The existing developer-productivity decision remains unchanged: normal local
development may run only infrastructure in Compose and the backend through
Maven; the backend remains in the optional `app` profile for full-stack
verification.

## MySQL and Flyway Flow

```text
Spring Boot backend
  -> application-owned DataSource
  -> isolated MySQL brokeros_risk schema
  -> automatic Flyway startup
  -> flyway_schema_history
  -> V1__initial_schema.sql
```

The verification queries version, description, type, script, checksum,
installed time, and success without printing credentials. It asserts exactly
one successful V1 with a checksum and no application table other than Flyway
history. The backend is restarted and the assertion is repeated to prove
idempotence. Hibernate schema generation is absent and prohibited.

## Redis and Kafka Boundary

Redis validation is infrastructure-only: health, unauthenticated local/test
behavior, `PING`, and an empty keyspace. Compose exposes Redis only on loopback.
No business key or Redis application feature is introduced.

Kafka validation checks container health and broker API connectivity. It does
not create a topic, producer, consumer, or event schema. Compose exposes only
the host listener on loopback; the backend uses the internal listener.

## Kubernetes Render Flow

`kubectl kustomize` renders `base`, `test`, and `prod`. Kustomize itself proves
that overlays and patches apply. Additional assertions confirm Deployment,
Service, ConfigMap, environment profile, labels/selectors, and the
`brokeros-risk-secrets` reference. Q-004 does not require or perform cluster
deployment.

## Secret Boundary

- Compose passwords are generated per verification run and remain process
  environment values.
- `.env.example` contains no values; populated `.env` remains ignored.
- Kubernetes commits only the `brokeros-risk-secrets/db-password` reference.
- CI receives no production credential and exposes no deployment permission.
- Verification commands never enable shell tracing or echo Secret values.

## Git Baseline Impact

Commit `8bf42bc` is the first project baseline. All Q-004 files and changes are
therefore visible through normal Git status and diff statistics. The current
Review Package preserves the earlier Phase 0.6 package before replacement.

## Architecture Impact

- Risk Case: no impact.
- Rule Engine: no impact.
- Account Control: no impact.
- Audit business implementation: no impact.
- Kafka: infrastructure verification only; no formal topic/event.
- Redis: infrastructure verification only; no business key.
- MT4/MT5/BrokerPilot/oneZero/CRM: no impact.
- API and ResultCode contracts: no impact.
- Database: V1 is executed and inspected; no migration or business table is
  added.

## Reusability Considerations

The blocking CI sequence, evidence matrix, isolated Compose verification,
Flyway idempotence check, and Kustomize render pattern are potentially reusable
across BrokerOS products. Resource names, image names, schema name, ports, and
Spring profiles are Risk-specific. There is only one real consumer, so these
patterns are Not Ready To Extract into a shared framework.

## ADR Evaluation

ADR required: YES. Selecting GitHub Actions and pinned repository-owned
verification scripts as the durable CI mechanism affects every future change
and warrants ADR-006. The modular-monolith and deployment architecture do not
otherwise change.
