# BrokerOS Risk Agent Guide

## Scope

This file applies to the entire repository. More specific `AGENTS.md` files may
add rules for their own subdirectories, but they must not weaken the product,
architecture, requirements, or Definition of Done constraints defined here.

## Long-term Standards Authority

Before every Q-XXX, phase, bug fix, refactor, code review, or technical task,
read and comply with:

1. this `AGENTS.md`;
2. the governing Requirement and applicable files in `docs/architecture`;
3. all applicable accepted ADRs in `docs/adr`;
4. `docs/skills/development-standards.md`;
5. applicable repository skills and recent Lessons Learned.

Phase 0.6 development standards remain mandatory until an explicit Requirement
and architecture decision/ADR changes them. If a request conflicts with an
existing standard, identify the exact conflict and its authority, determine
whether the Requirement, architecture, or an ADR must change, and obtain an
explicit new decision before implementation. Never bypass or weaken a standard
silently.

## Product Boundary

BrokerOS Risk is an independent trading risk management platform for Forex/CFD
brokers.

- Keep the product broker-neutral, CRM-neutral, and trading-platform-neutral.
- Do not bind domain logic to a specific broker, CRM, MT4, MT5, or another
  external vendor.
- Isolate external-system details behind adapters.

## Current Architecture Phase

The project is in **Phase 1**.

Approved stack:

- Java and Spring Boot
- MySQL
- Redis
- Kafka
- Docker
- Kubernetes

Do not introduce Flink or Python unless an approved architecture decision
explicitly advances the project to a phase that permits them.

## Architecture Principles

1. Prefer a modular monolith over microservices during Phase 1.
2. Integrate external systems through adapters.
3. Never directly modify another system's database.
4. Keep MT4 and MT5 integrations in adapters.
5. Never invent Manager API interfaces without the real SDK.
6. Separate risk detection from risk action execution.
7. Make critical actions auditable.
8. Make broker-specific policies configurable rather than hard-coded.
9. Avoid unnecessary abstractions.
10. Record every important architectural decision in an ADR.

## Required Development Workflow

Use this sequence for every change:

1. Record or identify the approved Requirement.
2. Read and analyze relevant architecture documentation.
3. Check accepted ADRs, repository skills, and Lessons Learned.
4. Analyze the impact on modules, API contracts, data, integrations, security,
   auditability, compatibility, and operations.
5. Implement the smallest coherent change that satisfies the Requirement.
6. Add or update tests and run compilation plus applicable verification.
7. Add/update a reusable skill and an honest Lessons Learned entry when required.
8. Generate the final Review Package and complete standards-compliance review.
9. Obtain architect review before starting the next Requirement.

If a referenced document, SDK, requirement, or decision is missing, do not
invent its contents. State the gap and resolve it before making a choice that
would materially change product behavior or architecture.

## Requirements Discipline

- Requirements use stable IDs in the form `Q-001`, `Q-002`, `Q-003`, and so on.
- Preserve the meaning of approved requirements.
- Never silently reinterpret, broaden, narrow, or replace an approved
  requirement.
- Trace implementation and tests to the relevant requirement ID when one
  exists.
- Surface conflicts or ambiguity explicitly before making a change that would
  alter approved behavior.
- Do not implement formal business behavior directly from chat without a
  `docs/requirements/Q-XXX-*.md` source.

## Module and Package Standards

- Use root package `com.brokeros.risk` and future module packages
  `com.brokeros.risk.<module>`.
- Add internal `controller`, `service`, `repository`, `domain`, `dto`, `mapper`,
  and `event` packages only when the module requires them.
- Avoid dumping-ground packages such as `common`, `utils`, `misc`, `helper`,
  `manager`, `processor`, or `handler`. `com.brokeros.risk.common` is permitted
  only for genuine cross-module platform infrastructure.
- Keep business logic inside its owning module and preserve one Spring Boot
  modular-monolith deployable during Phase 1.
- Controllers only translate HTTP, apply Bean Validation, call a service, and
  return `ApiResponse`; they do not access repositories, SQL, Redis, Kafka,
  adapters, rules, or audit logic directly.
- Services orchestrate clearly scoped use cases. Do not create ambiguous giant
  services such as `RiskService`, `CommonService`, `BusinessService`, or
  `MainService`.
- Separate request DTOs, response DTOs, persistence entities, and domain models.
  Never expose an entity through an API.

## Integration and Data Safety

- Treat external CRMs, broker systems, and trading platforms as independently
  owned systems.
- Access them only through documented adapter contracts and supported APIs or
  SDKs.
- Do not couple the core domain to external database schemas.
- Do not write directly to external-system databases.
- Keep detection decisions distinct from action commands and action execution.
- Persist enough context to audit critical decisions, requests, attempts,
  outcomes, actors, and timestamps.
- Never claim that an MT4/MT5 Manager API operation is supported unless it is
  backed by the real SDK and verified integration behavior.

## Engineering Foundation Rules

- Apply every application-owned database schema change through a versioned
  Flyway migration. Never edit an already-applied migration.
- Return `ApiResponse` from every application-owned REST API. Framework-managed
  operational endpoints such as Actuator and OpenAPI keep their required
  protocol formats.
- Route application exceptions through `GlobalExceptionHandler`; never expose
  implementation stack traces in API responses.
- Apply Jakarta Bean Validation annotations to request DTOs and use `@Valid` at
  REST boundaries.
- Keep Spring Boot's default Logback implementation unless an approved
  requirement and ADR justify changing it.
- Treat used ResultCodes as stable external contracts. Do not pre-create
  speculative business codes or change existing code representations silently.
- Use `/api/<resource>` for application endpoints. API versioning requires a
  Requirement and ADR.
- Expected application failures use `BusinessException`; generic runtime
  exceptions must not represent normal business states.

## Data Standards

- Use `snake_case` database objects, `BIGINT` internal primary keys named `id`,
  and separate approved external business identifiers.
- Use Flyway exclusively; prohibit `ddl-auto=update`, immutable shared
  migrations, and unrecorded production DDL.
- Review every migration for destructive DDL/DML, default/null/unique/index
  changes, data movement, locking, and compatibility. Mark high-risk migrations
  in the Review Package.
- Persist time in UTC and make broker/client/trading-server conversions explicit.
  Never rely implicitly on JVM local time.
- Use `BigDecimal`/`DECIMAL` for money and critical trading values, never
  `float`/`double`. Monetary values require currency; precision and conversion
  rules require a Requirement.
- Use enums with stable readable codes, never ordinals or scattered magic
  strings. Express complex state transitions through named domain operations.

## Audit, Messaging, Cache, Security, and Adapter Standards

- Design critical future actions to capture who, when, what, target,
  before/after, reason, and source. Do not invent the Audit module in advance.
- Kafka topics use `brokeros.risk.<domain>.<past-tense-event>`. Create no topic
  without Requirement and architecture approval; events are versioned contracts
  and never persistence entities.
- Redis keys use `brokeros:risk:<domain>:<type>:<id>` and declare TTL,
  invalidation, and source of truth. Redis is not the sole durable data store.
- Never log or commit passwords, tokens, secrets, full authentication headers,
  KYC documents, or sensitive personal-document data.
- External calls stay behind adapters and explicitly define timeout, bounded
  selective retry, idempotency, duplicate handling, and partial failure.
- Never introduce infinite retries or retry all exceptions indiscriminately.

## Git, ADR, Skill, and Lessons Standards

- Keep `main` buildable. Prefer lightweight `feature/<requirement>`,
  `fix/<description>`, `refactor/<description>`, or `chore/<description>`
  branches when branches are requested.
- Commit messages use `<type>: <description>` with `feat`, `fix`, `refactor`,
  `docs`, `test`, `chore`, `build`, or `ci`.
- ADRs are evaluated for framework/database additions and changes to system
  boundaries, module/API/database/Kafka/deployment strategy, or major
  dependencies. New ADRs contain Context, Decision, Alternatives, and
  Consequences.
- Every completed phase or Requirement evaluates `docs/skills`. Skills capture
  reusable rules, patterns, mistakes, validation, and examples—not changelogs.
- Every phase or important Requirement adds an honest
  `docs/lessons/YYYY-MM-DD-<phase-or-requirement>.md`. Never invent problems.

## Mandatory Review Package

Every completed phase or requirement must finish with an up-to-date review
package in `review/`. Do not report the work complete until all of these files
exist and reflect the final repository state:

- `review/Summary.md`
- `review/ArchitectureReview.md`
- `review/ProjectTree.txt`
- `review/GitStatus.txt`
- `review/GitDiffStat.txt`
- `review/Verification.md`
- `review/OutstandingItems.md`

The package must identify the phase or requirement, changed files and design
decisions, architecture impact, executed verification commands and results,
remaining risks, deferred work, and next-phase recommendations. Generate only
a bounded project tree and diff statistics; never place the complete Git diff
in the review package.

Every `review/ArchitectureReview.md` must include a substantive
`Development Standards Compliance` section with evidence for:

- AGENTS.md compliance;
- architecture compliance;
- ADR compliance;
- API standard compliance;
- database standard compliance;
- security standard compliance;
- auditability compliance;
- skill compliance.

Do not write only `Compliant`. Cite the inspected scope and explain the result.
For any violation, name the exact file, problem, and repair. An unresolved
standards violation prevents the review from being marked PASS.

## Definition of Done

A task is complete only when all applicable conditions are satisfied:

- The code compiles.
- Relevant automated tests pass.
- No obvious architecture violation remains.
- Documentation is updated when architecture or behavior changed.
- Important architecture decisions have an ADR.
- Reusable skills and Lessons Learned are updated when required.
- Development Standards Compliance is evidenced with no unresolved violation.
- The mandatory review package is complete and current.
- The final handoff summarizes the changes and reports verification performed.

If any condition cannot be completed, report the task as incomplete and state
the specific blocker or unverified item.
