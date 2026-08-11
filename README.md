# BrokerOS Risk

BrokerOS Risk is an independent, broker-neutral trading risk management
platform for Forex/CFD brokers.

This repository currently contains the Q-004 CI and integration-verification
foundation. It follows the Phase 1 architecture constraints: a Java 21/Spring
Boot modular monolith, MySQL, Redis, Kafka, Docker, and Kubernetes. Formal
risk-management features have not been implemented yet.

## Repository layout

- `backend/` — Spring Boot backend application
- `frontend/` — frontend placeholder
- `adapters/` — isolated MT4/MT5 adapter placeholders
- `deploy/` — Docker and Kubernetes deployment assets
- `docs/` — architecture, requirements, ADRs, and reusable skills
- `docs/lessons/` — honest lessons from completed phases and requirements
- `review/` — architect review package for the completed phase or requirement
- `scripts/` — repository-owned engineering verification

## Local verification

Prerequisites:

- JDK 21 or newer
- Maven 3.9 or newer

Run the backend tests:

```bash
cd backend
mvn test
mvn package
```

Run repository and deployment verification when the required tools are
available:

```bash
sh scripts/verify-static.sh
sh scripts/verify-kustomize.sh
sh scripts/verify-infrastructure.sh
```

The GitHub Actions workflow runs the same blocking checks with Java 21,
Kustomize, and an isolated Docker Compose project. It verifies infrastructure
only and performs no deployment.

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

All Compose host ports bind to loopback only.

Docker Compose reads local database passwords from the ignored `.env` file.
The tracked `.env.example` contains no credential values. Kubernetes database
credentials must be supplied through the deployment environment's approved
Secret-management process.

## Current scope

Q-004 adds CI and infrastructure verification only. It adds no business
functionality, business table, production topic, production Redis key, or
external integration.

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
