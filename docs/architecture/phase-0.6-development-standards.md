# Phase 0.6 Development Standards

## 1. Purpose and authority

These standards govern every future BrokerOS Risk phase, requirement, bug fix,
refactor, review, and technical task. They supplement the product and
architecture rules in `AGENTS.md` and the accepted ADRs; they do not replace or
weaken them.

Before work begins, read the applicable Requirement, architecture documents,
accepted ADRs, and `docs/skills/development-standards.md`. If a request conflicts
with them, identify the exact conflict and determine whether a new or amended
Requirement, architecture decision, or ADR is required. Do not silently bypass
an existing rule.

Phase 0.6 does not change the Java 21, Spring Boot, Maven, MySQL, Redis, Kafka,
Flyway, Docker, Kubernetes, OpenAPI, single-repository, modular-monolith
direction. No business functionality is introduced.

## 2. Package and module boundaries

The root Java package is `com.brokeros.risk`. Future business capabilities use
`com.brokeros.risk.<module>`, with internal packages only when needed:

- `controller` — HTTP translation and validation;
- `service` — use-case orchestration;
- `repository` — persistence ports/implementation;
- `domain` — business state and behavior;
- `dto` — request and response contracts;
- `mapper` — explicit boundary mapping;
- `event` — module-owned event contracts and publication logic.

Do not create broad dumping-ground packages such as `common`, `utils`, `misc`,
`helper`, `manager`, `processor`, or `handler`. A small
`com.brokeros.risk.common` package is permitted only for genuine platform
infrastructure shared across modules; convenience alone is not sufficient.

Business logic remains inside its owning module. Phase 1 retains one Spring
Boot deployable and one repository; package boundaries are not microservices.

## 3. HTTP and application-layer standards

Controllers only:

- receive HTTP input;
- trigger Jakarta Bean Validation;
- call a clearly scoped application/service operation;
- return `ApiResponse<T>`.

Controllers must not contain SQL, call repositories directly, implement complex
decisions or risk rules, access Redis, publish Kafka messages, call trading
adapters, or implement audit behavior. Application URLs use `/api/<resource>`.
API version prefixes require a separate Requirement and ADR.

Services orchestrate repositories, domain behavior, adapters, Kafka publishers,
Redis, and future audit events. Avoid catch-all names such as `RiskService`,
`CommonService`, `BusinessService`, or `MainService`; names must expose a single
responsibility. Complex domain decisions must not accumulate in one service.

Request DTOs use action-specific names such as `CreateXRequest`; response DTOs
use names such as `XResponse`, `XDetailResponse`, or `XSummaryResponse`.
Persistence models end in `Entity`. Domain models use domain names without DTO
or Entity suffixes. DTOs and entities are never interchangeable, and entities
must not be returned directly from APIs.

## 4. API response, result-code, and exception standards

Application-owned APIs use the Phase 0.5 `ApiResponse<T>`, `ErrorResponse`, and
`ResultCode` contract. Modules may not introduce their own wrappers. Actuator
and OpenAPI remain framework protocol exceptions.

Result codes are external contracts. Do not change a code already used by a
consumer without treating the change as breaking. Use codes only for real
requirements; do not pre-create speculative catalogs. Recommended numeric
ranges are:

- `1000-1999` — platform and request errors;
- `2000-2099` — Risk Case;
- `2100-2199` — Rule Engine;
- `2200-2299` — Account Control;
- `2300-2399` — Audit;
- `2400-2499` — external integration.

The existing Phase 0.5 symbolic result codes remain unchanged in Phase 0.6.
Migrating their external representation to numeric codes requires an explicit
API-contract Requirement and architecture review; it must not happen silently.

Expected application failures use `BusinessException` plus an explicit
`ResultCode`. Normal business states must not be represented by a generic
`RuntimeException`. Unexpected exceptions flow through `GlobalExceptionHandler`.
Java exceptions, SQL exceptions, internal messages, and stack traces never
appear in client responses; stack traces remain server-side logs.

## 5. Database and migration standards

Database identifiers use `snake_case`. Tables use domain nouns such as
`risk_case`, never Java casing or prefixes such as `tbl_` or `t_`. The default
primary key name is `id` and type is `BIGINT`. External business identifiers
must not depend directly on auto-increment IDs and require their own approved
strategy.

Common audit columns, when required by a table's Requirement, use
`created_at`, `updated_at`, `created_by`, and `updated_by`. Phase 0.6 does not
create any of these tables or columns.

Flyway is the only schema-change mechanism. Hibernate must not update formal
schemas (`ddl-auto=update` is prohibited). Migration names use
`V<number>__<description>.sql`. A migration applied to a shared environment is
immutable; corrective work uses a new migration. Unrecorded manual production
schema changes are prohibited.

Every migration review must explicitly examine:

- `DROP TABLE`, `DROP COLUMN`, `MODIFY COLUMN`, large-table `ALTER`, and data
  deletion;
- default and `NULL` changes;
- unique constraints, indexes, and compatibility;
- data migration, locking, deployment ordering, and rollback/forward-fix risk.

High-risk migrations must be identified in the Review Package. There are no
business migrations in Phase 0.6.

## 6. Data semantics

Persist system timestamps in UTC and use explicit API time formats. Never rely
implicitly on JVM local time. MT4 server time, MT5 server time, broker time,
client time, and UTC conversions require explicit future requirements.

Use `BigDecimal` and database `DECIMAL` for money and critical price, volume,
profit, commission, swap, markup, slippage, and markout calculations. Never use
`float` or `double` for them. Money fields require an explicit currency;
precision, scale, currency conversion, contract size, digits, point, pip, and
MT4/MT5 volume representation are requirement-specific and are not invented in
this phase.

Finite states use enums with stable, readable database codes. Never persist or
exchange Java enum ordinals. Complex state changes occur through named domain
operations such as `assign`, `close`, `restrict`, or `restore`, not arbitrary
`setStatus` calls. Actual states and transitions require business requirements.

## 7. Auditability

Critical future risk operations must be able to record who acted, when, what
operation occurred, the target, before/after state, reason, and source. This
includes account freeze/unfreeze, readonly/restore, blacklisting, case status,
rule changes, and manual decisions.

Phase 0.6 defines the obligation only. It creates no Audit module or table. Any
implementation must preserve the Phase 1 separation between detection and
action execution and must be driven by an approved Requirement.

## 8. Kafka and Redis

Kafka topics use `brokeros.risk.<domain>.<event>`. Event names are past-tense
facts such as `created`, `restricted`, or `restored`. Topics are created only
after Requirement and architecture approval.

Event contracts contain at least `eventId`, `eventType`, `eventVersion`,
`occurredAt`, `source`, and `payload`, with `correlationId` and `traceId`
considered where required. Contracts must be version-compatible; JPA entities
must never be Kafka messages.

Redis keys use `brokeros:risk:<domain>:<type>:<id>`. Every use declares purpose,
TTL, invalidation strategy, and source of truth. Redis is never the sole store
of durable business state; application-owned MySQL remains the default source
of truth.

## 9. Logging, security, adapters, and resilience

Keep Spring Boot Logback. Never log passwords, tokens, secrets, full
authentication headers, KYC documents, or sensitive personal-document data.
Critical-operation logs may include necessary identifiers but must minimize
sensitive data.

Do not hard-code or commit production credentials. Do not place production
secrets in `application.yml`; Kubernetes secrets remain environment-managed.
Authentication and authorization are not implemented in Phase 0.6.

External systems are accessed only through adapters that isolate protocol,
authentication, vendor DTOs, error mapping, timeout, and retry behavior.
Business modules do not call external SDKs directly. Calls must explicitly
consider timeout, bounded and selective retry, idempotency, duplicates, and
partial failure. Control commands require an idempotency design; infinite or
catch-all retries are prohibited.

## 10. Git, Requirement, ADR, skill, and lessons practices

Keep `main` buildable. Suggested lightweight branches are
`feature/<requirement>`, `fix/<description>`, `refactor/<description>`, and
`chore/<description>`. Commit messages use `<type>: <description>` with types
`feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `build`, or `ci`; commits
should have one responsibility.

Every formal feature begins with a `Q-XXX` document. Do not implement business
behavior directly from an unrecorded chat description.

Assess an ADR when adding a framework/database, changing boundaries, module/API
contract/database/Kafka/deployment strategy, or adding a major dependency. ADRs
record Context, Decision, Alternatives, and Consequences. Ordinary
implementation does not require an ADR.

After every phase or Requirement, evaluate `docs/skills` for reusable knowledge.
A skill documents when to use it, rules, patterns, mistakes, checks, and useful
examples rather than serving as a changelog.

Create honest Lessons Learned entries under `docs/lessons` using
`YYYY-MM-DD-<phase-or-requirement>.md`. Record implementation, rationale,
alternatives, encountered problems, lessons, reusable patterns, and future
risks. Never invent problems; explicitly say when none occurred.

## 11. Test and review standards

Business requirements test at least happy path, validation failure, business
failure, not found, and unexpected failure. State changes additionally test
valid, invalid, and repeated transitions. Persistence, Kafka, and account
control require appropriate integration, event/schema/duplicate, and idempotency
tests respectively.

Every code review examines:

- architecture and module boundaries, dependency value, service size, and
  cycles;
- API additions/changes/deletions, response/result-code compatibility, and
  breaking changes;
- database DDL/DML, indexes, constraints, migration and data compatibility;
- N+1, loop SQL, scans, blocking I/O, locks, and Redis misuse;
- injection, XSS, permission bypass, secret leakage, sensitive logs, and unsafe
  errors;
- impact on Risk Case, Rule Engine, Account Control, Audit, Kafka, Redis, MT4,
  MT5, BrokerPilot, oneZero, and CRM—even when the result is `No impact`;
- whether a skill was added, updated, or not required, with a reason.

## 12. Review Package compliance gate

Every completed phase or Requirement regenerates the root `review/` package.
Verification records Build, Tests, Docker, Database, Kafka, Redis, Kubernetes,
and static checks; unavailable checks are `NOT EXECUTED` with a reason, never
PASS.

Every `ArchitectureReview.md` includes `Development Standards Compliance` with
evidence for:

- AGENTS.md compliance;
- architecture compliance;
- ADR compliance;
- API standard compliance;
- database standard compliance;
- security standard compliance;
- auditability compliance;
- skill compliance.

A violation names the file, problem, and recommended repair. An unresolved
violation prevents a PASS conclusion. A bare `Compliant` statement is not
sufficient.

## 13. Required development sequence

The mandatory sequence is:

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

Correctness, traceability, auditability, safety, and compatibility take priority
over speed. Prefer simple, explicit, testable, auditable, and replaceable
solutions over speculative abstraction or distribution.
