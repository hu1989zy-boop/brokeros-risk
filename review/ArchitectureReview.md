# Q-005 Architecture Review

## Review Result

PARTIAL — ARCHITECTURE AND STANDARDS REVIEW PASS; RUNTIME GATE PENDING

No architecture or development-standards violation was found. Overall Q-005
cannot be marked PASS because the Docker-capable infrastructure verification
has not executed against the current changes.

## Architecture Decision

ADR required: YES.

Accepted ADR-007 records Micrometer Tracing through the OpenTelemetry bridge,
W3C-only Trace Context, separate Request ID, MDC ownership, Spring Boot Logback,
and the explicit absence of exporters or observability infrastructure.

The application remains one Java/Spring Boot feature-first modular-monolith
deployable. Q-005 adds one narrowly owned platform package,
`com.brokeros.risk.observability`; it does not move existing packages or create
horizontal `interfaces/application/domain/infrastructure` layers.

## Why These Decisions Were Made

Spring MVC and Actuator already provide the HTTP server observation lifecycle.
Reusing it avoids a duplicate server span and produces a real Micrometer Trace
ID that can continue W3C `traceparent`. A small inner filter is still necessary
because Request ID is an application HTTP contract rather than a Trace ID or
W3C baggage concern.

Strict single-value, character, and length validation permits safe opaque
correlation without trusting caller metadata or logging rejected input.
Component-owned cleanup prevents the application from disrupting Micrometer's
trace scope while ensuring servlet-thread reuse cannot retain MDC state.

No exporter is needed to satisfy inbound correlation. Adding one would require
unapproved decisions about destination, authentication, encryption, sampling,
retention, sensitive attributes, access control, cost, and failure handling.

## Architecture Impact

| Area | Impact |
|---|---|
| Runtime | One managed tracing bridge inside the existing backend JAR; no new process or network destination. |
| HTTP | Adds `X-Request-ID` response/header behavior; no endpoint or response-body change. |
| Logging | Existing Logback correlation pattern now contains safe request/trace/span fields. |
| Packages | Adds only `com.brokeros.risk.observability`; no package move or feature/DDD restructure. |
| Database/Flyway | No impact; unchanged V1 remains the only migration. |
| Redis | No impact; no key, TTL, cache, or data access. |
| Kafka | No impact; no topic, event, producer, consumer, or auto-creation. |
| Kubernetes/Docker | No topology/config change; dependency packages in the existing backend image. |
| External systems | No MT4, MT5, CRM, BrokerPilot, oneZero, or other adapter behavior. |
| Risk business | No Risk Case, Rule Engine, Workflow, Account Control, Audit Module, or RBAC. |

## Development Standards Compliance

### AGENTS.md compliance

Evidence checked: root `AGENTS.md`, approved Q-005, Phase 0.5/0.6 and Q-004
architecture documents, ADR-001 through ADR-007, the development-standards
skill, and recent Q-004 lessons. The sequence was Requirement approval → Q-005
architecture/ADR → implementation → tests → skill/lesson → Review. The only
production package added is the authorized observability capability; no
prohibited business or technology scope appears in code or dependencies.

### Architecture compliance

Evidence: `backend/src/main/java/com/brokeros/risk/observability` contains one
filter inside the existing deployable. `backend/pom.xml` retains Spring Boot,
Java 21, MySQL, Redis, Kafka, Flyway, Docker, and Kubernetes direction. No
module/package relocation, repository split, service mesh, external database
access, vendor SDK, or microservice exists. Risk detection/action separation is
unaffected because neither is implemented.

### ADR compliance

Evidence: ADR-001 modular monolith/stack, ADR-002 isolation, ADR-003
API/Flyway/Logback standards, ADR-004 local/deployment layout, ADR-005 durable
standards, and ADR-006 CI gates remain unchanged. ADR-007 contains Context,
Decision, Alternatives, and Consequences and explicitly authorizes the new
bridge, W3C propagation, filter order, MDC ownership, and no-exporter boundary.
The implementation and configuration match ADR-007.

### API standard compliance

Evidence: no controller, endpoint, DTO, `ApiResponse`, `ErrorResponse`,
`ResultCode`, `BusinessException`, or `GlobalExceptionHandler` changed. The
existing health success/404 error contract tests still pass. The only external
HTTP addition is `X-Request-ID`; success and standardized error tests prove the
header without changing body formats. Actuator/OpenAPI remain framework-native.

### Database standard compliance

Evidence: `git diff --exit-code -- backend/src/main/resources/db/migration`
passed; the directory still contains only unchanged
`V1__initial_schema.sql`. Static verification found no business DDL and no
Hibernate schema generation. Q-005 introduces no entity, repository, SQL, DDL,
DML, table, column, index, constraint, lock, or data migration.

### Security standard compliance

Evidence: the filter accepts exactly one `[A-Za-z0-9._-]{1,128}` value and
replaces control-character, oversized, or multi-valued input without logging
the rejected value. Tests cover those cases. Documentation prohibits secrets,
tokens, full authentication/cookie headers, credentials, payloads, KYC, and
sensitive documents in logs. Secret/private-key/token pattern scans passed.
Dependency-tree review found no exporter, and configuration disables OTLP and
Zipkin export. Request ID is explicitly not identity or authorization.

### Auditability compliance

Evidence: Q-005 has no critical business action, decision, state transition, or
Audit module, so no audit persistence is required. Request ID and Trace ID make
engineering failures correlatable but are explicitly prohibited from becoming
audit actor/ownership. Future critical actions still require who/when/what/
target/before/after/reason/source under Phase 0.6 standards.

### Skill compliance

Evidence: `docs/skills/development-standards.md` governed preflight and review.
New `docs/skills/observability-correlation.md` captures reusable header,
filter-order, W3C, MDC ownership, logging, exporter-boundary, dependency-review,
and genuine-concurrency test patterns. `docs/skills/README.md` indexes it, and
the Q-005 lesson records actual issues without inventing incidents.

## Test and Concurrency Review

`RequestCorrelationIntegrationTests` enables real observability and uses a
test-only filter after production correlation. Its barrier makes four requests
overlap. Each request sees its own Request ID, real 32-hex Trace ID, and 16-hex
Span ID; all keys are absent on the worker after completion. A known inbound
W3C Trace ID is preserved while the server Span ID differs from the parent,
demonstrating continuation rather than superficial copying.

## Technical Debt and Recommendations

- Blocking: execute `scripts/verify-infrastructure.sh` for the current Q-005
  revision on Docker-capable CI and update the Review before PASS.
- Non-blocking: local Maven runs on Java 23 while compiling release 21; Mockito
  warns about future dynamic-agent restrictions.
- Deferred by scope: async, scheduled, outbound HTTP, Kafka propagation,
  exporter/backends, sampling policy, and telemetry retention/access control.

Do not begin a new Requirement until the runtime gate passes and the architect
reviews the completed Q-005 package.
