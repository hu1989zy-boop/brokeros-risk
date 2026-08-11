# Phase 0.5 Outstanding Items

## Remaining Work

- Run `docker compose config` and the `app` profile on a Docker-capable machine.
- Render both Kubernetes overlays with `kubectl kustomize` or `kustomize build`.
- Run Flyway against a real MySQL instance and inspect `flyway_schema_history`.
- Provision the production `brokeros-risk-secrets` Secret through the approved
  secret-management process before deployment.

## Known Issues

- The host `java` command points to Java 8, while Maven uses Java 23 and compiles
  with `--release 21`. The build is valid, but aligning the host default JDK to
  Java 21 would reduce developer confusion.
- Tests intentionally disable Flyway in the Spring application context because
  no MySQL test instance is available locally.
- Mockito emits a dynamic-agent warning under the local Java 23 runtime; it does
  not fail the tests but should be monitored before later JDK upgrades.

## Deferred Work

- CI pipeline and automated deployment-manifest validation.
- MySQL-backed migration integration testing.
- Authentication, authorization, API versioning, and production OpenAPI exposure
  policy; these require approved requirements.
- All formal risk, account-control, external-integration, and frontend modules.

## Risks

- A future migration could pass unit tests but fail on MySQL until integration
  coverage is added.
- Result-code changes can become client compatibility changes after external
  consumers exist.
- Local Compose credentials must never be reused outside local/test environments.

## Suggested Next Phase

After architect approval, either establish a small CI/integration-verification
phase or approve the first narrowly scoped business requirement. Before any
business table is introduced, prioritize MySQL-backed Flyway verification and
automated Compose/Kustomize checks.
