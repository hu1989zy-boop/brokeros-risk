# CI and Integration Verification

## When to Use

Use this skill for CI changes, deployment/configuration validation, the first
database migration gate, or any Requirement that changes Maven, Docker Compose,
Flyway, MySQL, Redis, Kafka, or Kustomize behavior. Use it together with
`development-standards.md`; it does not replace Requirement and architecture
preflight.

## Preflight

1. Read `AGENTS.md`, the active Requirement and architecture document, all
   Accepted ADRs, `development-standards.md`, and relevant lessons.
2. Confirm that verification targets only local/test/CI-isolated resources.
3. Check the current Git baseline and working tree before producing evidence.
4. Identify required tools. A missing tool is NOT EXECUTED, never PASS.
5. Confirm that no business table, topic, Redis key, event, or integration is
   being created merely to prove infrastructure connectivity.

## Verification Matrix

Record one row per material component using only PASS, FAIL, PARTIAL, or NOT
EXECUTED. Every PASS names the command, exit result, and observable evidence.
Typical rows are Git Baseline, Maven Build, Unit Tests, CI, Compose Config,
Docker Startup, MySQL, Flyway, Redis, Kafka, Kubernetes base/test/prod renders,
and Static Checks.

Do not collapse semantic validation and runtime validation. For example,
Compose config can PASS while Docker Startup is NOT EXECUTED.

## Static and Maven Pattern

Run:

```bash
sh scripts/verify-static.sh [base-commit]
cd backend
mvn test
mvn package
```

The static script checks the working/staged diff, a supplied commit range,
shell syntax, the one-migration foundation boundary, absence of business DDL,
and absence of Hibernate schema-generation configuration. Maven test and
package are separate blocking checks even though package repeats the tests.

## Docker Validation Pattern

1. Generate ephemeral local/test credentials in memory; do not store or print
   them.
2. Run Compose semantic validation before startup.
3. Use a unique Compose project name so cleanup cannot target a developer's
   normal stack.
4. Start the optional backend profile and wait for every declared health check.
5. Print scoped service logs when an assertion fails.
6. Remove only the isolated project's containers and volumes on exit.

Never weaken health checks to make startup pass. If the host has a Compose CLI
but no daemon, record semantic config and startup separately.

## MySQL and Flyway Validation

Use a real non-production MySQL instance. Query and record:

- version;
- description;
- type;
- script;
- checksum;
- installed time;
- success.

Assert one successful V1 row, a non-null checksum, no table other than
`flyway_schema_history`, and one V1 row after backend restart. Do not create V2
or a committed test migration to make the check easier. Do not start the first
business migration until this verification is PASS.

## Redis and Kafka Connectivity

Redis infrastructure verification may use container health, unauthenticated
local/test `PING`, and `DBSIZE`. The keyspace must remain empty; do not create a
formal key.

Kafka infrastructure verification may use container health and the broker API
versions command. Do not create a formal topic, producer, consumer, or business
event. If a temporary topic is ever unavoidable, its test-only lifecycle must
be explicit and isolated.

## Kustomize Validation

Render base, test, and production with `kubectl kustomize` or `kustomize build`.
Confirm that patches apply and that Deployment, Service, ConfigMap,
labels/selectors, profiles, and external Secret references are present. A
successful render is not evidence of cluster deployment.

## CI Blocking Rules

- Java 21, Maven test, Maven package, relevant whitespace checks, Compose
  semantics, executed infrastructure assertions, and Kustomize rendering are
  blocking.
- Do not use `continue-on-error`, `|| true`, or an equivalent around a core
  check.
- CI receives no production Secret and has no deployment permission.
- Pin external Actions to reviewed commit SHAs and retain readable release
  comments for updates.
- Pull-request code must not gain access to privileged credentials.

## Evidence Checklist

- Exact command and tool version where relevant.
- Exit status or successful test count.
- Compose service list and health state without environment values.
- Flyway row metadata without credentials.
- Redis PONG/key count and Kafka connectivity command.
- Render result for each Kubernetes environment.
- Git commit/diff/status evidence.
- Explicit environment limitation for every NOT EXECUTED row.

## Common Mistakes

- Calling YAML parsing a Compose semantic PASS.
- Calling Kustomize render a cluster deployment PASS.
- Marking MySQL/Flyway PASS from a resource-only unit test.
- Printing `docker compose config` after Secret interpolation into public logs.
- Reusing a normal Compose project name and deleting developer volumes.
- Creating business tables, topics, or keys solely for connectivity checks.
- Letting an optional cleanup failure hide the original core-check result.
- Marking a workflow PASS before it has executed on its selected CI provider.

## Reusability Guidance

The verification sequence, evidence matrix, isolation pattern, and blocking
rules are platform-reusable candidates. Product resource names, schemas, images,
profiles, and ports are product-specific. Until at least two real systems use
the same implementation, classify the scripts as Not Ready To Extract and keep
them in the product repository.
