# ADR-003: Engineering Foundation Standards

- Status: Accepted
- Date: 2026-08-11

## Context

Business development requires stable database, HTTP, validation, error-handling,
and API-documentation conventions. Deferring these conventions would create
inconsistent migrations and contracts across future modules.

## Decision

- Use Flyway for every change to the application-owned MySQL schema.
- Keep migrations immutable after they are applied to a shared environment.
- Use `ApiResponse<T>`, `ErrorResponse`, and `ResultCode` for application-owned
  REST responses.
- Translate application exceptions through `GlobalExceptionHandler`.
- Use `BusinessException` only for expected application failures represented by
  an explicit result code.
- Use Jakarta Bean Validation on request DTOs at REST boundaries.
- Generate OpenAPI and Swagger UI with SpringDoc 2.x for Spring Boot 3.x.
- Retain Spring Boot's Logback implementation.

Framework-managed operational protocols are not application REST contracts.
Actuator and OpenAPI therefore retain their required native response formats.

## Consequences

- Database history and API contracts become predictable and reviewable.
- Unexpected implementation details are logged but not returned to clients.
- New result codes and migrations require deliberate, backward-compatible
  changes.
- Unit tests disable Flyway when no database is present; a MySQL-backed migration
  integration test remains future engineering work.
