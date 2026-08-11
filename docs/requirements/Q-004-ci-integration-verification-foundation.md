# Q-004: CI and Integration Verification Foundation

## Status

Approved

## Background

Phase 0, Phase 0.5, and Phase 0.6 established the repository, engineering
foundation, and development standards. Their Maven checks pass, but the
repository previously had no initial Git commit and no repeatable CI evidence
for Docker Compose, real MySQL/Flyway startup, Redis, Kafka, or Kustomize
rendering. Those gaps make the first business migration unsafe to begin.

## Objective

Establish a reviewable Git baseline and the smallest repeatable CI/integration
verification loop for the existing engineering foundation. Q-004 verifies
infrastructure only; it does not create business behavior.

## Scope

- Audit the repository and create an initial Git baseline.
- Add a blocking Java 21 Maven CI workflow.
- Validate whitespace, shell syntax, Docker Compose semantics, and Kustomize
  base/test/prod rendering.
- Start the isolated Compose stack and verify MySQL, Flyway, Redis, Kafka, and
  the backend health endpoints.
- Verify exactly one successful V1 Flyway record, a stable checksum, idempotent
  restart behavior, and no business table.
- Add reusable repository-owned verification scripts, documentation, skill,
  lessons, and a complete Review Package.
- Classify reusable patterns without extracting a framework.

## Out of Scope

- Risk Case, Rule Engine, Account Control, formal Audit, authentication,
  authorization, API versioning, or any other business module.
- Business tables, V2 migration, business ResultCode, production Kafka topics
  or events, production Redis keys, or business Redis client code.
- MT4, MT5, BrokerPilot, oneZero, CRM, or other real external integrations.
- Flink, Python, Elasticsearch, MongoDB, API gateway, service mesh,
  microservices, CD, or production deployment automation.
- Shared framework/starter/common modules without multiple real consumers.

## Acceptance Criteria

1. The initial baseline commit exists and post-commit Git status contains no
   unignored file.
2. `mvn test` passes without reducing the existing 12-test suite.
3. `mvn package` passes and creates an ignored executable JAR.
4. `git diff --check` passes for local changes and CI checks the relevant commit
   range.
5. GitHub Actions supplies Java 21, read-only repository permission, Maven
   caching, blocking Maven checks, Kustomize rendering, and Compose/integration
   verification.
6. `docker compose config` exits successfully with ephemeral local/test
   credentials.
7. The isolated Compose stack reports MySQL, Redis, Kafka, and backend healthy.
8. `flyway_schema_history` contains exactly one successful V1 SQL migration
   with a non-null checksum, and restarting the backend does not add another V1
   row.
9. The application schema contains no table other than Flyway history.
10. Redis returns `PONG` and contains no business key; Kafka responds to a
    broker API connectivity command without creating a business topic.
11. Kustomize base, test, and production resources render successfully and the
    Secret reference remains an external contract.
12. The final Verification Matrix uses only PASS, FAIL, PARTIAL, or NOT
    EXECUTED and gives evidence for every PASS.
13. No prohibited business or technology scope is introduced.
14. Skill, Lessons Learned, Reusability Review, and the seven-file Review
    Package are complete.

## Verification Criteria

Required commands include:

```bash
git status
git diff --check
cd backend && mvn test
cd backend && mvn package
docker compose config
kubectl kustomize deploy/kubernetes/base
kubectl kustomize deploy/kubernetes/test
kubectl kustomize deploy/kubernetes/prod
```

The repository integration script must additionally query
`flyway_schema_history`, restart the backend, verify Redis and Kafka
connectivity, and fail on any unsuccessful core check. Missing tooling must be
reported as NOT EXECUTED, never PASS.

## Architecture Constraints

- Retain the Phase 1 modular monolith, one repository, one backend deployable,
  and adapter boundaries.
- Retain Maven, Spring Boot, MySQL, Redis, Kafka, Docker, Kubernetes, Flyway,
  and the existing Kustomize layout.
- CI verifies and tests only; it does not deploy.
- Do not add abstractions beyond small repository-owned scripts with a current
  local/CI consumer.

## Security Constraints

- Use only local/test/CI-isolated infrastructure and never connect to
  production MySQL, Redis, Kafka, or Kubernetes.
- Generate ephemeral Compose credentials at execution time; do not commit or
  print them.
- Keep Kubernetes Secret values external and commit only the Secret reference.
- Grant CI only `contents: read`, disable persisted checkout credentials, and
  provide no production Secret to pull-request code.
- Do not perform destructive production database or deployment operations.

## Reusability Constraints

- Classify verification patterns as platform-reusable, risk-specific, or not
  ready to extract.
- Do not create `brokeros-framework`, `brokeros-common`, `brokeros-starter`, or
  `brokeros-core`.
- Discover reuse in at least two real consumers before proposing extraction,
  unless a separate approved Requirement establishes another concrete reason.

## Review Requirements

- Preserve the Phase 0.6 Review Package under `review/archive/phase-0.6/`.
- Generate all seven current Review Package files.
- Include the eight Phase 0.6 compliance areas, Infrastructure Verification
  Impact, ADR required YES/NO, and Reusability Review.
- Do not mark the Review PASS while any implemented core check fails.
