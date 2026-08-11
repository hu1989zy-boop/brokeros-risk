# Phase 0 Foundation Architecture

## Purpose

Phase 0 establishes a runnable project foundation without implementing formal
risk-management business capabilities. It operates within the Phase 1
architecture constraints defined in `AGENTS.md`.

## Structure

The backend is a single Spring Boot deployable and is the starting point for a
modular monolith. Future business modules must remain internally separated but
must not be split into independently deployed services without an approved ADR.

External trading-platform integration is isolated under `adapters/`. The MT4
and MT5 directories are intentionally documentation-only until the real Manager
API SDKs and verified contracts are available.

## Runtime dependencies

- MySQL provides application-owned relational persistence.
- Redis provides cache and short-lived state infrastructure.
- Kafka provides asynchronous messaging infrastructure.
- Spring Boot Actuator provides health probes.

BrokerOS Risk must not directly modify an external CRM, broker, MT4, or MT5
database. Future integrations must use documented adapters and supported APIs
or SDKs.

## Deployment

The root Docker Compose file provides a local/test stack. Kubernetes Kustomize
manifests provide a common base with test and production overlays. Production
database credentials are intentionally externalized to a Kubernetes Secret.

## Phase 0 boundaries

The following are deliberately absent:

- Risk Case and Risk Rule business models
- risk detection and action execution workflows
- real MT4/MT5 Manager API implementations
- frontend framework selection
- Flink, Python, Elasticsearch, service mesh, and microservice decomposition

See `docs/adr/ADR-001-technology-roadmap.md` and
`docs/adr/ADR-002-system-isolation.md` for the accepted decisions.
