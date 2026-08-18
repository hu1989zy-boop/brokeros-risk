# Q-005 Outstanding Items

## Remaining Work Before PASS

None. GitHub Actions run `32104955908` passed every required Q-005 verification
stage for commit `f693128eb381564bc8f5f1fed02f2d933e9f2822`, including the
Docker-capable infrastructure gate and isolated cleanup. Q-005 Review status is
PASS.

## Known Non-Blocking Issues

- Local Maven runs on Java 23 while compiling release 21. The project and CI
  target remain Java 21.
- Mockito/Byte Buddy warns that dynamic agent attachment will be disabled by a
  future JDK. It did not fail any of the 19 tests.
- The user-owned untracked `review/review-history/*.zip` remains outside Q-005.
  It was not read, modified, deleted, staged, or included in verification.
- Pre-existing uncommitted Q-004 closure documentation remains visible in the
  working tree and must be handled separately from this Q-005 review closure.

## Deferred Work

- Trace export, Jaeger, Zipkin, OTLP Collector, telemetry SaaS, dashboards,
  metrics backends, sampling/retention/access-control strategy.
- Outbound HTTP, async executor, scheduler, Kafka, or adapter trace propagation.
- Authentication, authorization/RBAC, Audit Module, Workflow, Risk Case, Rule
  Engine, Account Control, and all business modules.
- Business tables/migrations, Kafka topics/events, Redis business keys/caches,
  and real MT4/MT5/CRM integrations.
- Package/DDD restructuring, microservices, service mesh, Flink, Python,
  Elasticsearch, Prometheus, and Grafana.

Deferred items are outside Q-005 and are not implementation defects.

## Residual Risks

- Filter-order changes in a future Spring Boot upgrade could affect when trace
  MDC is available; retain the W3C and in-scope MDC integration tests.
- Caller-provided Request IDs remain untrusted. Future code must not promote
  them to identity, permissions, audit ownership, idempotency, or business keys.
- Future async or messaging code will not inherit servlet MDC automatically
  unless an approved context-propagation design is added.
- A future exporter can leak sensitive span attributes or create network/cost
  failure modes unless governed by a new Requirement and ADR.

## Recommendation

Q-005 is closed. Do not implement any deferred or business capability from this
document. Start the next task only from an approved Requirement after its
architecture and standards preflight.
