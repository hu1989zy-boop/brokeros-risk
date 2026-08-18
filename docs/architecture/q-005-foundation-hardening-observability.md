# Q-005 Foundation Hardening and Observability Architecture

## Purpose

Q-005 adds inbound HTTP correlation and a logging baseline to the existing
Spring Boot modular monolith. It does not add business behavior, persistence,
messaging contracts, cache data, an external telemetry backend, or another
deployable.

## Existing foundation

The application already has Spring MVC, Actuator and Micrometer Observation,
Spring Boot Logback, unified API responses, global exception handling, Jakarta
Bean Validation, OpenAPI, MySQL/Flyway, Redis, Kafka, Docker Compose,
Kubernetes Kustomize, Maven tests, and GitHub Actions verification. Q-005 reuses
these capabilities. It does not create parallel API, exception, validation,
logging, infrastructure, or test foundations.

## Correlation model

Request ID and Trace ID are intentionally separate:

- Request ID is an application HTTP correlation value carried by
  `X-Request-ID`. It helps an operator and caller identify one request but is
  not a security identity, audit actor, idempotency key, or business identifier.
- Trace ID comes from a real Micrometer Tracing span context. It is propagated
  through the W3C `traceparent` protocol and can correlate work across future
  instrumented boundaries.

The request flow is:

```text
HTTP request
  -> Spring ServerHttpObservationFilter creates or continues W3C trace context
  -> RequestCorrelationFilter validates or generates X-Request-ID
  -> requestId + traceId + spanId are available in MDC
  -> controller / GlobalExceptionHandler
  -> X-Request-ID response header
  -> RequestCorrelationFilter removes requestId
  -> tracing scope closes and removes traceId/spanId
```

The custom filter runs immediately inside Spring's server observation filter.
This ensures a real current span exists before application request processing.
Micrometer owns `traceId` and `spanId`; application code does not manufacture or
manually clear those keys. The custom filter owns `requestId` and removes it in
a `finally` block. Tests verify that all three MDC values are absent after the
filter chain completes.

## Request ID contract and security

`X-Request-ID` is reused only when the servlet request exposes exactly one value
and it matches `[A-Za-z0-9._-]{1,128}`. Missing, empty, malformed, oversized, or
multi-valued input is replaced with a UUID. The effective value is set before
the filter chain runs so success and error responses both carry it.

The allow-list blocks whitespace and control-character log injection. No
header, query, or body is logged by the filter. Caller-supplied values remain
untrusted metadata and cannot grant permissions or establish identity.

## Tracing strategy

Use Spring Boot-managed Micrometer Tracing with the OpenTelemetry bridge and
W3C propagation only. Spring MVC's existing observation instrumentation creates
the inbound server span and continues a valid `traceparent` trace. Spring Boot
and the bridge put `traceId` and `spanId` in MDC while the span is in scope.

No Zipkin, Jaeger, OTLP, or other exporter dependency is present. No collector,
dashboard, logging backend, metrics backend, or external observability service
is configured. The default sampling probability remains externally
configurable and does not affect the availability of an active trace context.

## Logging baseline

Spring Boot Logback remains the logging implementation. The standard console
correlation segment identifies the application and includes `requestId`,
`traceId`, and `spanId` when available. Non-request logs use `-` for absent
values. Production application logging remains `INFO` by default.

Application code must not log passwords, secrets, tokens, full authentication
or cookie headers, connection credentials, request/response bodies by default,
KYC documents, or sensitive personal-document data. Q-005 adds no access-log or
payload-log filter.

## Package and API impact

The request filter belongs to `com.brokeros.risk.observability`, a narrowly
owned platform capability. No existing package moves, no horizontal top-level
package architecture is introduced, and the application remains one
feature-first modular-monolith deployable.

No endpoint, response body, `ApiResponse`, `ErrorResponse`, `ResultCode`, or
exception mapping changes. `X-Request-ID` is the only new application HTTP
response header. Actuator and OpenAPI retain their framework-native bodies.

## Data, messaging, cache, and integration impact

- Database/Flyway: no migration, DDL, DML, table, column, or V1 change.
- Kafka: no topic, event, producer, consumer, or auto-creation change.
- Redis: no key, value, TTL, cache, or source-of-truth change.
- Adapters: no MT4, MT5, CRM, or other integration change.
- Auditability: no Audit module or critical business action; Request ID is not
  audit identity.
- Operations: no deployment topology or Secret change; the dependency is
  packaged in the existing backend image.

## Verification design

Spring Boot integration tests enable observability explicitly and verify:

- Request ID generation, valid preservation, and invalid/multi-value replacement;
- response headers on success and standardized error paths;
- W3C `traceparent` continuation using the active real Trace ID;
- Request ID and trace MDC values while a request is processed;
- normal/error cleanup, sequential isolation, and concurrent isolation;
- unchanged API bodies and all existing foundation tests.

Repository static checks, Maven test/package, Kustomize rendering, and the Q-004
isolated Compose infrastructure gate remain the final operational evidence.
