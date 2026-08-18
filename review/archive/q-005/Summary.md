# Q-005 Review Summary

## Review Status

PASS — Q-005 COMPLETE

The Q-005 code, automated tests, package build, dependency review, static
checks, base/test/prod Kubernetes renders, and Docker infrastructure gate pass.
The local host has no Docker CLI or daemon, so the approved GitHub Actions path
executed the isolated Compose/MySQL/Flyway/Redis/Kafka/backend runtime gate for
the committed Q-005 revision. CI run `32104955908` completed successfully for
commit `f693128eb381564bc8f5f1fed02f2d933e9f2822`.

## Current Phase / Requirement

- Architecture phase: Phase 1
- Requirement: Q-005 — Foundation Hardening & Observability Baseline
- Requirement status: Approved on 2026-08-12
- Review date: 2026-08-18
- Branch / reviewed commit: `main` / `f693128eb381564bc8f5f1fed02f2d933e9f2822`

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
- Passed GitHub Actions CI run `32104955908`, including static validation,
  Maven test/package, Kustomize rendering, and the complete isolated Docker
  infrastructure verification.

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

Pre-existing uncommitted Q-004 documentation remains visible in Git status and
is not claimed as Q-005 implementation. The user-owned
`review/review-history/*.zip` archive was not read, modified, deleted, staged,
or committed as part of Q-005.

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
- GitHub Actions: PASS — run `32104955908`, job `95612441180`, commit
  `f693128eb381564bc8f5f1fed02f2d933e9f2822`.
- Docker/Compose/infrastructure runtime: PASS in the approved GitHub Actions
  runner. Compose startup, MySQL/Flyway, Redis, Kafka, backend health, fatal-log
  scan, and cleanup all passed.
- Local Docker execution: NOT EXECUTED — Docker is unavailable locally; this is
  no longer a closure blocker because the current committed revision passed the
  approved CI gate.

Q-005 is complete and its Review is PASS. Any next phase or Requirement must be
formally approved before implementation.
