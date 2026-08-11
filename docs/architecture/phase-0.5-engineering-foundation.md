# Phase 0.5 Engineering Foundation Architecture

## Objective

Phase 0.5 improves development safety and consistency before business modules
are introduced. It does not change the Phase 1 modular-monolith direction or
add business behavior.

## Database lifecycle

Flyway is the only supported schema-change mechanism. It is enabled by default,
validates migrations on startup, prohibits `clean`, and applies versioned SQL
from `classpath:db/migration` against the application-owned MySQL database.

`V1__initial_schema.sql` intentionally contains no business tables. Flyway's
schema history is the only metadata introduced by the baseline.

## HTTP API foundation

Application-owned REST endpoints return `ApiResponse<T>` with a stable result
code, message, payload, and timestamp. Errors place safe details in
`ErrorResponse`; `GlobalExceptionHandler` maps validation, malformed requests,
business exceptions, common HTTP failures, and unexpected exceptions.

Unexpected exceptions are logged server-side with their stack trace but return
only a stable internal-error response. Actuator and OpenAPI are operational
protocol endpoints rather than application APIs, so their response formats are
not wrapped.

Jakarta Bean Validation is the request-boundary validation mechanism for future
DTOs. SpringDoc exposes `/v3/api-docs` and `/swagger-ui.html`.

## Logging

Spring Boot's default Logback integration remains sufficient. No custom logging
backend or aggregation stack is introduced. Profile-specific log levels remain
in Spring configuration, and the exception handler records unexpected failures.

## Local development

Docker Compose starts infrastructure by default. The backend remains in the
optional `app` profile for image and full-stack verification. This avoids image
rebuilds during normal Java development without losing deployment parity.

## Kubernetes and repository review

Kustomize base plus test/prod overlays remains appropriate. The repository
continues as one repository with one backend deployable, external-adapter
placeholders, deployment assets, documentation, and a frontend placeholder.
No structural split is justified before business requirements define real
module boundaries.
