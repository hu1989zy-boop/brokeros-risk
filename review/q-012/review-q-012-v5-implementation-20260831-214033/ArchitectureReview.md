# Q-012 Architecture Review Evidence

## Architecture result

The implementation follows the approved Q-012 module and dependency direction:
Decision depends on the public Q-009 authorization, Q-010 reference-eligibility,
and Q-011 provenance-read contracts; none depends back on Decision. JDBC and
Spring concerns remain in infrastructure/interfaces. Domain and application
architecture tests reject framework, persistence, and vendor imports.

No undocumented architecture change was introduced. `backend/pom.xml` is
unchanged; there are no new services, adapters to external vendors, Kafka
topics, Redis keys, or deployment units. ADR-014's immutable Decision and
reference-only cross-module boundary are preserved.

## Approved design alignment

- Implementation Design §11.1 order is implemented in
  `DecisionRecordingService`: authorize → require `HUMAN` → raw semantic
  fingerprint → replay check → content parsing → Q-010 validation → Q-011
  validation → authorized mutation context → atomic mutation.
- Exactly four Decision tables exist. The only foreign keys are the three
  intra-module references to `decision_record.id`.
- `DecisionSource` contains only `MANUAL`; operation type/outcome contain only
  `RECORD`/`CREATED`.
- The narrow read is in process only. The HTTP controller exposes exactly record
  and full-detail routes.
- Full-detail disclosure is preceded by a committed, dedicated access-log
  transaction.

## Development Standards Compliance

### AGENTS.md compliance

Inspected the complete Q-012 changed scope, `git status`, migration inventory,
HTTP routes, ResultCodes, tests, lesson, skill update, and this new review
package. The change is a Phase-1 Java/Spring/MySQL modular-monolith addition.
No broker/CRM/MT4/MT5 binding, external DB write, Q-008 work, commit, stage, or
push occurred. The unresolved full-database regression prevents a PASS gate and
is reported instead of concealed.

### Architecture compliance

The module uses the approved domain/application/port/infrastructure/interfaces
shape. `DecisionArchitectureTests.domainAndApplicationHaveNoFrameworkPersistenceOrVendorImports`
passed. Decision consumes narrow existing contracts and does not reach into
Q-010/Q-011 tables from core logic. No new application boundary or deployment
model was introduced.

### ADR compliance

ADR-014 was inspected and implemented: Decisions are record-once and immutable;
the Evidence relationship is a normalized join table; cross-module refs are
validated opaque columns without cross-module SQL foreign keys; narrow and
full-detail reads remain separate. No correction/history table or delete path
exists. No new ADR trigger was found because there is no new framework,
database technology, dependency, system boundary, or deployment strategy.

### API standard compliance

`DecisionController` returns the repository-standard `ApiResponse`, applies
`@Valid`, obtains the actor from the trusted context provider, and delegates to
one service. Request, response, domain, and persistence shapes are separate.
Only `/api/decisions` POST and `/api/decisions/{decisionRef}` GET exist. Ten
approved additive ResultCodes were added; expected failures use Decision's
`BusinessException` hierarchy and remain handled by `GlobalExceptionHandler`.

### Database standard compliance

The only schema change is additive immutable V5. It uses `snake_case`, internal
`BIGINT id`, UTC `DATETIME(6)`, ASCII binary collation for canonical identifiers,
and `VARBINARY(4000)` for exact UTF-8 conclusion bytes. Real MySQL tests inspect
and enforce every named key/check/index, upgrade/validate/restart, no seed data,
query-plan index use, transaction rollback, collision retry, and concurrency.
No existing migration was edited and no destructive DDL/DML exists.

### Security standard compliance

Authorization precedes every Decision port. Recording additionally fails
non-`HUMAN` actors before replay/content/external calls. Q-010 and Q-011 calls
use the caller's own `ActorContext` and fail closed on denial/unavailability.
SQL is parameterized. Conclusion, subject, Evidence refs, actor identity,
credentials, and auth headers are absent from logs and metric tags. The narrow
type structurally excludes conclusion text; full detail fails closed when its
audit write fails.

### Auditability compliance

Recording atomically stores the immutable Decision, every Evidence reference,
operation id, semantic fingerprint, outcome, actor, and UTC time. Full-detail
reads record accessing actor and time before disclosure. Forced rollback and
failed-audit tests prove no partial or unaudited success path.

### Skill compliance

The mandatory `docs/skills/development-standards.md` was applied. A reusable
rule emerged for deterministic idempotency fingerprints over unordered raw
multi-reference input, especially when replay precedes validation; that skill
was updated. `docs/lessons/2026-08-31-q-012-implementation.md` records both the
successful patterns and the real blockers without inventing incidents.

## Architecture deviations and violations

No implementation deviation from Requirement V1, Architecture V1, or ADR-014
was identified. One unresolved verification conflict exists: Design §16.7/§18
requires unchanged earlier database tests to pass, but the unchanged Q-011
migration-count assertion is not forward compatible with V5. Prompt hard
boundaries prohibit the only local repair in this stage. This is recorded as a
BLOCKED gate, not marked compliant or waived.

## Gate Decision

**BLOCKED** pending an authorized resolution of the Q-011 forward-compatibility
test conflict and a subsequent zero-failure all-database rerun. This package is
evidence for independent review, not an architecture or completion sign-off.
