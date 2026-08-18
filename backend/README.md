# BrokerOS Risk Backend

Spring Boot engineering foundation for BrokerOS Risk.

## Technology

- Java 21
- Spring Boot 3.5.x
- Maven
- MySQL JDBC
- Redis
- Kafka
- Spring Boot Actuator
- Micrometer Tracing with W3C Trace Context
- Flyway
- Jakarta Bean Validation
- SpringDoc OpenAPI and Swagger UI

## API conventions

Application-owned REST endpoints return `ApiResponse<T>`. Exceptions are
translated by `GlobalExceptionHandler`, and request DTOs must use Jakarta Bean
Validation at REST boundaries.

Actuator and OpenAPI endpoints keep their framework-defined response formats.

## Request and trace correlation

Every backend HTTP response includes `X-Request-ID`. The backend preserves one
inbound value only when it matches `[A-Za-z0-9._-]{1,128}`; otherwise it returns
a generated UUID. Treat this value as untrusted correlation metadata, not as
identity, authorization, audit ownership, idempotency, or a business key.

Micrometer Tracing creates or continues W3C `traceparent` context. Request ID,
Trace ID, and Span ID are separate log fields and are cleared from MDC after
request processing. No trace exporter or observability backend is configured.

Do not log passwords, secrets, tokens, full authentication or cookie headers,
connection credentials, request/response bodies by default, KYC documents, or
sensitive personal-document data.

## Database migrations

Flyway runs automatically when the application starts with a configured MySQL
database. Add schema changes as new versioned files under
`src/main/resources/db/migration`. Never edit a migration that has already been
applied in any shared environment.

## Configuration management

Spring Boot Externalized Configuration is the only runtime configuration
mechanism. Framework-owned properties retain their native namespaces and must
not be wrapped in BrokerOS-specific classes. A future real BrokerOS-owned group
uses `brokeros.risk.<capability>`, immutable `@ConfigurationProperties`, and
startup-time Jakarta Validation.

There is currently no BrokerOS-owned configuration group, so no production
properties class exists. See `../docs/configuration/README.md` for the complete
catalog, profiles, aliases, validation, Secret, restart, and compatibility
rules.

## Run tests

```bash
mvn test
```

## Run locally

Start MySQL, Redis, and Kafka, then run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Configuration is supplied by environment variables. See `application.yml` for
the supported variables and local defaults.

Available endpoints:

- `/api/health`
- `/actuator/health`
- `/v3/api-docs`
- `/swagger-ui.html`
