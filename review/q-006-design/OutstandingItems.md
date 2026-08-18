# Q-006 Design Outstanding Items

## Blocking Before Implementation — Resolved

1. Architect approval of `docs/requirements/Q-006-Requirement.md`.
2. Architect approval of the Q-006 architecture design and scope exclusions.
3. Architect approval of the ADR-008 determination.
4. Draft and explicit acceptance of ADR-008.
5. Confirmation that no production `@ConfigurationProperties` type should be
   invented while no concrete application-owned setting exists.
6. Confirmation that configuration is startup-bound and dynamic refresh remains
   outside Q-006.

The architect approved all applicable items on 2026-08-18, and ADR-008 was
accepted before Phase 2 implementation. This design-stage blocker is closed.

## Design-Time Gaps Addressed in Phase 2

- The central catalog now consolidates configuration ownership, keys, aliases,
  types, defaults, profiles, requiredness, sensitivity, validation, sources,
  restart behavior, and compatibility.
- Focused tests now cover profile loading, missing required configuration,
  invalid typed values, override priority, safe diagnostics, Actuator exposure,
  and catalog coverage of deployment aliases.
- The configuration guide now unifies local/CI/Kubernetes Secret conventions
  and environment-alias compatibility rules.
- Production profile activation remains an explicit deployment responsibility;
  the supported base/test/prod contract is documented and tested.

## Deferred Work

- Broker-specific policies, thresholds, feature flags, and business rules.
- Runtime configuration API/UI, persistence, approval workflow, audit trail,
  dynamic refresh, rollback, and multi-instance consistency.
- Config Server, Vault, Consul, Kubernetes operators, or another secret/config
  service.
- Authentication, RBAC, Audit Module, Risk Case, Rule Engine, Workflow, Account
  Control, business schema, topics/events, Redis business state, and adapters.
- Package restructuring, microservices, another repository/deployable, Flink,
  Python, Elasticsearch, Prometheus, Grafana, Jaeger, Zipkin, and OTLP.

Deferred items are not defects in the Design Only package.

## Working Tree Boundary

Pre-existing Q-004/Q-005 Review closure changes and the protected
`review/review-history/` archive remain outside Q-006 ownership. They must not be
mixed into a future Q-006 commit without separate review.

## Recommendation

The design approval gate is complete. Consult the current root Q-006 Review for
Phase 2 verification and outstanding runtime-gate status. Add production typed
configuration only for a concrete approved BrokerOS-owned setting.
