# Q-001: Phase 0 Project Foundation

## Status

Approved

## Requirement

BrokerOS Risk shall provide a runnable Phase 0 repository foundation containing:

- a Java 21, Spring Boot 3.x, Maven backend;
- baseline MySQL, Redis, Kafka, and Actuator configuration;
- `/api/health` and `/actuator/health` endpoints;
- a backend Dockerfile and a local Docker Compose stack;
- Kubernetes base, test, and production configuration;
- placeholders for the frontend and isolated MT4/MT5 adapters;
- architecture, requirements, ADR, skills, deployment, and scripts directories.

Phase 0 shall not implement Risk Case, Risk Rule, or other formal risk business
capabilities. It shall not introduce Flink, Python, Elasticsearch,
microservices, service mesh, or invented MT4/MT5 Manager API interfaces.

## Acceptance criteria

1. `backend/pom.xml` targets Java 21 and Spring Boot 3.x.
2. `mvn test` passes in `backend/`.
3. Both health endpoints are covered by automated tests.
4. `docker-compose.yml` defines MySQL, Redis, Kafka, and the backend.
5. Kubernetes contains a Deployment, Service, ConfigMap, and test/prod overlays.
6. ADR-001 and ADR-002 document the Phase 0 technology and isolation decisions.
