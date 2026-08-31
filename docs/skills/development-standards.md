# Development Standards Skill

## When to use

Use this skill before and during every BrokerOS Risk Q-XXX, phase, bug fix,
refactor, technical task, code review, migration review, or Review Package.

It is mandatory after Phase 0.6. Read the applicable Requirement, architecture
documents, and accepted ADRs alongside this skill; this file does not override
them.

## Preflight workflow

1. Read root `AGENTS.md` completely.
2. Identify and read the governing `docs/requirements/Q-XXX-*.md`.
3. Read relevant files in `docs/architecture`.
4. Read every accepted ADR relevant to dependencies, boundaries, API, data,
   messaging, integrations, or deployment.
5. Check existing skills and latest Lessons Learned.
6. State the affected modules, contracts, data, integrations, security,
   auditability, compatibility, and operations.
7. If the request conflicts with a standard, name the rule and affected file;
   decide whether a Requirement/architecture/ADR change is required and stop
   before bypassing it.
8. Implement the smallest solution authorized by the Requirement.

## Architecture rules

- Stay broker-, CRM-, and trading-platform-neutral.
- Stay in one repository and one Spring Boot modular-monolith deployable during
  Phase 1.
- Put future modules under `com.brokeros.risk.<module>` and keep behavior within
  its module.
- Avoid dumping-ground packages and speculative abstractions.
- Put external integrations behind adapters; do not invent vendor SDKs or write
  to external databases.
- Keep risk detection separate from action execution and make critical actions
  auditable.

## Implementation patterns

### HTTP boundary

- Controller: request translation, `@Valid`, one service call, `ApiResponse<T>`.
- Request/response DTOs are distinct from entities and domain models.
- Expected failures use `BusinessException` plus stable `ResultCode`.
- Unexpected failures reach `GlobalExceptionHandler`; never return internal
  exception details.
- Treat a used ResultCode change as a breaking API change.

Example future package shape (do not create it until its Requirement exists):

```text
com.brokeros.risk.<module>
├── controller
├── service
├── repository
├── domain
├── dto
├── mapper
└── event
```

### Persistence

- Use Flyway only; add a new immutable `V<number>__<description>.sql`.
- Use `snake_case`, `BIGINT id`, readable enum codes, UTC timestamps, and
  explicit business identifiers.
- Review destructive DDL, constraints, indexes, data movement, compatibility,
  locks, and deployment ordering.
- In MySQL, a `CHECK` passes when its expression is `TRUE` **or `UNKNOWN`**.
  For nullable columns, encode both nullability directions explicitly with
  `IS NULL`/`IS NOT NULL`; an equality such as `nullable_status = 'ACTIVE'`
  alone does not reject `NULL`. Prove every direction against the supported
  real MySQL version, not only by reading the DDL.
- Mark high-risk migrations in the Review Package.

### Financial data

- Use `BigDecimal`/`DECIMAL`, never `float`/`double`, for money and critical
  trading values.
- Require currency for monetary values.
- Do not invent precision, scale, pip, point, contract-size, or MT4/MT5 volume
  conversions without a Requirement.

### State and audit

- Use enums with stable codes, never ordinals or magic strings.
- Express complex transitions through named domain behavior rather than
  arbitrary status setters.
- When an idempotency fingerprint includes an unordered multi-reference set,
  frame every raw value, de-duplicate it, and sort it deterministically before
  hashing. If the approved replay check precedes content validation, preserve
  invalid raw values in that canonicalization so an exact replay cannot be
  reinterpreted by later validation rules.
- For critical operations, design for who/when/what/target/before/after/reason/
  source even when the Audit module is not yet implemented.

### Kafka and Redis

- Topic: `brokeros.risk.<domain>.<past-tense-event>`.
- Event: `eventId`, `eventType`, `eventVersion`, `occurredAt`, `source`,
  `payload`; assess correlation and trace identifiers.
- Never publish an entity directly or create a topic without approved design.
- Redis key: `brokeros:risk:<domain>:<type>:<id>`.
- Declare TTL, invalidation, and durable source of truth; Redis is not the sole
  durable store.

### External calls and security

- Adapter boundaries isolate protocol, auth, vendor DTO, error mapping, timeout,
  and retry.
- Design bounded selective retries, idempotency, duplicate handling, and partial
  failure; never use infinite/catch-all retries.
- Never hard-code, commit, return, or log credentials, secrets, tokens, full
  authentication headers, or sensitive documents.

## Common mistakes

- Creating a module or table before its Q-XXX is approved.
- Returning an entity or a new response wrapper from a controller.
- Calling a repository, Kafka, Redis, or adapter directly from a controller.
- Throwing generic runtime exceptions for expected application outcomes.
- Editing a migration already applied to a shared environment.
- Using JVM local time, enum ordinals, magic strings, or floating-point money.
- Treating Redis as authoritative persistent storage.
- Publishing speculative topics/events or SDK interfaces.
- Writing a Review Package that says only `Compliant` without evidence.
- Marking an unavailable verification as PASS.

## Validation checklist

### Architecture

- Does the change comply with `AGENTS.md`, architecture documents, and accepted
  ADRs?
- Are module ownership and dependencies explicit, with no cycles or giant
  services?
- Was every new dependency necessary?

### API and compatibility

- List added, changed, and removed endpoints.
- List ApiResponse and ResultCode changes.
- Identify breaking changes or state `No API impact` with evidence.

### Database and performance

- List migrations, tables, columns, DDL/DML, indexes, and constraints.
- Check destructive changes, data compatibility, N+1, loop SQL, scans, locks,
  blocking I/O, and Redis misuse.
- If none, state `No database change` and identify the checked migration path.

### Security and auditability

- Check injection, XSS, authorization bypass, secret leakage, sensitive logs,
  and unsafe error responses.
- Explain how critical changes remain auditable, or state why the task has no
  critical-action impact.

### Risk-system impact

Explicitly check Risk Case, Rule Engine, Account Control, Audit, Kafka, Redis,
MT4, MT5, BrokerPilot, oneZero, and CRM. Record `No impact` when applicable,
with the scope evidence.

### Skill and lessons

- State whether a skill was added, updated, or unnecessary, and why.
- Create an honest Lessons Learned entry for a phase or important Requirement.

## Review Package gate

Generate `review/` only after implementation and verification are final.
`ArchitectureReview.md` must contain `Development Standards Compliance` and
substantive evidence for:

1. AGENTS.md compliance;
2. architecture compliance;
3. ADR compliance;
4. API standard compliance;
5. database standard compliance;
6. security standard compliance;
7. auditability compliance;
8. skill compliance.

For a violation, name the exact file, problem, and repair. Do not mark the
review PASS until it is fixed or an explicit standards-changing decision is
approved.

## Required completion sequence

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
