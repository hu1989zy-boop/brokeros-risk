# Q-004 Architecture Review

## Review Result

PARTIAL — DO NOT MARK PASS. No unresolved development-standards violation was
found in the implementation, but Q-004 acceptance requires real
MySQL/Flyway/Redis/Kafka verification. Those checks and the selected CI run are
NOT EXECUTED because this host has no Docker daemon and the repository has no
remote. The first business migration remains blocked.

## Architecture Decision

ADR required: YES. ADR-006 is Accepted and records GitHub Actions plus
repository-owned POSIX verification scripts as the durable CI mechanism. This
is a cross-cutting decision affecting all future Requirements. ADR-001 through
ADR-005 remain unchanged.

The runtime architecture remains one Java/Spring Boot modular-monolith
deployable with MySQL, Redis, and Kafka infrastructure. Docker Compose remains
the local/test stack, the backend remains in its optional `app` profile, and
Kustomize base/test/prod remains the Kubernetes layout.

## Development Standards Compliance

### AGENTS.md compliance

Evidence: `AGENTS.md`, Q-001 through Q-003, architecture documents, ADR-001
through ADR-005, `docs/skills/development-standards.md`, and the Phase 0.6
lesson were read before Q-004 implementation. Q-004 followed Requirement →
Architecture → ADR → Implementation → Verification → Skill → Lesson → Review.
No prohibited technology or business module was added.

### Architecture compliance

Evidence: production Java source is unchanged and still contains only the
application, API foundation, configuration, exception boundary, and health
endpoint. `backend/pom.xml` is unchanged. No service split, repository split,
new runtime dependency, vendor adapter, or speculative framework exists.

Impact check: Risk Case — none; Rule Engine — none; Account Control — none;
formal Audit — none; MT4/MT5/BrokerPilot/oneZero/CRM — none. Kafka and Redis
changes are verification-only. The application API and runtime module layout do
not change.

### ADR compliance

Evidence: ADR-001's approved stack and modular monolith, ADR-002's adapter and
external-data isolation, ADR-003's Flyway/API foundation, ADR-004's optional
Compose backend and Kustomize layout, and ADR-005's governance gate are all
preserved. ADR-006 explicitly records the new CI provider and validation
strategy; no durable decision is left implicit.

### API standard compliance

Evidence: no controller, DTO, `ApiResponse`, `ErrorResponse`, `ResultCode`,
`BusinessException`, or `GlobalExceptionHandler` source changed. The existing
12 tests, including application API, Actuator, OpenAPI, and error-envelope
coverage, pass. Framework-native Actuator/OpenAPI formats remain unchanged.

### Database standard compliance

Evidence: `V1__initial_schema.sql` remains the sole migration, contains no
business DDL, and the static script blocks additional foundation migrations,
business DDL, and Hibernate schema-generation settings. No table, column,
index, migration, or datasource behavior was added. Real MySQL/Flyway execution
is NOT EXECUTED, so this category cannot supply Q-004's required runtime PASS.

### Security standard compliance

Evidence: CI permissions are only `contents: read`; checkout sets
`persist-credentials: false`; external Actions are pinned to full commit SHAs;
there is no deployment step or production Secret input. Compose verification
generates passwords in memory without shell tracing or output, uses a unique
project name, and targets only its own volumes. Local ports bind to
`127.0.0.1`. Kubernetes commits only the external
`brokeros-risk-secrets/db-password` reference. Repository-wide secret patterns
and sensitive filenames were rechecked; no credential value or private key was
found.

### Auditability compliance

Evidence: no critical risk action or business transition was implemented, so
no runtime Audit record is required. Engineering traceability improved through
initial commit `8bf42bc`, range-aware diff checks, explicit command evidence,
component statuses, and preserved Review Packages. Generated credentials are
intentionally excluded from logs.

### Skill compliance

Evidence: the existing development-standards skill guided preflight and Review.
`docs/skills/ci-integration-verification.md` now provides reusable verification
matrices, isolation, Flyway, Redis/Kafka, Kustomize, CI blocking, evidence, and
reuse guidance. The skills index and Q-004 lesson were updated. The skill does
not act as a changelog or invent framework code.

## Infrastructure Verification Impact

- CI changes: one GitHub Actions workflow for push/pull request, Java 21,
  Maven cache, static checks, Kustomize, Compose, and isolated integration
  verification. It has no CD capability.
- Docker changes: host ports now bind to loopback; normal infrastructure-only
  development and the optional `app` profile remain. The script uses an
  isolated project and ephemeral credentials.
- MySQL changes: configuration/schema are unchanged; the script will assert
  health, V1 row metadata/checksum, no business table, and restart idempotence.
- Redis changes: configuration is unchanged apart from loopback host binding;
  the script will assert PONG and empty keyspace without creating a key.
- Kafka changes: configuration is unchanged apart from loopback host binding;
  the script will call broker API versions without creating a topic or event.
- Kubernetes changes: manifests are unchanged; all base/test/prod renders and
  expected resource/profile/Secret contracts pass.
- Runtime behavior changes: none in the application. Local network exposure is
  reduced to loopback.
- Compatibility impact: application/API/database contracts are unchanged.
  Developers connecting from another host can no longer use Compose-published
  ports, which is an intentional local security boundary.

## Reusability Review

### Platform-Reusable

- Requirement-first CI pattern with blocking Maven and deployment checks.
- Commit-range whitespace validation and evidence matrix.
- SHA-pinned, read-only, verification-only workflow posture.
- Unique Compose project plus ephemeral credential isolation.
- Flyway history/checksum/idempotence and no-business-table assertions.
- Kustomize base/overlay render pattern and Review/Verification structure.

### Risk-Specific

- `brokeros-risk` image/resource/config/Secret names.
- `brokeros_risk` schema, application endpoints, service names, ports, and
  Spring profiles.
- Current Compose images and health commands.

### Not Ready To Extract

The platform-reusable patterns have only BrokerOS Risk as a real consumer.
There is no proven cross-repository duplication and no approved framework
Requirement. No `brokeros-framework`, `brokeros-common`, `brokeros-starter`, or
`brokeros-core` was created. Discover reuse first; extract second.

## Technical Debt and Recommendations

- Execute the workflow on a Docker-capable GitHub runner or run
  `scripts/verify-infrastructure.sh` on an equivalent host.
- Capture the real `flyway_schema_history` row and repeat the Review gate.
- Do not approve Q-004 or start the first business migration until Docker,
  MySQL/Flyway, Redis, and Kafka rows are PASS.
- Align the local Maven runtime to Java 21 and address Mockito's future
  dynamic-agent restriction before it becomes a build failure.
