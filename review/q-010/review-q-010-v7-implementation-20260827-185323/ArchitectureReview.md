# Q-010 V7 Architecture Conformance Review

## Result

**PASS FOR SUBMISSION — AWAITING INDEPENDENT ARCHITECT REVIEW**

The implementation remains a `com.brokeros.risk.tradingaccount` module inside
the existing Spring Boot modular monolith. Domain/application code is
framework-neutral; JDBC, Jackson, Spring configuration, command startup, and
Micrometer remain in infrastructure/interface packages. No architecture or ADR
change was required.

## Development Standards Compliance

### AGENTS.md compliance

Inspected all 62 Q-010 production/test Java files, Flyway V3, ResultCode,
security descriptor registration, verification scripts, Q-010 documents,
skill, lesson, and Review evidence. The work follows the Requirement → approved
Architecture/ADR/Design → implementation → verification → skill/lesson → Review
sequence. It does not implement Q-008, add vendor coupling, commit, push, stage,
or alter an already-applied migration.

### Architecture compliance

The module is broker/CRM/platform-neutral and remains within the Phase 1
modular monolith. External identity fields are canonical data, not an external
SDK/database adapter. Risk detection/action execution, Kafka, Redis, MT4/MT5,
CRM, frontend, deployment topology, and public APIs are unchanged. Q-008 can
only consume the bounded Q-010 eligibility service.

### ADR compliance

ADR-009 ownership boundaries, ADR-010's Q-008 prerequisite boundary, ADR-011's
trusted Actor/capability model, and ADR-012's identity/persistence decision were
checked. The implementation reuses the actual Q-009 ActorContext,
AuthorizationGuard, registered descriptor object identity, MySQL source of
truth, exact tuple cardinality, non-Web provisioning, durable history, and
fail-closed behavior. No new ADR was needed because the approved decisions were
implemented without changing a boundary or dependency.

### API standard compliance

No REST endpoint or controller was added. Existing application APIs still use
ApiResponse. Q-010 adds only the approved stable ResultCodes and an internal
typed application facade; persistence entities/rows and external identity
fields do not cross that contract.

### Database standard compliance

Flyway V3 is additive, schema-only, forward-only, and immutable. It creates
four snake_case InnoDB tables with BIGINT `id` primary keys, binary-exact key
storage, readable lifecycle/operation codes, UTC DATETIME(6), named uniqueness,
restricting FKs, CHECK constraints, and bounded indexes. Disposable MySQL 8.4
proved V1→V2→V3, seven-table shape, constraints, CHECK error 3819, collision
classification, index compatibility, restart idempotence, CAS, and rollback.
No destructive DDL/DML exists.

### Security standard compliance

Every application read/mutation invokes Q-009 authorization before a Q-010
port. The command requires the code-owned descriptor singleton plus active
service mapping/grant, rejects unknown/duplicate/trailing manifest data and
symlinks, and has no HTTP/scheduler/watcher surface. Tests prove denial causes
zero authority lookup, grant revocation denies the command, and safe output
omits the external key, namespace, attestation, reason, ActorRef, and manifest.
No secret or credential is committed or packaged.

### Auditability compliance

Each committed mutation has one durable idempotency operation and one immutable
history row capturing trusted actor, exact capability, authorization time and
versions, attestation, reason/change ref, before/after lifecycle/version,
correlation, and server time. State/outcome/history share one transaction;
forced operation and history failures both roll back current state.

### Skill compliance

The mandatory development/core-domain/Q-009 skills were applied. Q-010's
reusable identity, authorization, transaction, race, privacy, and verification
patterns are captured in
`docs/skills/trading-account-reference-authority.md`. Honest implementation
lessons were added at
`docs/lessons/2026-08-27-q-010-trading-account-reference-authority-implementation.md`.

No unresolved development-standards violation was found in the Q-010 change.
The separate historical Q-009 whitespace issue remains unchanged and is not a
Q-010 architecture failure.
