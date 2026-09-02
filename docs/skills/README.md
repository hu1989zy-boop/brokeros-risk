# Repository Skills

This directory is reserved for reusable BrokerOS Risk engineering lessons and
task-specific skills extracted from completed work.

Available engineering knowledge:

- `brokeros-risk-core-domain.md` — the Evidence → Decision → Action → Risk Case
  baseline, Decision Core Domain, Action/Execution separation, downstream Risk
  Case, adapter isolation, and future Rule Engine/AI boundaries.
- `configuration-management.md` — configuration ownership, native versus
  application binding, catalog/alias compatibility, startup validation,
  secret/profile rules, deterministic contract tests, and YAGNI boundaries.
- `ci-integration-verification.md` — evidence matrices, blocking CI checks,
  isolated Compose verification, real MySQL/Flyway assertions, Redis/Kafka
  connectivity boundaries, Kustomize rendering, and reuse guidance.
- `development-standards.md` — mandatory preflight, architecture, API, database,
  security, auditability, review, and completion rules for every future task.
- `flutter-risk-console-development.md` — thin Flutter client boundaries,
  browser Authorization Code + PKCE, memory/secure token handling, bounded
  query projections, Riverpod state, version conflicts, and honest frontend
  verification.
- `observability-correlation.md` — separate Request ID and W3C Trace ID
  contracts, safe header validation, Micrometer/MDC lifecycle, logging rules,
  concurrency tests, and exporter boundaries.
- `phase-0.5-engineering-foundation.md` — Flyway, API envelopes, validation,
  exception handling, API documentation, and local development checks.
- `trusted-actor-authorization.md` — signed human JWT trust, authoritative
  ActorRef mapping, purpose-specific service identity, immutable ActorContext,
  explicit capabilities, fail-closed provisioning/persistence, safe errors,
  and signed-JWT plus real-MySQL verification.

Future skills must remain consistent with `AGENTS.md`, approved requirements,
and accepted ADRs.
