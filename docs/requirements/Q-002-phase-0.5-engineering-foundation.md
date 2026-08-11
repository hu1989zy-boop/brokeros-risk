# Q-002: Phase 0.5 Engineering Foundation

## Status

Approved

## Requirement

BrokerOS Risk shall establish engineering conventions before formal business
development begins:

- Flyway owns all application database schema migrations and runs on startup.
- The initial migration creates no business tables.
- Application REST APIs use a common `ApiResponse<T>` envelope and `ResultCode`.
- API errors use `ErrorResponse` and are translated by a global exception
  handler without exposing stack traces.
- Request DTOs support Jakarta Bean Validation.
- OpenAPI JSON and Swagger UI are available.
- Spring Boot Logback remains the logging implementation.
- Repository formatting and ignore conventions are explicit.
- Local Docker Compose prioritizes a fast development loop while retaining a
  complete containerized-stack option.
- The Kubernetes and repository structures are reviewed without introducing
  microservices or unnecessary deployment abstractions.
- A complete architect Review Package is generated at the end of the phase.

This requirement does not authorize formal risk business modules, external
system integrations, Flink, Python, Elasticsearch, additional observability
products, microservices, or a repository split.

## Acceptance criteria

1. Flyway dependencies, configuration, and `V1__initial_schema.sql` exist.
2. The initial migration contains no business tables.
3. `/api/health` uses `ApiResponse`.
4. Validation, business exceptions, unexpected exceptions, OpenAPI, and Swagger
   UI have automated coverage.
5. `mvn test` and `mvn package` succeed.
6. Docker and Kubernetes review outcomes are documented.
7. ADR-003 and ADR-004 record the engineering and local-development decisions.
8. `docs/skills` records reusable engineering knowledge.
9. `review/` contains the mandatory final review artifacts.
