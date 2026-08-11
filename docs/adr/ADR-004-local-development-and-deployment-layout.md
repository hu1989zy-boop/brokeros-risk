# ADR-004: Local Development and Deployment Layout

- Status: Accepted
- Date: 2026-08-11

## Context

Running the backend only as a rebuilt Compose image slows the Java edit/test
cycle, while removing it from Compose would lose a convenient full-stack and
container-image validation path. The existing Kubernetes and repository layouts
also need review before business development.

## Decision

- Keep MySQL, Redis, and Kafka as the default Docker Compose services.
- Keep the backend in Compose under the optional `app` profile.
- Run the backend with Maven on the host for the normal local development loop.
- Retain Kubernetes Kustomize `base`, `test`, and `prod` directories.
- Retain one repository and one Spring Boot deployable.
- Do not introduce Helm, extra environment layers, microservices, or repository
  splits without a demonstrated need and an accepted ADR.

## Consequences

- Developers get faster code/test feedback while preserving a containerized
  full-stack command.
- Two documented run modes must remain configuration-compatible.
- Kubernetes and repository structures remain small and can evolve
  incrementally when real requirements appear.
