# Phase 0.5 Engineering Foundation Knowledge

## Adding a database change

1. Add a new `V{next}__meaningful_description.sql` file under
   `backend/src/main/resources/db/migration`.
2. Never edit or renumber a migration that has reached a shared environment.
3. Keep the migration owned by BrokerOS Risk; never target an external-system
   database.
4. Verify against MySQL before release and run the full Maven test suite.

## Adding a REST endpoint

1. Return `ApiResponse<T>` for application-owned REST operations.
2. Add or reuse a stable `ResultCode`; do not make client behavior depend on
   exception text.
3. Put safe structured error details in `ErrorResponse`.
4. Annotate request DTO fields with Jakarta Bean Validation and apply `@Valid`
   at the controller boundary.
5. Let `GlobalExceptionHandler` translate failures; do not catch exceptions in
   controllers solely to construct HTTP responses.
6. Verify the generated contract through `/v3/api-docs`.

Actuator and OpenAPI are framework-managed operational protocols and keep their
native formats.

## Local development loop

```bash
docker compose up -d
cd backend
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

Use `docker compose --profile app up --build` when validating the backend image
and complete containerized stack.

## Completion checks

- Run `mvn test` and `mvn package` from `backend/`.
- Run `docker compose config` when Docker is available.
- Render both Kustomize overlays when `kubectl` is available.
- Update the phase Review Package only after all files and verification results
  are final.
