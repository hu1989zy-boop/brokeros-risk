# ADR-007: Micrometer W3C Tracing and Request Correlation

- Status: Accepted
- Date: 2026-08-12
- Requirement: Q-005

## Context

The Spring Boot backend has safe API and logging foundations but no Request ID
contract or verified distributed trace context. Q-005 requires `X-Request-ID`,
W3C `traceparent` propagation, MDC correlation, and cleanup without adding an
exporter, collector, monitoring platform, business module, or architecture
split.

Request ID and Trace ID solve different problems. A caller-visible Request ID
correlates one application request. A Trace ID must come from a standards-based
tracing context and must not be replaced by a random application identifier.
The choice of tracing bridge and propagation strategy is an architecture
decision because it introduces a runtime dependency and a durable integration
contract.

## Decision

- Use Spring Boot-managed Micrometer Tracing with
  `io.micrometer:micrometer-tracing-bridge-otel`.
- Configure W3C as the only consumed and produced propagation type. A valid
  inbound `traceparent` continues its Trace ID; otherwise Spring's HTTP server
  observation creates a new trace context.
- Add no span exporter. Do not add Jaeger, Zipkin, OTLP, ELK/OpenSearch,
  Prometheus, Grafana, collector, dashboard, or vendor telemetry integration.
- Let Spring MVC's existing `ServerHttpObservationFilter` and Micrometer own the
  Trace ID/span lifecycle and their `traceId`/`spanId` MDC values.
- Add one narrow `RequestCorrelationFilter` immediately inside the observation
  filter. It validates or generates `X-Request-ID`, returns the effective value,
  places `requestId` in MDC, and removes it in `finally`.
- Keep Request ID and Trace ID as separate log fields and contracts. Never use
  Request ID as trace, authentication, authorization, audit, idempotency, or
  business identity.
- Retain Spring Boot Logback and add correlation fields through Spring Boot
  logging configuration rather than a custom appender or logging backend.

## Alternatives

### Use Request ID as Trace ID

Rejected because it would falsely claim distributed trace semantics, would not
validate or continue W3C `traceparent`, and would couple two distinct contracts.

### Use a proprietary `X-Trace-ID` filter

Rejected because W3C Trace Context is the approved interoperable protocol and
Spring MVC already provides observation instrumentation.

### Use the Brave bridge

Brave can be configured for propagation, but the OpenTelemetry bridge was
selected because W3C Trace Context is native to its context model and it keeps
the chosen baseline aligned with the approved W3C contract. Neither choice
requires an exporter, but using both would create duplicate spans and is
prohibited.

### Add an exporter and observability backend now

Rejected because Q-005 needs correlation only. Export destination, sampling,
retention, access control, sensitive-data policy, cost, and operations require a
future approved Requirement.

### Build custom tracing abstractions

Rejected because Spring Boot, Micrometer Observation, and Micrometer Tracing
already own the required lifecycle and context propagation. A wrapper would add
no current value.

## Consequences

- Requests have separate Request ID and real W3C Trace ID correlation.
- Valid inbound `traceparent` values can be continued without an external
  telemetry system.
- Logs include bounded, safe correlation fields while request processing is in
  scope; lifecycle cleanup prevents thread reuse from leaking MDC state.
- The backend gains the OpenTelemetry bridge and its managed transitive runtime
  dependencies, but no network exporter or new infrastructure component.
- Future outbound HTTP, Kafka, async, exporter, or sampling strategy changes
  require their own approved Requirement and ADR evaluation.
- Tests must explicitly enable observability because Spring Boot test contexts
  disable it by default.
