# Q-003: Phase 0.6 Development Standards

## Status

Approved

## Objective

Establish mandatory, durable development standards for BrokerOS Risk before
formal business development begins. This phase defines constraints and review
practices only; it does not implement business capabilities.

## Scope

Phase 0.6 shall standardize:

- Java package and modular-monolith boundaries;
- controller, service, DTO, entity, domain, and mapper responsibilities;
- application API responses, result codes, validation, and exception handling;
- MySQL naming, identifiers, Flyway migrations, time, money, price, volume,
  enum, and state-transition rules;
- auditability expectations;
- Kafka topic/event and Redis key/cache conventions;
- logging, security, external-adapter, timeout, retry, and idempotency rules;
- Git, Requirement, ADR, skill, Lessons Learned, test, code-review, and Review
  Package practices;
- mandatory standards-compliance checking before and after every future task.

The detailed standards are defined in
`docs/architecture/phase-0.6-development-standards.md` and summarized for task
execution in `docs/skills/development-standards.md`.

## Long-term authority

After Phase 0.6 approval, the following are mandatory constraints for every
future Q-XXX, phase, bug fix, refactor, and technical task:

- `AGENTS.md`;
- applicable architecture documents;
- all accepted ADRs;
- `docs/skills/development-standards.md`.

A conflicting request must identify the exact conflict and determine whether a
Requirement, architecture document, or ADR needs an explicit change. Existing
rules may not be bypassed silently or weakened without an approved decision.

Every Review Package `ArchitectureReview.md` must contain a substantive
`Development Standards Compliance` section covering AGENTS, architecture, ADR,
API, database, security, auditability, and skill compliance. A review containing
an unresolved standards violation cannot be marked PASS.

## Explicit exclusions

Phase 0.6 shall not:

- implement Risk Client, Risk Event, Risk Case, Rule Engine, Account Control,
  or Audit business modules;
- create business database tables or production Kafka topics;
- implement CRM, MT4/MT5 Manager SDK, BrokerPilot, or oneZero integrations;
- introduce Flink, Python, Elasticsearch, MongoDB, microservices, another
  message queue, a service mesh, a logging platform, an API gateway, or an
  authentication framework;
- invent business identifiers, calculations, state machines, or integration
  contracts before approved requirements exist.

## Acceptance criteria

1. The Phase 0.6 architecture document covers every standard in scope.
2. ADR-005 records the decision to make these standards durable and mandatory,
   including alternatives and consequences.
3. `docs/skills/development-standards.md` provides reusable preflight,
   implementation, review, and validation guidance.
4. `docs/lessons/` defines the Lessons Learned format and contains an honest
   Phase 0.6 record.
5. `AGENTS.md` automatically applies the standards and conflict process to all
   future development work.
6. Existing backend tests and packaging still pass without any business code or
   business schema change.
7. The Phase 0.5 review is preserved and a complete Phase 0.6 Review Package is
   generated.
8. Phase 0.6 `ArchitectureReview.md` provides evidence for all eight mandatory
   Development Standards Compliance checks.
