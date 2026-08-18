# Q-005 Outstanding Items

## Remaining Work Before PASS

1. Run the current Q-005 revision through
   `sh scripts/verify-infrastructure.sh` on a Docker-capable approved host or the
   existing GitHub Actions workflow.
2. Confirm Compose config/startup, backend image/health, MySQL/Flyway, Redis,
   Kafka, fatal-log scan, and isolated cleanup all PASS without creating a
   business table, Redis business key, or Kafka business topic.
3. Update `review/Verification.md`, `review/Summary.md`,
   `review/ArchitectureReview.md`, Git status/diff statistics, and this file
   with that evidence.
4. Obtain architect review before starting another Requirement.

These are closure gates, not optional enhancements. Until they pass, Q-005
Review status remains PARTIAL.

## Known Non-Blocking Issues

- Local Maven runs on Java 23 while compiling release 21. The project and CI
  target remain Java 21.
- Mockito/Byte Buddy warns that dynamic agent attachment will be disabled by a
  future JDK. It did not fail any of the 19 tests.
- The user-owned untracked `review/review-history/*.zip` remains outside Q-005.
  It was not read, modified, deleted, staged, or included in verification.
- Pre-existing uncommitted Q-004 closure documentation remains visible in the
  working tree and should be reviewed separately when preparing a commit.

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

Do not start Q-006 or a business Requirement yet. First close the Docker-capable
Q-005 runtime gate, refresh the Review Package, and obtain architect approval.
