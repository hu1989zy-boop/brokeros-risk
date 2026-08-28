# Q-010 V5 Architecture and Development Standards Review

## Review Result

- Architecture conformance: **PASS FOR EXTERNAL ARCHITECT REVIEW**
- Architecture gap: **NONE**
- Design status: **DRAFT — AWAITING EXTERNAL ARCHITECT APPROVAL**
- Implementation Allowed: **NO**

## Architecture Impact

The proposed future change remains one package-bounded feature inside the
Phase 1 Spring Boot modular monolith. It plans four application-owned MySQL
tables, Spring JDBC adapters, a controlled non-Web input adapter, exact Q-009
authorization consumption, and a narrow Q-008 application contract. It adds no
service boundary, external database access, vendor adapter, public endpoint,
Kafka/Redis authority, Flink, Python, or new framework/dependency.

No approved identity owner, tuple, mapping cardinality, lifecycle, authority,
provisioning, consumer disclosure, or atomic-history decision is changed.

## Development Standards Compliance

### AGENTS.md compliance

The root `AGENTS.md`, Q-010 Requirement/Architecture/ADR-012, applicable
ADR-002/009/010/011, Q-009 implemented security contracts, Q-008 consumer
boundary, `docs/skills/development-standards.md`, relevant repository Skills,
Lessons Learned, code/package/schema/test conventions, and previous Review
packages were inspected. V5 adds design/governance/review documentation only.
No staging, commit, push, reset, clean, stash, or destructive history action
was performed.

### Architecture compliance

The design preserves the Phase 1 Java/Spring Boot/MySQL modular monolith,
broker/vendor-neutral core values, adapter isolation, explicit application
authorization, separation from Q-008 Risk Case behavior, and durable local
transaction boundary. Source-system discovery/integration remains outside
Q-010 and no external database is treated as authority.

### ADR compliance

ADR-002 keeps application schema in forward-only Flyway. ADR-009 supplies the
opaque subject/reference vocabulary, ADR-010 preserves Q-008 aggregate and
prerequisite constraints, ADR-011 supplies Q-009 ActorContext/capability
authorization, and ADR-012 fixes Q-010 identity ownership, tuple,
one-to-one immutability, lifecycle, MySQL authority, controlled provisioning,
bounded Q-008 disclosure, and atomic history. The design resolves only details
explicitly deferred by ADR-012; no new ADR is required.

### API standard compliance

No REST API, controller, request/response DTO, route, or versioning decision is
added. The only consumer contract is an internal typed application interface.
Future expected failures reuse `BusinessException`/stable ResultCodes, and any
later HTTP exposure would still require `ApiResponse` and
`GlobalExceptionHandler`; none is authorized here.

### Database standard compliance

The future V3 plan is additive and forward-only, uses `snake_case`, `BIGINT id`
internal PKs, separate opaque business refs, UTC `DATETIME(6)`, readable status
codes, exact binary collations/`VARBINARY`, named checks/unique keys/FKs/indexes,
optimistic versions, and `ON DELETE RESTRICT`. V1/V2 are not edited and no SQL
or migration is created. Mandatory proof is assigned to disposable MySQL
8.4.11 rather than H2 or schema auto-generation.

### Security standard compliance

Every Q-010 access requires the existing Q-009 guard before repository access.
The design has exact least-privilege capabilities, no SYSTEM/wildcard/role
bypass, a registered purpose-specific service descriptor, no caller-supplied
actor/time/generated ref, strict manifests, durable replay protection, safe
non-enumerating failures, and explicit bans on credentials, raw external
identities, manifests, SQL values, and sensitive provenance in logs/metrics.

### Auditability compliance

Each future mutation commits operation outcome and exactly one immutable
history row with actor, evaluated capability/decision versions/time,
attestation, reason/change reference, before/after lifecycle/version,
correlation, target, and UTC time in the same transaction as state. Q-010 does
not invent the general Audit module, and a forced history failure must roll
back state and operation outcome.

### Skill compliance

`development-standards.md`, the core-domain skill, and the trusted-actor/
authorization skill informed package ownership, opaque identities, Q-009
ordering, failure, Flyway, test, and Review decisions. No new Skill is created:
the Q-010 transaction/idempotency pattern remains unimplemented and
runtime-unverified. The new Lessons Learned records this honestly and requires
reassessment after executable MySQL evidence.

## Violations

Unresolved standards violation in the Implementation Design: **NONE**.

This result permits Architect review only. Runtime conformance is unverified
and implementation remains prohibited.
