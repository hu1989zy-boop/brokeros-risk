# BrokerOS Risk

BrokerOS Risk is an independent, broker-neutral trading risk management
platform for Forex/CFD brokers.

This repository currently contains the Phase 0.6 development standards. It follows
the Phase 1 architecture constraints: a Java 21/Spring Boot modular monolith,
MySQL, Redis, Kafka, Docker, and Kubernetes. Formal risk-management features
have not been implemented yet.

## Repository layout

- `backend/` — Spring Boot backend application
- `frontend/` — frontend placeholder
- `adapters/` — isolated MT4/MT5 adapter placeholders
- `deploy/` — Docker and Kubernetes deployment assets
- `docs/` — architecture, requirements, ADRs, and reusable skills
- `docs/lessons/` — honest lessons from completed phases and requirements
- `review/` — architect review package for the completed phase or requirement
- `scripts/` — project automation placeholder

## Local verification

Prerequisites:

- JDK 21 or newer
- Maven 3.9 or newer

Run the backend tests:

```bash
cd backend
mvn test
```

## Local development with Docker Compose

For the fastest edit/test cycle, start only infrastructure in Docker:

```bash
cp .env.example .env
# Set MYSQL_PASSWORD and MYSQL_ROOT_PASSWORD in .env.
docker compose up -d
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

The backend remains available through the optional `app` profile when a fully
containerized stack is needed:

```bash
docker compose --profile app up --build
```

Local endpoints and ports:

- Backend API: `http://localhost:8080/api/health`
- Actuator health: `http://localhost:8080/actuator/health`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Kafka: `localhost:29092`

Docker Compose reads local database passwords from the ignored `.env` file.
The tracked `.env.example` contains no credential values. Kubernetes database
credentials must be supplied through the deployment environment's approved
Secret-management process.

## Current scope

Phase 0.6 adds durable development, naming, API, database, auditability,
messaging, cache, security, delivery, and review standards. It adds no business
functionality, business tables, production topics, or external integrations.

## Mandatory development sequence

```text
Requirement
→ Architecture Analysis
→ Implementation
→ Test
→ Skill Update
→ Lessons Learned
→ Review Package
→ Architect Review
→ Next Requirement
```

The detailed long-term standards are in
`docs/architecture/phase-0.6-development-standards.md` and the operational
checklist is in `docs/skills/development-standards.md`.
