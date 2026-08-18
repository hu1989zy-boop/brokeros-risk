# Q-005 Review Summary

## Review Status

PARTIAL — IMPLEMENTATION COMPLETE; DOCKER RUNTIME VERIFICATION PENDING

The Q-005 code, automated tests, package build, dependency review, static
checks, and base/test/prod Kubernetes renders pass. The local host has no Docker
CLI or daemon, so Q-005's isolated Compose/MySQL/Flyway/Redis/Kafka/backend
runtime gate is `NOT EXECUTED`. This Review must not be marked PASS until the
current Q-005 changes pass `scripts/verify-infrastructure.sh` on a
Docker-capable runner.

## Current Phase / Requirement

- Architecture phase: Phase 1
- Requirement: Q-005 — Foundation Hardening & Observability Baseline
- Requirement status: Approved on 2026-08-12
- Review date: 2026-08-13
- Branch / baseline: `main` / `77229a2`

## Objective

Add the minimum inbound HTTP correlation and logging baseline without changing
the feature-first modular monolith or introducing business behavior. Request ID
and Trace ID remain distinct; W3C `traceparent` supplies real trace context; MDC
must be isolated and cleared; no exporter or observability infrastructure is
introduced.

## Completed Tasks

- Approved and recorded Q-005 with explicit scope and non-goals.
- Added Accepted ADR-007 and the Q-005 architecture analysis.
- Added Spring Boot-managed Micrometer Tracing through the OpenTelemetry bridge
  with W3C-only propagation and no exporter.
- Added `RequestCorrelationFilter` for bounded, single-valued
  `X-Request-ID` validation/generation, response propagation, and `requestId`
  MDC lifecycle.
- Added log correlation fields for application, Request ID, Trace ID, and Span
  ID while retaining Spring Boot Logback and production `INFO` logging.
- Added 7 observability integration tests for generation, preservation,
  malformed/oversized/multi-value replacement, error response headers, W3C
  parent continuation, sequential cleanup, and genuinely concurrent isolation.
- Preserved all 12 foundation tests; the complete suite now has 19 tests.
- Added reusable observability guidance and an honest Q-005 Lessons Learned
  entry.
- Updated root/backend documentation and this Review Package.

## Files Created

- `backend/src/main/java/com/brokeros/risk/observability/RequestCorrelationFilter.java`
- `backend/src/test/java/com/brokeros/risk/observability/RequestCorrelationIntegrationTests.java`
- `docs/requirements/Q-005-Requirement.md`
- `docs/architecture/q-005-foundation-hardening-observability.md`
- `docs/adr/ADR-007-micrometer-w3c-tracing.md`
- `docs/skills/observability-correlation.md`
- `docs/lessons/2026-08-13-q-005-foundation-hardening-observability.md`

## Files Modified

- `README.md`
- `backend/README.md`
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-test.yml`
- `docs/skills/README.md`
- The seven root Review Package files and `review/PhaseReviewIndex.md`

Pre-existing uncommitted Q-004 closure changes remain visible in Git status,
including the Q-004 lesson and Patch-01/review history. They were not claimed as
Q-005 implementation. The user-owned `review/review-history/*.zip` archive was
not read, modified, deleted, or staged.

## Files Deleted

None.

## Important Design Decisions

- ADR-007 selects `micrometer-tracing-bridge-otel` and W3C-only propagation.
- Spring MVC's existing server observation creates/continues the real trace;
  application code does not create a second server span or synthetic Trace ID.
- The Request ID filter runs immediately inside the observation filter at
  `Ordered.HIGHEST_PRECEDENCE + 2`.
- Micrometer owns `traceId`/`spanId`; the application owns only `requestId` and
  removes it in `finally`.
- One inbound Request ID is accepted only if it matches
  `[A-Za-z0-9._-]{1,128}`; all other input is replaced without logging the raw
  value.
- Request ID is untrusted correlation metadata, not identity, authorization,
  audit actor, idempotency, or business identity.
- OTLP and Zipkin export are disabled and no exporter dependency, collector,
  dashboard, or observability backend is present.

## Scope Preserved

No Risk Case, Rule Engine, Workflow, Account Control, Audit Module, RBAC,
business endpoint, ResultCode, business table/migration, Kafka topic/event,
Redis business key, adapter implementation, package restructure, DDD
restructure, microservice, Flink, Python, Elasticsearch, Prometheus, Grafana,
Jaeger, Zipkin, or OTLP Collector was introduced.

## Verification Summary

- Maven test: PASS — 19/19.
- Maven package: PASS — executable JAR produced, 19/19 tests.
- Dependency/exporter review: PASS.
- Static and `git diff --check`: PASS.
- Kubernetes base/test/prod rendering: PASS with checksum-verified kubectl
  v1.36.3.
- Docker/Compose/infrastructure runtime: NOT EXECUTED — Docker unavailable.

The next action is not a new Requirement. First run the current Q-005 changes
through the existing Docker-capable CI gate, update this package with that
evidence, and obtain architect review.
