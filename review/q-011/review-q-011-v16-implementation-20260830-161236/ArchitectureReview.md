# Q-011 Architecture Review

## Authority and scope inspected

The implementation was checked in the mandated authority order against
Requirement Q-011 V3, Architecture V4, amended/re-accepted ADR-013, and
Implementation Design V5, followed by AGENTS.md, development standards,
the unchanged Q-009 security module, and the unchanged Q-010 trading-account
authority module.

The result remains a single Spring Boot modular-monolith deployable. Q-011
owns its domain and ports; SQL and Spring transaction concerns remain in
infrastructure; Q-009 authorization and Q-010 recognition are reused through
their existing application contracts. No new system boundary, framework,
database engine, dependency, Kafka/Redis contract, or deployment unit was
introduced, so no new ADR beyond the already amended ADR-013 is indicated.

## Architectural findings

- Domain types depend only on JDK and existing domain value types. Application
  services import no JDBC, Servlet, JPA, vendor schema, or persistence row.
- `EvidenceController` is the sole HTTP input adapter and is Servlet-Web
  conditional so the existing Q-010 non-Web bootstrap remains constructible.
- Q-010 is called only by new recording before mutation; correction copies the
  stored target subject and never calls Q-010.
- `EvidenceProvenanceView` contains bounded provenance only and has no
  observation or correction-reason component.
- Full-detail access auditing is isolated in a short `REQUIRES_NEW`, explicitly
  non-read-only transaction that completes before content is returned.
- V4 is additive and creates exactly four InnoDB tables. V1–V3 are untouched.
- No Decision, Action, ActionOutcome, Rule Engine, Risk Case, Q-008 wiring,
  file/blob storage, Kafka, or Redis implementation appears in scope.

## Development Standards Compliance

### AGENTS.md compliance

Evidence inspected: the authorized Q-011-only file set, `git diff` protected
path scan, full test/package commands, Lessons Learned, skill update, and this
non-overwriting review package. The implementation follows the required
Requirement → architecture/ADR → implementation → tests → skill/lessons →
review sequence. No stage, commit, or push occurred. The unresolved Q-009 test
gate is reported rather than hidden, so this review is not marked PASS.

### Architecture compliance

Evidence inspected: every class under `com.brokeros.risk.evidence`, package
dependency tests, module configuration, and REST surface. Business invariants
remain in domain/application; JDBC, SQL exception classification, transaction
templates, and row mapping remain under infrastructure. External authority
details are reused behind Q-009/Q-010 application contracts. No microservice,
vendor binding, or unnecessary generic package was added.

### ADR compliance

Evidence inspected: ADR-013 including its re-accepted amendment and the final
record/correct/read flows. Stable opaque references, append-only correction,
typed subject, idempotent operation ledger, two-tier reads, HUMAN-only
authoring, and pre-return access auditing match the accepted decision. No
implementation decision changes the accepted boundary or requires another ADR.

### API standard compliance

Evidence inspected: `EvidenceController`, request/response DTOs, ResultCode,
and REST contract tests. The three `/api/evidence` routes return
`ApiResponse`, apply Jakarta Bean Validation with `@Valid`, accept no caller
actor/status/time/generated EvidenceRef, expose entities neither directly nor
through persistence rows, and use only Design §13 Q-011 ResultCodes plus
existing Q-009 authorization codes.

### Database standard compliance

Evidence inspected: V4 DDL, real MySQL 8.4.11 metadata/constraint/query-plan
tests, rollback/concurrency tests, and unchanged V1–V3 diff scan. Tables use
snake_case, BIGINT `id`, UTC `DATETIME(6)`, readable code values, named FKs,
checks, and indexes. V4 contains no destructive/data-seeding DDL/DML. The
nullable `before_status` check explicitly rejects SQL `NULL`; this was proven
against MySQL rather than inferred from DDL text.

### Security standard compliance

Evidence inspected: exact service execution order, real Q-009/Q-010
integration tests, architecture scans, and full-detail audit path. Trusted
`ActorContext` supplies the actor. Each use case authorizes before lookup or
replay; Record/Correct then require HUMAN. Reads add no actor-type restriction.
Denied/unavailable dependencies call no Q-011 data port. No content/reason is
logged or placed in metrics.

### Auditability compliance

Evidence inspected: operation/history schema, mutation adapter transaction
tests, and access-log transaction tests. Every successful mutation retains
actor, capability, UTC time, operation, target/result, subject-linked record,
reason where required, and before/after status atomically. Full-detail read
persists actor/time access before disclosure and denies disclosure on audit
failure.

### Skill compliance

Evidence inspected: `docs/skills/development-standards.md` and
`docs/lessons/2026-08-30-q-011-implementation.md`. A reusable rule now states
that MySQL `CHECK` expressions over nullable fields must encode both nullability
directions and test `NULL` on the supported real engine. The Lessons Learned
entry records actual defects found and repaired, plus the unresolved Q-009 gate.

## Review conclusion

No unresolved architecture or development-standard violation was found inside
the authorized Q-011 implementation scope. Nevertheless, the repository-wide
Acceptance Criterion 15 is not satisfied because the unchanged Q-009
real-MySQL gate fails its obsolete fixed migration-count assertion. Therefore
this package is **not** an implementation sign-off and awaits Claude Code's
independent review and Product Owner disposition.
