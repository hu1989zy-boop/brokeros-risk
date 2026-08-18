# Observability Correlation Skill

## When to use

Use this guidance when adding or reviewing HTTP request correlation, trace
propagation, MDC fields, logging patterns, asynchronous context propagation, or
a future trace exporter in BrokerOS Risk.

Read Q-005, the Q-005 architecture document, ADR-007, `AGENTS.md`, and the
development-standards skill first. A future exporter, async/Kafka propagation,
or observability backend needs its own approved Requirement and ADR evaluation.

## Correlation contracts

- Request ID and Trace ID are separate concepts.
- Request ID is carried by `X-Request-ID` and identifies one application HTTP
  request for caller/operator correlation.
- Trace ID comes only from the active Micrometer Tracing span context and is
  propagated with W3C `traceparent`.
- Never manufacture `traceId`, introduce `X-Trace-ID`, or copy Request ID into
  Trace ID.
- Neither identifier is authentication, authorization, an audit actor, an
  idempotency key, or a business identifier.

## Safe Request ID pattern

Accept an inbound Request ID only when the servlet exposes exactly one value
matching:

```text
[A-Za-z0-9._-]{1,128}
```

Generate an opaque UUID for missing, empty, malformed, oversized, or
multi-valued input. The allow-list and length bound prevent control-character
log injection and resource abuse. Set the effective response header before the
downstream filter chain so standardized error responses carry it too.

Do not log the raw rejected value.

## Servlet and MDC lifecycle

Spring's `ServerHttpObservationFilter` must establish the active trace before
application request processing. The Request ID filter therefore runs
immediately inside it at `Ordered.HIGHEST_PRECEDENCE + 2`.

The ownership rule is:

- Micrometer owns `traceId` and `spanId` MDC values and closes their scope.
- `RequestCorrelationFilter` owns only `requestId` and removes it in `finally`.
- Application code must not clear another component's MDC while its scope is
  active.
- Tests must assert that `requestId`, `traceId`, and `spanId` are all absent
  after the complete filter chain returns.

Do not use a controller interceptor when correlation must cover the whole
servlet request and exception-handler path. Do not create a second server span
when Spring MVC observation already creates one.

## W3C tracing baseline

- Use the Spring Boot-managed `micrometer-tracing-bridge-otel` dependency.
- Configure `management.tracing.propagation.type=w3c`.
- A valid inbound `traceparent` must retain its Trace ID and create a server
  child Span ID.
- A request without `traceparent` must still have a real active trace context.
- Keep baggage disabled until a concrete approved propagation need exists.
- Do not add Jaeger, Zipkin, OTLP, collector, exporter, dashboard, or vendor
  telemetry dependencies under Q-005.

## Logging rules

Keep Spring Boot Logback. Include application name, Request ID, Trace ID, and
Span ID in the correlation segment, with a visible default when no request is
active.

Never log:

- passwords, tokens, secrets, API keys, or connection credentials;
- full `Authorization`, `Cookie`, or `Set-Cookie` headers;
- request or response bodies by default;
- KYC files or sensitive personal-document data;
- rejected raw correlation header values.

Prefer method, normalized path, safe result code, duration, and bounded opaque
identifiers only when a concrete operational log is needed. Stack traces stay
server-side; client responses remain standardized and safe.

## Test pattern

Spring Boot tests disable observability by default. Use
`@AutoConfigureObservability` in the focused integration test.

A test-only filter ordered after Request correlation can capture the in-scope
MDC without adding a production endpoint. Verify:

1. absent Request ID generates a UUID;
2. one safe value is preserved;
3. control-character, oversized, and multi-valued inputs are replaced;
4. success and error responses return `X-Request-ID`;
5. a known W3C `traceparent` continues the Trace ID with a new server Span ID;
6. sequential requests leave no MDC residue;
7. concurrent requests overlap and retain distinct request/trace contexts.

Use a barrier in the concurrent test so the requests genuinely overlap. After
each request, assert all correlation MDC keys are absent on the worker thread.

## Common mistakes

- Testing only generated values and not attacker-controlled header boundaries.
- Calling a random UUID a Trace ID.
- Placing Request ID in W3C baggage merely to avoid an explicit application
  correlation filter.
- Running the Request ID filter before the observation filter and finding no
  active trace.
- Clearing `traceId` or `spanId` manually inside the application filter.
- Omitting `finally`, allowing servlet thread reuse to leak context.
- Adding an exporter transitively through a tracing starter without reviewing
  the dependency tree.
- Claiming concurrency coverage when requests never overlap.

## Validation checklist

- `mvn dependency:tree` contains one tracing bridge and no exporter.
- W3C is the only configured propagation type.
- Request/Trace IDs remain separate in headers, MDC, logs, and docs.
- Header input is single-valued, bounded, and allow-listed.
- No payload or sensitive-header logging was introduced.
- Existing API bodies and ResultCodes are unchanged.
- No database migration, Redis key, Kafka topic/event, or business module was
  added.
- Unit/integration tests cover behavior, cleanup, isolation, and concurrency.
- The Review Package records actual Maven, static, Kubernetes, and
  infrastructure verification evidence.
