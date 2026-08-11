# BrokerOS Risk Backend

Phase 0.5 Spring Boot engineering foundation for BrokerOS Risk.

## Technology

- Java 21
- Spring Boot 3.5.x
- Maven
- MySQL JDBC
- Redis
- Kafka
- Spring Boot Actuator
- Flyway
- Jakarta Bean Validation
- SpringDoc OpenAPI and Swagger UI

## API conventions

Application-owned REST endpoints return `ApiResponse<T>`. Exceptions are
translated by `GlobalExceptionHandler`, and request DTOs must use Jakarta Bean
Validation at REST boundaries.

Actuator and OpenAPI endpoints keep their framework-defined response formats.

## Database migrations

Flyway runs automatically when the application starts with a configured MySQL
database. Add schema changes as new versioned files under
`src/main/resources/db/migration`. Never edit a migration that has already been
applied in any shared environment.

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
