# ADR-001: Technology Roadmap

- Status: Accepted
- Date: 2026-08-11

## Context

BrokerOS Risk needs a small, runnable foundation that can evolve into an
independent Forex/CFD broker risk platform without premature operational or
architectural complexity. Phase 0 is project initialization performed under the
Phase 1 architecture constraints.

## Decision

Use a modular-monolith backend based on Java 21, Spring Boot 3.x, and Maven.
Use MySQL for application-owned relational data, Redis for cache and short-lived
state, and Kafka for asynchronous messaging. Package and deploy with Docker and
Kubernetes.

During Phase 1:

- keep one backend deployable unless a later accepted ADR justifies a split;
- do not introduce Flink or Python;
- do not introduce Elasticsearch, service mesh, or microservice decomposition
  as part of the foundation;
- defer formal risk business modules until their approved requirements exist.

## Consequences

- The initial system is straightforward to build, test, and operate.
- Module boundaries must be maintained inside the backend as capabilities are
  added.
- Scaling and deployment occur at the backend application level during Phase 1.
- Any future phase or technology change requires a new ADR and impact analysis.
