# Phase 0.5 Architecture Review

## Decision Rationale

Flyway was selected because schema history must be ordered, repeatable, and
auditable before persistence-backed business modules appear. The empty logical
baseline proves the mechanism without inventing business tables.

The API envelope, validation, and exception boundary were introduced together
because they form one external-contract foundation. Stable result codes prevent
clients from depending on exception messages, while centralized handling keeps
implementation details out of responses.

SpringDoc was added to make API behavior inspectable as it evolves. Spring Boot
Logback remains sufficient, so no logging or observability platform was added.

The Docker backend remains available, but behind an optional profile. This
preserves full-stack and image validation while avoiding rebuild latency during
normal Java development. Existing Kustomize base/test/prod overlays and the
single-repository modular-monolith layout remain appropriately small.

## Architecture Principle Changes

No existing product or architecture principle changed. Phase 0.5 adds concrete
engineering rules for Flyway, application API responses, centralized exception
handling, Bean Validation, and mandatory Review Packages.

## ADRs

- ADR-003: Engineering Foundation Standards
- ADR-004: Local Development and Deployment Layout

Both are accepted. ADR-001 and ADR-002 remain unchanged.

## Technical Debt Introduced or Retained

- Flyway startup was not executed against a real MySQL instance in this local
  environment because Docker is unavailable.
- Docker Compose semantic validation and image startup were not executable.
- Kustomize overlays could not be rendered because neither `kubectl` nor
  `kustomize` is installed.
- Production secret provisioning remains an environment responsibility.
- The generic result-code catalog will require deliberate expansion when
  approved APIs introduce specific failure semantics.

## Recommendations Before the Next Phase

- Obtain architect approval for Q-002 and both new ADRs.
- Add CI checks for Java 21 Maven test/package, Docker Compose config, and
  Kustomize rendering.
- Add a MySQL-backed Flyway integration test before the first migration that
  creates application tables.
- Define and approve the next requirement before adding any business module.
