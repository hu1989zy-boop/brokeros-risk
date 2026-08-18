# Q-005: Foundation Hardening & Observability Baseline

## Status

Approved

- Requirement ID: `Q-005`
- Created: 2026-08-12
- Approved: 2026-08-12
- Architecture phase: Phase 1
- Change type: engineering foundation only

Architect approval selected Micrometer Tracing with W3C Trace Context. Request
ID and Trace ID remain separate, `X-Request-ID` and `traceparent` propagation
are required, and no trace exporter or observability infrastructure is
authorized.

## 1. Background

Q-004 established and verified the current engineering foundation: Java 21,
Spring Boot, Maven, MySQL, Flyway, Redis, Kafka, Docker Compose, Kubernetes
Kustomize, unified application API responses, global exception handling,
Jakarta Bean Validation, OpenAPI, Actuator, tests, and GitHub Actions.

The backend remains one feature-first modular-monolith deployable. Its existing
foundation is intentionally small and has no business module, business table,
Kafka business topic or event, or Redis business key.

Q-005 hardens this foundation without changing those boundaries. It adds an
HTTP correlation and logging baseline, makes sensitive-log rules explicit, and
re-verifies existing capabilities instead of reimplementing them.

## 2. Problem Statement

The backend currently has no application-owned Request ID contract, no verified
trace context, and no MDC-based correlation between a request and the logs
emitted while processing it. Spring Boot Logback and safe exception responses
already exist, but the runtime logging configuration does not expose request or
trace correlation fields and there are no tests for correlation creation,
propagation, validation, or cleanup.

Without a defined baseline, future modules could invent incompatible headers,
use untrusted correlation values, leak MDC data between requests, mislabel a
random identifier as a distributed Trace ID, or add overlapping observability
frameworks. Q-005 must close that gap before business development while
preserving the approved architecture.

## 3. Scope

### 3.1 Architecture boundary confirmation

- Retain one repository, one Spring Boot deployable, and the current
  feature-first modular-monolith direction.
- Retain the existing packages and avoid package relocation or horizontal
  `interfaces/application/domain/infrastructure` trees.
- If a new package is needed, use the smallest capability-owned platform
  package, such as `com.brokeros.risk.observability`; do not create a dumping
  ground or a business module.
- Reuse the existing API, exception, validation, OpenAPI, database, cache,
  messaging, container, Kubernetes, and test foundations.

### 3.2 Request ID baseline

- Use `X-Request-ID` as the HTTP request-correlation header.
- Give every backend HTTP request a non-empty Request ID.
- Reuse an inbound `X-Request-ID` only when it is a single value of 1–128 ASCII
  characters matching `[A-Za-z0-9._-]+`; otherwise generate a new opaque UUID.
- Return the effective Request ID in the `X-Request-ID` response header for
  successful and failed requests without changing an API response body.
- Make the Request ID available to request-scope logs through MDC and always
  remove it when request processing completes.
- Treat an inbound Request ID as untrusted correlation metadata. It must never
  establish identity, authorization, audit ownership, idempotency, or business
  uniqueness.

### 3.3 Trace ID baseline

- Continue a valid W3C Trace Context when supplied and create a trace context
  when one is absent.
- Make the active Trace ID available to request-scope logs. Request ID and Trace
  ID remain distinct concepts and fields.
- Do not invent an `X-Trace-ID` protocol or label an unrelated random value as a
  distributed Trace ID.
- Add no trace exporter, collector, backend, dashboard, or external telemetry
  service in Q-005.
- Before implementation, evaluate the tracing bridge/dependency choice against
  the ADR threshold. A new tracing dependency or propagation strategy requires
  an approved ADR before code is added.

### 3.4 Logging foundation

- Retain Spring Boot's default Logback implementation.
- Include Request ID and the active Trace ID in application log correlation
  output where request context exists; keep non-request logs readable.
- Keep environment-appropriate log levels and avoid enabling production debug
  logging by default.
- Document and apply rules that prohibit logging passwords, secrets, tokens,
  full authentication or cookie headers, connection credentials, request or
  response bodies by default, KYC documents, and sensitive personal-document
  data.
- Do not introduce blanket HTTP payload logging. Log only the minimum safe
  operational context needed to diagnose a failure.
- Preserve server-side unexpected-exception logging and the existing rule that
  stack traces or implementation details are never returned to API clients.

### 3.5 Existing foundation verification

Re-run and record verification of the existing, unchanged foundation:

- `ApiResponse<T>`, `ErrorResponse`, and `ResultCode`;
- `GlobalExceptionHandler` and `BusinessException`;
- Jakarta Bean Validation support;
- SpringDoc OpenAPI and Swagger UI;
- Actuator health;
- MySQL configuration and Flyway V1 behavior;
- Redis and Kafka infrastructure connectivity without business data or topics;
- Docker Compose and the optional backend `app` profile;
- Kubernetes base/test/prod Kustomize rendering;
- the existing Maven and GitHub Actions test foundation.

### 3.6 Documentation and review

- Record the approved Q-005 architecture analysis before implementation.
- Create or update an ADR only when the approved tracing or logging decision
  meets the repository ADR threshold.
- Update reusable engineering guidance and add an honest Q-005 Lessons Learned
  entry after implementation.
- Generate the mandatory seven-file Review Package only after implementation
  and final verification are complete.

## 4. Non Goals

Q-005 does not authorize:

- Risk Case, Rule Engine, Workflow, Account Control, Audit Module, RBAC,
  authentication, authorization, or any other business capability;
- a business controller, service, repository, entity, domain model, DTO, mapper,
  state machine, result code, endpoint, or API version;
- a new business database table, a new Flyway business migration, DDL, DML, or
  modification of `V1__initial_schema.sql`;
- a Kafka topic, Kafka business event, producer, consumer, listener, schema, or
  topic auto-creation;
- a Redis business key, cache policy, or durable business state;
- an MT4/MT5 Manager SDK implementation or another real external integration;
- package restructuring, feature-first restructuring, DDD restructuring,
  repository splitting, microservices, or another deployable;
- Flink, Python, Elasticsearch, OpenSearch, ELK, Prometheus, Grafana, a service
  mesh, a telemetry SaaS integration, a trace collector, or a trace exporter;
- production deployment, CD, or production secrets;
- outbound trace propagation abstractions when no approved outbound integration
  exists.

## 5. Acceptance Criteria

1. `Q-005` is approved before any implementation begins, and the final
   implementation stays within this Requirement.
2. The repository remains a feature-first modular monolith with one backend
   deployable; no existing package is moved or restructured.
3. Every tested backend HTTP request receives an effective `X-Request-ID`.
   Absent, malformed, oversized, or multi-valued input is replaced with a new
   opaque UUID; a valid single input value is preserved.
4. The effective Request ID is returned in the response header for both success
   and standardized application-error responses without changing
   `ApiResponse`, `ErrorResponse`, or existing `ResultCode` values.
5. A standards-compliant trace context is created or continued for HTTP request
   processing, and the real active Trace ID—not a synthetic substitute—is
   available to request-scope logging.
6. Request ID and Trace ID are present in MDC while applicable request logs are
   emitted and are removed reliably after completion, including exceptional
   completion; automated tests demonstrate no cross-request leakage.
7. The default application log output exposes available correlation fields,
   keeps Spring Boot Logback, and leaves production application logging at
   `INFO` unless an explicit environment override is supplied.
8. Sensitive logging guidance is explicit and the implementation adds no
   request/response body logging, credential logging, full authorization/cookie
   header logging, or unsafe client error detail.
9. Existing application endpoints continue to use `ApiResponse`; Actuator and
   OpenAPI retain their framework-native formats. Q-005 adds no public endpoint
   or result code.
10. Existing exception handling, validation, OpenAPI, MySQL, Flyway, Redis,
    Kafka, Docker, Kubernetes, and test foundations are reused rather than
    duplicated.
11. Flyway still has only `V1__initial_schema.sql`, the application schema still
    has no business table, Redis has no business key, and infrastructure
    verification creates no Kafka business topic.
12. No prohibited business module, external adapter implementation, technology,
    package restructuring, or deployment split is introduced.
13. Automated tests cover Request ID generation, valid input preservation,
    invalid input replacement, response headers on success and error, real
    Trace ID availability, MDC availability, and MDC cleanup while preserving
    all existing tests.
14. `mvn test`, `mvn package`, `git diff --check`, repository static checks,
    Kustomize rendering, and isolated Docker/infrastructure verification pass,
    or an unavailable local tool is recorded as `NOT EXECUTED` and verified by
    the approved CI path before Q-005 can be marked PASS.
15. Architecture documentation, any required accepted ADR, reusable skill,
    honest Lessons Learned, and the final Review Package are complete and match
    the implemented state.
16. The Review Package contains evidence-based Development Standards Compliance
    for all eight mandatory areas and has no unresolved violation.

## 6. Technical Constraints

- Use Java 21, Spring Boot 3.x, Maven, MySQL, Redis, Kafka, Docker, Kubernetes,
  Flyway, SpringDoc, Actuator, and Spring Boot Logback as already approved.
- Use the existing `com.brokeros.risk` root package and preserve current package
  ownership.
- Keep the implementation servlet-safe and compatible with the existing Spring
  MVC application. Correlation cleanup must use a `finally`-equivalent lifecycle
  guarantee.
- Use established W3C Trace Context semantics for Trace ID. Do not define a
  proprietary trace protocol.
- Keep correlation identifiers opaque and bounded. Do not place credentials,
  personal data, account details, or business meaning inside them.
- Do not change application response-body contracts, API paths, result-code
  representations, database schema, Kafka configuration to create topics, Redis
  data contracts, or Kubernetes topology.
- Keep observability local to application logging in this Requirement. No
  exporter, collector, metrics backend, dashboard, or vendor SDK is permitted.
- A tracing library/bridge, propagation strategy, change to a major dependency,
  or logging-strategy change must pass ADR evaluation and receive explicit
  architecture approval before implementation.
- Avoid generic observability frameworks, custom starter projects, shared
  libraries, and speculative outbound propagation abstractions.

## 7. Deliverables

- This approved `docs/requirements/Q-005-Requirement.md`.
- Q-005 architecture analysis documenting correlation semantics, package
  impact, API compatibility, security, operations, and the ADR determination.
- An accepted ADR if the approved tracing/logging implementation meets the ADR
  threshold; otherwise a documented `ADR not required` rationale.
- Minimal Request ID, trace-context, MDC, and logging configuration changes.
- Focused automated correlation/logging tests plus unchanged-foundation
  regression verification.
- Explicit sensitive-log engineering guidance in `docs/skills` or the approved
  development-standards documentation.
- An honest Q-005 Lessons Learned entry.
- A complete, current seven-file Review Package with Development Standards
  Compliance evidence.

No database migration, business artifact, Kafka topic/event, Redis business
artifact, production Secret, commit, or push is a Q-005 deliverable.

## 8. Verification Plan

### Automated behavior checks

- Verify generated, preserved, rejected, and returned Request IDs.
- Verify success and error responses retain their existing bodies and include
  the effective response header.
- Verify a real active Trace ID and Request ID are available during request log
  processing.
- Verify MDC cleanup after normal and exceptional completion and between
  sequential requests on a reused test thread.
- Verify invalid correlation input cannot inject control characters into logs.
- Preserve the existing unified API, exception, validation, Actuator, OpenAPI,
  and Flyway tests.

### Required commands

```bash
cd backend && mvn test
cd backend && mvn package
git diff --check
sh scripts/verify-static.sh
sh scripts/verify-kustomize.sh
sh scripts/verify-infrastructure.sh
```

`scripts/verify-infrastructure.sh` includes Compose configuration/startup,
MySQL/Flyway, Redis, Kafka, backend health, log scan, and isolated cleanup. It
must not create a business table, Redis business key, or Kafka business topic.
Missing local Docker or Kubernetes tooling is not a PASS; final CI evidence is
required before Q-005 closure.

### Repository-boundary checks

- Confirm no migration beyond V1 and no change to V1.
- Confirm no business package, table, topic/event, Redis key, public endpoint,
  ResultCode, deployable, or package relocation was added.
- Confirm no secret, local environment file, build artifact, or IDE file is
  tracked.
- Confirm only approved dependencies and documentation were introduced.

## 9. Review Checklist

- [ ] The approved Requirement and architecture analysis match the final
      implementation.
- [ ] AGENTS.md was checked and the change remains broker-neutral and within
      the Phase 1 modular monolith.
- [ ] Applicable architecture documents and ADR-001 through the latest accepted
      ADR were checked.
- [ ] The tracing/logging dependency and strategy received an explicit ADR
      determination; any required ADR was accepted before implementation.
- [ ] No API body, endpoint, ResultCode, validation, or exception contract was
      duplicated or silently changed.
- [ ] `db/migration` still contains only unchanged V1 and no business schema or
      unrecorded DDL exists.
- [ ] No Kafka topic/event or Redis business key/cache contract was created.
- [ ] Header validation, log-injection resistance, secret/sensitive-data rules,
      safe error responses, and configuration exposure were reviewed.
- [ ] Request ID and Trace ID are correlation metadata only and are not treated
      as authentication, authorization, audit identity, or business identity.
- [ ] MDC cleanup and cross-request isolation are verified.
- [ ] Risk Case, Rule Engine, Workflow, Account Control, Audit, RBAC, MT4, MT5,
      CRM, and all prohibited technology impacts are explicitly `No impact`.
- [ ] Reusable skill guidance and honest Lessons Learned are current.
- [ ] All verification evidence is classified honestly; unavailable checks are
      not marked PASS.
- [ ] The seven-file Review Package includes substantive AGENTS, architecture,
      ADR, API, database, security, auditability, and skill compliance evidence.
- [ ] No unresolved standard violation remains before the review is marked
      PASS.

## 10. Risks

### Confusing Request ID with Trace ID

A generated Request ID alone is not distributed tracing. Calling it a Trace ID
would create a misleading contract. The implementation must use real trace
context semantics and retain separate identifiers.

### New dependency or propagation decision without approval

True Trace ID support may require a tracing bridge. Adding one silently would
conflict with the ADR review rule for major dependencies and observability
strategy. The implementation must stop after architecture analysis if that
decision has not been approved.

### Header spoofing and log injection

Inbound Request IDs are attacker-controlled. Strict length/character validation
and untrusted-metadata treatment reduce log forging, oversized values, and false
identity assumptions.

### MDC leakage

Servlet threads are reused. Failure to clear MDC in all completion paths can
attribute one request's logs to another. Tests must cover normal and exceptional
cleanup.

### Sensitive data exposure

Adding broad access or payload logging could expose credentials, personal data,
or future trading data. Q-005 explicitly excludes body logging and requires
minimum safe operational context.

### Duplicate foundation and scope expansion

Rewriting API, exception, validation, infrastructure, or test foundations would
increase compatibility risk without solving the correlation gap. Existing
capabilities must be re-verified and documented, not recreated.

### Operational overhead and incomplete trace semantics

Sampling, span creation, asynchronous propagation, and exporters can affect
cost and semantics. Q-005 is limited to inbound HTTP trace correlation without
an exporter. Future async, Kafka, or adapter propagation requires its own
approved Requirement.

## Gap Analysis

### Already Exists

| Capability | Current evidence | Q-005 treatment |
| --- | --- | --- |
| Feature-first modular monolith | One backend deployable under `com.brokeros.risk`; ADR-001 and ADR-004 | Preserve and document; no restructure |
| Unified API contract | `ApiResponse`, `ErrorResponse`, `ResultCode`, and health API tests | Reuse and regression-test only |
| Exception handling | `GlobalExceptionHandler`, `BusinessException`, safe-error tests | Reuse; correlation must be visible in server logs without changing client bodies |
| Jakarta Bean Validation | Validation starter plus boundary behavior in `GlobalExceptionHandlerTests` | Document readiness; do not add a dummy production DTO or API |
| OpenAPI and Swagger UI | SpringDoc configuration and endpoint tests | Re-verify only |
| Actuator health | Health/info exposure with details disabled and endpoint test | Re-verify only; do not wrap its native format |
| Logging runtime | Spring Boot Logback, profile-specific levels, unexpected-exception logging | Retain; harden correlation and guidance only |
| MySQL and Flyway | Datasource/Flyway configuration, V1 foundation migration, unit and Q-004 integration evidence | Re-verify only; no migration or table |
| Redis and Kafka | Spring configuration, Compose services, Q-004 connectivity gates | Re-verify only; no business key, topic, event, producer, or consumer |
| Docker and Kubernetes | Optional Compose backend profile and Kustomize base/test/prod | Re-verify only; no topology change |
| Test and CI foundation | Maven tests, repository verification scripts, blocking GitHub Actions | Extend only with Q-005-focused tests and preserve existing gates |
| Baseline sensitive-log rules | `AGENTS.md`, Phase 0.6 architecture, and the development-standards skill | Consolidate Q-005-specific operational examples; no logging backend change |

The API, exception, validation, OpenAPI, Actuator, MySQL, Flyway, Redis, Kafka,
Docker, Kubernetes, and CI items above need documentation and re-verification,
not new production implementations.

### Need Improvement

| Gap | Required improvement |
| --- | --- |
| Request ID contract | Add bounded `X-Request-ID` validation, generation, response propagation, MDC availability, and cleanup |
| Real Trace ID | Select an approved standards-compliant tracing bridge/strategy, then create or continue real trace context without an exporter |
| Log correlation output | Add Request ID and active Trace ID fields while keeping default Logback and readable non-request logs |
| Correlation security | Treat inbound IDs as untrusted, prevent control-character/log injection, and forbid identity/audit use |
| Automated tests | Cover Request ID behavior, real Trace ID availability, success/error headers, and MDC cleanup/isolation |
| Focused documentation | Record correlation semantics, sensitive-log examples, package/API/security/operations impact, and ADR determination |

### Not In Scope

- Every business capability or business persistence model, including Risk Case,
  Rule Engine, Workflow, Account Control, Audit Module, and RBAC.
- Kafka topics/events and Redis business data or cache behavior.
- Any database or Flyway change.
- Package, feature-first, DDD, repository, deployable, or microservice
  restructuring.
- Metrics/tracing backends, exporters, collectors, dashboards, ELK/OpenSearch,
  Prometheus, Grafana, service mesh, Flink, Python, or Elasticsearch.
- Outbound adapter or asynchronous trace propagation before an approved
  integration or messaging Requirement exists.
- Authentication, authorization, production deployment, and production
  credentials.

## Implementation Plan

### Task 1 — Approve the contract and architecture decision

- Obtain architect approval for this Requirement draft and its exact boundary.
- Write the Q-005 architecture analysis covering Request ID semantics, W3C
  trace context, package ownership, API compatibility, security, and operations.
- Evaluate the tracing/logging dependency and strategy against the ADR threshold.
- If an ADR is required, draft it with alternatives and wait for explicit
  acceptance before changing production code.

### Task 2 — Implement the minimal correlation and logging baseline

- Add only the approved Request ID validation/generation/response behavior.
- Add only the approved real trace-context bridge and MDC integration, with no
  exporter or external backend.
- Configure readable correlation fields while retaining Spring Boot Logback and
  existing production log levels.
- Apply explicit sensitive-log rules without request/response payload logging or
  changes to API response bodies.

### Task 3 — Add focused tests and re-verify the foundation

- Add automated tests for Request ID, Trace ID, error-path correlation, input
  hardening, and MDC cleanup/isolation.
- Run Maven test/package and static checks.
- Re-run Kustomize and isolated Compose/MySQL/Flyway/Redis/Kafka/backend gates,
  reusing Q-004 verification rather than creating parallel infrastructure.
- Confirm no business code, migration/table, Redis business key, Kafka topic or
  event, public API change, or package relocation exists.

### Task 4 — Complete documentation and the Review Package

- Finalize the architecture record and ADR determination.
- Update reusable skills and add an honest Q-005 Lessons Learned entry.
- Regenerate all seven Review Package files after the implementation and
  verification state is final.
- Perform evidence-based Development Standards Compliance review and stop for
  architect approval before beginning another Requirement.

## Approval Gate

The Requirement and tracing direction were approved on 2026-08-12. ADR-007
records the selected implementation strategy. Completion still requires final
verification, the mandatory Review Package, and architect review before the
next Requirement begins.
