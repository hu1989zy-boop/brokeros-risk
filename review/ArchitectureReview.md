# Q-006 Architecture Review

## Review Result

PARTIAL — ARCHITECTURE AND DEVELOPMENT STANDARDS PASS; CURRENT-REVISION
DOCKER/CI RUNTIME GATE PENDING

No architecture or standards violation was found. The only incomplete item is
operational evidence: the local host has no Docker CLI and no committed Q-006
revision exists for the approved GitHub Actions fallback.

## Architecture Decision

ADR required: YES.

Accepted ADR-008 records the durable cross-module and deployment contract:
Spring Boot Externalized Configuration is the sole mechanism, framework-owned
properties remain native, real BrokerOS-owned settings use
`brokeros.risk.<capability>` with immutable typed startup validation,
configuration is startup-bound, and Secret values remain external and
unexposed.

The application remains one Java/Spring Boot feature-first modular-monolith
deployable. Q-006 adds no production package or runtime component.

## Why These Decisions Were Made

The repository already delegates datasource, Redis, Kafka, Flyway, server,
management, logging, tracing, and SpringDoc configuration to Spring Boot and
library binders. Wrapping them would duplicate ownership and create contracts
BrokerOS does not own.

Repository inspection found no real BrokerOS-owned runtime setting. Creating an
empty `BrokerProperties` or a fake test group would add abstraction without a
consumer. The approved YAGNI decision therefore establishes rules, catalog, and
verification now, while deferring the first production properties type until a
concrete approved capability needs it.

Startup-bound configuration fits the current deployment model. Dynamic refresh
would require authorization, auditing, rollout, rollback, atomicity, failure,
and multi-instance consistency decisions absent from Q-006.

## Architecture Impact

| Area | Impact |
| --- | --- |
| Runtime | No production Java, dependency, runtime property, or new process. |
| Packages | One test-only `com.brokeros.risk.config` package; no production package or restructure. |
| Configuration | Existing keys/values/profiles/aliases unchanged; their contract is cataloged and tested. |
| API | No endpoint, response, header, ResultCode, exception, validation DTO, OpenAPI, or Actuator format change. |
| Database/Flyway | No migration, schema, entity, repository, SQL, DDL, or DML change. |
| Redis | No key, TTL, cache, business data, or connectivity behavior change. |
| Kafka | No topic, event, producer, consumer, or auto-creation change. |
| Logging/tracing | Existing Logback, Request ID, W3C tracing, MDC, and sensitive-log rules unchanged. |
| Docker/Kubernetes | Existing sources inspected and rendered; no image, manifest, overlay, Secret object, or topology change. |
| CI | Existing workflow and scripts reused; no workflow or gate change. |
| External systems | No MT4, MT5, CRM, database, SDK, or adapter implementation. |
| Risk business | No Risk Case, Rule Engine, Workflow, Account Control, Audit Module, or RBAC. |

## Implementation Review

The new integration test uses Spring Boot's existing test foundation and
`ConfigDataApplicationContextInitializer` to resolve the repository's actual
base/test/prod files. It deliberately does not start the full application or
external services. Host environment and system-property sources are removed in
the isolated runner so missing/invalid cases cannot depend on a developer's
shell.

The catalog-coverage test extracts environment placeholders from the actual
application YAML and Compose file, plus environment names from Kubernetes YAML,
then requires every alias to appear in the catalog. A runtime-generated
synthetic sensitive value verifies that an unrelated typed-conversion failure
does not include the value, without committing credential-like test data.

## Development Standards Compliance

### AGENTS.md compliance

Evidence checked: root `AGENTS.md`, approved Q-006 Requirement, Q-006
architecture/Gap Analysis/plan, ADR-001 through ADR-008, architecture documents,
development standards, applicable repository skills, recent Q-004/Q-005
lessons, production/test source, deployment sources, and the final change set.
The sequence was design approval → Accepted ADR-008 → smallest implementation →
tests/docs/skill/lesson → Review. No formal business behavior was inferred from
chat, and no prohibited Phase 1 technology or module was introduced.

The protected `review/review-history/` archive was excluded from static and tree
generation and was not read, modified, deleted, staged, or committed.

### Architecture compliance

Evidence: `git diff --exit-code` over production Java, runtime resources,
`backend/pom.xml`, Compose, Kubernetes, CI, scripts, and Flyway reports no Q-006
change. The only Java addition is a focused integration test. The feature-first
modular monolith, one deployable, approved Java/Spring/MySQL/Redis/Kafka/Docker/
Kubernetes stack, adapter isolation, and risk detection/action boundary remain
unchanged. Scans found no Flink, Python, Elasticsearch, microservice, service
mesh, horizontal package restructure, or external database access.

### ADR compliance

Evidence: ADR-001 modular monolith/stack, ADR-002 system isolation, ADR-003 API/
Flyway/Logback foundation, ADR-004 deployment layout, ADR-005 standards
authority, ADR-006 CI gates, and ADR-007 tracing remain unchanged. ADR-008 has
Status Accepted and contains Context, Decision, Alternatives, and Consequences.
The implementation matches it: no parallel configuration system, no native-key
wrapper, no empty application group, startup-bound semantics, external Secrets,
and unexposed Actuator configuration endpoints.

### API standard compliance

Evidence: no production controller, DTO, `ApiResponse`, `ErrorResponse`,
`ResultCode`, `BusinessException`, `GlobalExceptionHandler`, OpenAPI, or Actuator
contract changed. Existing API/exception/health/OpenAPI tests are included in
the 26-test PASS. The configuration test confirms Actuator exposure remains
exactly `health,info`, so framework-managed `env` and `configprops` remain
unavailable over HTTP.

### Database standard compliance

Evidence: `git diff --exit-code -- backend/src/main/resources/db/migration`
passes; unchanged `V1__initial_schema.sql` remains the only migration and has no
business DDL. No entity, repository, SQL, table, column, index, constraint,
data migration, Hibernate schema setting, or direct external database operation
was added. Framework-owned datasource and Flyway properties remain unwrapped.

### Security standard compliance

Evidence checked: `.gitignore`, `.env.example`, base/test/prod YAML, Compose,
Kubernetes Secret references, logging standards, catalog, test source, and
credential/private-key/token scans excluding `.git`, ignored build output, and
the protected archive. No Secret value or unsafe production default was added.
The test uses a runtime-generated synthetic value and asserts it is absent from
the diagnostic. Documentation prohibits logging or reviewing values. Actuator
`env`/`configprops` remain unexposed. No remote configuration/Secret product,
authentication token, or full authorization header handling was introduced.

### Auditability compliance

Evidence: Q-006 creates no critical action, mutable policy, runtime
administration, business decision, state transition, command execution, or
Audit module, so no new audit persistence is required. Startup configuration is
explicitly immutable. Dynamic or administrable configuration remains excluded
because it would require actor, reason, before/after, timestamp, approval,
rollback, and consistency design under a later Requirement.

### Skill compliance

Evidence: `docs/skills/development-standards.md` governed preflight and closure.
`docs/skills/configuration-management.md` captures the actual ownership test,
YAGNI rule, catalog contract, startup/Secret rules, deterministic host-source
isolation, source-derived alias coverage, common mistakes, and validation
checklist. It is indexed by `docs/skills/README.md`. The Q-006 lesson records
the actual host-environment determinism review finding and retest rather than an
invented incident.

## Technical Debt and Recommendations

- Blocking for final PASS: execute the existing Docker infrastructure gate on
  the current Q-006 revision, locally or through the approved GitHub Actions
  path, then refresh the Review with immutable run/commit evidence.
- Non-blocking: local Maven runs on Java 23 while compiling release 21; CI must
  continue verifying Java 21.
- Non-blocking: Mockito/Byte Buddy reports its known future dynamic-agent warning
  during tests; no test fails.
- Operational risk: correct production behavior still depends on explicit
  `prod` profile activation. Q-006 documents/tests the contract but does not
  invent environment admission or authorization.
- Deferred: a first application-owned properties type, Secret provider,
  dynamic configuration, and business policy configuration require a real
  approved Requirement and appropriate architecture/ADR review.

Do not interpret deferred candidates as approved Q-007 scope. Q-006 is ready
for architect review of implementation and must remain PARTIAL until its runtime
gate is evidenced.
