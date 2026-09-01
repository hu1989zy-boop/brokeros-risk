# Q-013 Architecture Review

## Review conclusion

The Q-013 implementation matches Requirement V1, approved Architecture V1,
ADR-015, and Implementation Design V1 within the inspected Action scope. No
Q-013 architecture deviation or development-standards violation was found.
The lifecycle gate remains **BLOCKED** because the mandatory full repository
test gate is not green due to the unchanged Q-012 migration-count assertion.

## Architecture and module boundary

- Ownership is contained in com.brokeros.risk.action inside the existing
  modular monolith.
- Domain/application depend only on Java, Q-009 security contracts, and the
  Q-012 Decision provenance contract; they do not import Spring, JDBC, Kafka,
  Redis, servlet, or vendor APIs.
- Infrastructure implements Action-owned ports through Spring JDBC and the
  existing transaction manager.
- ActionRecord imports DecisionRef as an opaque read-only domain dependency.
  V6 stores decision_ref as checked CHAR(40) without a cross-module SQL foreign
  key.
- No Q-008 package, consumer wiring, ActionOutcome, Execution, Account Control,
  eligibility service, or approval workflow was created.
- No library, Maven dependency, deployable, Kafka topic, Redis key, adapter, or
  deployment manifest changed.

## API and compatibility impact

Added endpoints:

- POST /api/actions
- GET /api/actions/{actionRef}

Both use ActorContextProvider, Bean Validation, a single application-service
call, and ApiResponse. No PUT, PATCH, correction, transition, or delete route
exists. Eight ACTION_* ResultCodes were added without changing existing codes.
No existing endpoint or response contract changed.

## Data and operational impact

V6 is additive, forward-only, and schema-only. It creates exactly three
application-owned InnoDB tables. There is no seed data, destructive DDL/DML,
data movement, existing-table lock, or existing-column compatibility change.
The only real foreign keys are Action-internal and use ON DELETE RESTRICT.
The migration was applied, validated, restarted, constraint-tested, and
query-plan-tested on disposable MySQL 8.4.11.

## Development Standards Compliance

### AGENTS.md compliance

The inspected change stays in the Phase 1 Java/Spring Boot modular monolith,
uses Flyway for the sole schema change, preserves broker/platform neutrality,
separates Action intent from execution, and records authorship plus access
audit. Git scope checks found no forbidden Q-008-Q-012 module change and no
existing migration edit. No commit or push occurred.

### Architecture compliance

The Action module owns its domain and persistence. Controller code only
translates HTTP, applies validation, calls a service, and wraps ApiResponse.
Application services own use-case ordering; JDBC details remain in
infrastructure. Q-012 is consumed through DecisionProvenanceQueryService, not
through its repository or table. The architecture test scans domain and
application imports and prohibits infrastructure, framework, and vendor
leakage.

### ADR compliance

ADR-015's decisions are represented directly: one DecisionRef column; no join
table; PROPOSED-only status retained as a real CHECK-constrained column;
immutable recording-only Action; two-tier reads; no cross-module FK; no
approval workflow; and no vendor execution semantics. No new architectural
decision requiring another ADR was introduced.

### API standard compliance

ActionController exposes only the two approved /api/actions routes.
RecordActionRequest uses Jakarta Bean Validation. Request, response, domain,
and persistence shapes are separate. Both endpoints return ApiResponse.
Expected failures use ActionException/BusinessException and the eight approved
ResultCodes; no entity is exposed.

### Database standard compliance

V6 uses snake_case objects, BIGINT id primary keys, ASCII binary-collated
opaque references, readable enum values, VARBINARY content, UTC-compatible
DATETIME(6), named constraints, indexes, and ON DELETE RESTRICT. Real-MySQL
tests cover each Design §8.4 constraint, both FK restrictions, zero seed data,
Flyway validate/restart, and indexed query plans. V1-V5 hashes/diffs were not
changed.

### Security standard compliance

AuthorizationGuard is called before every Action lookup or mutation.
Recording checks HUMAN immediately after authorization and before replay.
Reads accept authorized SERVICE actors. The Q-012 confirmation receives the
recording actor's own ActorContext. Prepared JDBC statements are used. No
credential, intent text, DecisionRef, or actor identity is logged or used as a
metric tag.

### Auditability compliance

Recording atomically persists the immutable Action and operation ledger with
actor, source, time, DecisionRef, semantic fingerprint, operation identity,
and outcome. Full-detail access commits action_access_log in a dedicated
REQUIRES_NEW, non-read-only transaction before disclosure. Forced audit-write
failure prevents content return and remains isolated from an unrelated
recording transaction.

### Skill compliance

The mandatory development-standards skill and the personal
brokeros-review-package skill were applied. The existing migration-count lesson
was enforced through a dynamic Q-013 assertion and a static guard. No new
docs/skills rule was necessary because the reusable rule already existed; an
honest Q-013 implementation lesson was added.

## Findings

No in-scope architecture or standards finding.

External blocker: Q012MySqlMigrationTests line 45 hard-codes one migration
after V4. V5 plus V6 produce two. Repairing that Q-012 file is explicitly
outside this implementation authority.

## Gate Decision

**BLOCKED** — architecture/design compliance passes, but implementation
verification cannot pass while the required full repository gate has one
failure.
