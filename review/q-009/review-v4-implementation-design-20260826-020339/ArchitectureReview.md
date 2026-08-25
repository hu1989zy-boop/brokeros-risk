# Q-009 Implementation Design Architecture Review

## Review Result

Architecture Review: **PASS FOR ARCHITECT REVIEW**

Design Status: **Draft — awaiting architect approval**

Implementation Authorized: **NO**

## Architecture Impact

The proposed future change adds one `com.brokeros.risk.security` feature module,
three application-owned MySQL tables, a Spring Security JWT inbound adapter,
framework-neutral mapping/context/authorization contracts, a controlled
bootstrap command, and safe security failure adapters. It preserves one Spring
Boot modular-monolith deployable and creates no external integration or domain
implementation.

## Development Standards Compliance

### AGENTS.md compliance

The governing Q-009 Requirement V1, Architecture V2, accepted ADR-011, Q-007/
Q-008 authorities, development standards, configuration/observability skills,
Lessons Learned, current packages, configuration, migration, dependencies, and
tests were inspected. This phase adds documentation/review artifacts only. No
production implementation, Git staging, commit, push, reset, clean, or stash was
performed.

### Architecture compliance

The design retains Phase 1 Java/Spring Boot/MySQL, one deployable, adapter
isolation, explicit application authorization, auditability, and broker-neutral
core contracts. It adds no microservices, Flink, Python, vendor coupling,
external database access, or direct action execution.

### ADR compliance

ADR-007 separation of correlation from identity, ADR-008 native versus owned
configuration, ADR-009 opaque ActorRef, ADR-010 Q-008 trusted actor dependency,
and ADR-011 pluggable hybrid/capability decisions are explicitly preserved.
No new ADR is proposed because this design implements the decisions already
accepted in ADR-011 without changing a system boundary or technology category.

### API standard compliance

No endpoint or DTO is added. The design requires future Spring Security filter
failures to reuse `ApiResponse<ErrorResponse>` and stable ResultCodes. It
prohibits actor identity fields in request DTOs and keeps controllers as
translation-only adapters.

### Database standard compliance

The proposed schema uses a new forward-only Flyway V2, snake_case, BIGINT
internal IDs, separate opaque actor reference, UTC `DATETIME(6)`, stable enum
codes, exact collations, bounded fields, FK/check/unique constraints, no
destructive migration, and MySQL 8.4 verification. No SQL is created now.

### Security standard compliance

The design validates signed JWT trust properties, defaults deny, separates
provider claims from BrokerOS capabilities, uses least privilege, has no global
SYSTEM/admin, fails closed, avoids permissive test providers, and excludes
credentials/full claims from persistence, logs, errors, Audit, and correlation.

### Auditability compliance

ActorRef/type, authentication method/source, capability, outcome, safe reason,
UTC time, and optional correlation are available for future Audit. Service work
uses a stable purpose-specific ActorRef. Q-009 does not invent the Audit module
or store sensitive provider payloads.

### Skill compliance

The configuration skill controls native resource-server properties, the owned
clock-skew property, catalog expectations, fail-fast/startup-bound settings,
profile limitations, and secret safety. The observability skill controls filter
ordering, request/trace separation, MDC ownership, and logging exclusions. The
development standards govern packages, Flyway, API responses, tests, and Review.
No reusable security skill is added in design because the proposed pattern has
not been implemented and verified; the Lessons Learned records this explicitly
and implementation must reevaluate skill extraction.

## Violations

Unresolved standards violations: **NONE IN THE DESIGN ARTIFACT**.

Runtime compliance remains unverified until a separately authorized
implementation and final standards Review.
