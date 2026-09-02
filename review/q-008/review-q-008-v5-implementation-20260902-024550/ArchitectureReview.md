# Q-008 Architecture Review

## Review result

**PASS WITH CONDITIONS** for entry into independent implementation review.

The implementation preserves the approved Phase 1 modular-monolith boundary.
Risk Case is downstream of the Decision Core Domain, owns only case state and
case history, and stores bounded opaque references to upstream capabilities.
No new service, database, message topic, cache, framework, or deployment unit
was introduced.

## Boundary assessment

- `com.brokeros.risk.riskcase.domain` owns the aggregate and invariants and has
  no Spring, JDBC, servlet, Kafka, Redis, vendor, or upstream persistence
  dependency.
- Application services authorize before loading case data, invoke only bounded
  provider ports, perform one optimistic root CAS, append command-owned
  history, append one independently owned Audit Record, and commit once.
- `com.brokeros.risk.audit` exposes only an append port and JDBC writer. Audit
  does not depend on Risk Case domain classes and has no controller,
  administration, retention, or messaging capability.
- Thin adapters bind Q-008 ports to Q-010 Trading Account eligibility and the
  Q-011 Evidence, Q-012 Decision, Q-013 Action, and Q-014 ActionOutcome query
  services. There are no upstream table reads or cross-module foreign keys.
- Risk Case records Action and ActionOutcome references only. It contains no
  execution command, vendor interpretation, success taxonomy, MT4/MT5/CRM/
  bridge/LP vocabulary, retry executor, or external database write.

## Domain and lifecycle

The aggregate exposes `openManual`, `openDecisionDriven`, `beginReview`,
`cancel`, `markActionRequired`, `resolve`, `returnToReview`, `close`,
`resumeResolvedCase`, `reopenClosedCase`, and the approved non-transition
operations. It exposes no public `setStatus`. `CANCELLED` remains terminal;
resolved/closed reopen operations increment the cycle, clear the current
Decision pointer, and retain prior immutable records. Every material operation
increments the aggregate version once.

The root snapshot stays bounded: Evidence, Decision, Action, Resolution, note,
and audit histories are queried independently rather than loaded as an
unbounded aggregate graph.

## Persistence and migration review

`V8__create_risk_case_foundation.sql` is the next unused migration and creates
exactly the 13 Design §8 tables. It is additive schema-only DDL: no existing
table alteration, destructive DDL/DML, data movement, or upstream foreign key.
Application-owned child/self/snapshot relationships use restrictive deletion.
The schema enforces the global primary-Decision association, one resolution per
case cycle, root assignment/status/Decision shapes, positive cycle/version,
case-number uniqueness, actor-scoped create idempotency, and deterministic
history indexes.

Migration risk is **MEDIUM** because 13 tables and their constraints/indexes
must be created and because `audit_record` establishes a new independent
platform boundary. Risk is bounded by additive-only DDL, empty-table creation,
no data backfill, real MySQL 8.4 migration tests, exact metadata assertions,
and successful full repository migration/test execution.

## Transaction, concurrency, and auditability

- Root CAS uses `UPDATE risk_case ... WHERE id = ? AND version = ?` and requires
  exactly one row.
- History and audit inserts share the same Spring transaction and do not use
  `REQUIRES_NEW`.
- Real-MySQL failure injection proved history failure rolls back root/audit and
  audit failure rolls back root/history.
- Concurrent assignment, resolution, close-versus-resume, reopen, primary-
  Decision creation, and evidence association tests prove one winner and no
  loser-owned history/audit rows.
- Read audit is fail closed and does not increment the aggregate version.
- Audit JSON is restricted to bounded state codes, identifiers, and hashes; it
  excludes note text, intake summary, credentials, and upstream payloads.

## Development Standards Compliance

### AGENTS.md compliance

Inspected the complete Q-008 production/test tree, V8 migration, ResultCode
change, repository status, and verification scripts. The implementation is a
single Java/Spring Boot modular-monolith capability, uses MySQL/Flyway/JDBC,
does not introduce forbidden technology, and leaves upstream Q-009 through
Q-014 code and existing migrations untouched. A new non-overwriting Review
Package, Lessons Learned, and reusable skill rule are included. No staging,
commit, or push occurred.

### Architecture compliance

Inspected `RiskCase`, all application ports/services, five reference adapters,
JDBC repository, controller, and architecture tests. Decision remains Core
Domain; Risk Case remains downstream and optional. Evidence/Decision/Action/
ActionOutcome ownership is not moved. Action intent and execution remain
separate. External details remain behind adapters. No new ADR is needed because
the implementation realizes ADR-010's already-approved boundaries and V4's
delegated UUIDv4/normalized-resolution decisions.

### ADR compliance

ADR-009 is preserved by immutable Decision references and the absence of
Decision creation/lifecycle logic in Q-008. Accepted ADR-010 is realized by the
case-owned aggregate, named lifecycle, append-only associations/history,
independent audit, opaque case number, resolution cycles, and no execution.
Architecture tests enforce the import and API boundaries.

### API standard compliance

Inspected all 21 controller methods and request/response records. Every endpoint
is under `/api/risk-cases`, returns `ApiResponse`, uses `@Valid` for mutation
DTOs, obtains ActorContext through the trusted resolver, exposes CaseNumber and
opaque event/note refs rather than internal `BIGINT` IDs, and uses named command
routes. No generic status setter or DELETE route exists. ResultCode tests prove
the exact nine Q-008 contracts and HTTP statuses.

### Database standard compliance

Inspected V8 and the seven migration tests on MySQL 8.4.11. Objects use
`snake_case`, `BIGINT id`, stable string codes, UTC `DATETIME(6)`, explicit
business identifiers, restrictive foreign keys, and Flyway-only versioning.
The migration count derives dynamically from the V7 baseline; Q-008 ownership
assertions use exact object names. There is no money/precision behavior and no
existing migration edit.

### Security standard compliance

Inspected authorization order tests, controller inputs, provider mappings,
query fail-closed behavior, audit payload construction, logs/scans, and
SecurityReview.md. Actor identity is never accepted from a request. Authorization
precedes case load/provider access. Strict subject eligibility distinguishes
not-eligible, not-found, and provider-unavailable results. No credential,
authentication header, note text, or upstream payload is logged/audited.

### Auditability compliance

Inspected every mutation service, Audit factory/writer, read query service, and
transaction tests. Each material operation writes one independent audit fact
in the same local transaction as root/history. Actors, reasons, source, UTC
time, before/after bounded facts, affected ref, and request/trace correlation
are captured. Notes and Evidence dispositions append replacements rather than
silently overwriting prior facts.

### Skill compliance

Read `docs/skills/development-standards.md`,
`brokeros-risk-core-domain.md`, `trading-account-reference-authority.md`,
`trusted-actor-authorization.md`, relevant recent Lessons Learned, and the
personal `brokeros-review-package` skill. Exact inventory scope and dynamic
migration-count rules are implemented. The transactional root/history/audit
failure-injection pattern was added to development standards, and the package
will be archived and checksum-verified by the review-package skill.

## Conditions

- Q-009 exposes trusted current ActorContext but no approved active-actor lookup
  by arbitrary assignee ref; see `OutstandingItems.md`.
- Stale governance status mirrors and the Q-004-specific infrastructure
  verifier require separate governance/maintenance work; they were not silently
  altered during Q-008 implementation.
- Independent Architect/Claude review remains mandatory before any acceptance
  or next Requirement.
