# Q-005 Foundation Hardening and Observability Lessons Learned

## What was implemented

Q-005 added a bounded `X-Request-ID` contract, Micrometer Tracing over the
OpenTelemetry bridge, W3C `traceparent` propagation, Request ID/Trace ID/Span ID
log correlation, MDC lifecycle cleanup, and focused sequential/concurrent
tests. It retained the existing modular monolith, API response, exception,
validation, database, messaging, cache, Docker, Kubernetes, and CI foundations.

No exporter, collector, observability backend, business module, migration,
table, topic, event, or Redis business key was added.

## Why this design

Spring MVC already instruments inbound requests with Micrometer Observation.
Placing one small Request ID filter immediately inside that observation filter
reuses the real server span while keeping application Request ID ownership
explicit. The OpenTelemetry bridge supports the approved W3C context without
requiring an exporter.

Strict single-value, character, and length validation makes caller-supplied
Request IDs safe enough for correlation fields while preserving their
untrusted status.

## Alternatives considered

- Request ID as Trace ID was rejected because it is not distributed tracing.
- A proprietary `X-Trace-ID` contract was rejected in favor of W3C
  `traceparent`.
- The Brave bridge was considered; the OpenTelemetry bridge was selected for
  the approved W3C-native baseline.
- A custom tracing wrapper and a second server span were rejected because
  Spring Boot already owns the lifecycle.
- Exporters and observability infrastructure were deferred because Q-005 does
  not define their security, retention, sampling, cost, or operations.

## Problems encountered

The first Maven dependency inspection ran inside a filesystem sandbox that
could read but not update the local Maven repository. Running the same Maven
work with the required host permission resolved and verified the approved
dependencies; no code workaround was appropriate.

The initial correlation pattern used Logback's empty default form. Runtime logs
made that visible, so the pattern was corrected to show `-` for correlation
fields outside a request. The first 19-test implementation run otherwise
passed.

## Lessons learned

- Filter order is part of the tracing contract: application correlation must
  run after Spring creates the server observation.
- Component ownership matters for cleanup. The application removes
  `requestId`; Micrometer removes its own trace/span MDC scope.
- Spring Boot tests need `@AutoConfigureObservability` to exercise real tracing.
- A known inbound Trace ID plus a different active server Span ID is strong
  evidence that `traceparent` was continued rather than copied superficially.
- Concurrent isolation tests should use a barrier; an executor alone does not
  prove that requests overlapped.
- Dependency-tree review is necessary to prove that a tracing bridge did not
  introduce an exporter.
- Runtime log output is useful verification for pattern syntax that compiles
  but can still render differently than intended.

## Reusable patterns

- Separate application Request ID from standards-based Trace ID.
- Validate untrusted correlation headers with a single-value allow-list and
  strict length bound.
- Test MDC in scope through a test-only filter instead of adding a production
  diagnostic endpoint.
- Assert MDC cleanup on the same worker thread after request completion.
- Pair dependency-tree evidence with configuration and architecture review when
  an observability backend is explicitly prohibited.

## Future risks

- Outbound HTTP, Kafka, async execution, and scheduled work need separate
  context-propagation requirements before implementation.
- An exporter requires decisions for destination, authentication, encryption,
  sampling, retention, access control, sensitive attributes, cost, and failure
  handling.
- Caller-provided Request IDs remain untrusted and must never become identity,
  authorization, audit actor, idempotency, or business keys.
- Mockito's dynamic Java-agent warning remains existing test-foundation debt as
  newer JDKs move toward disabling dynamic attachment by default.
